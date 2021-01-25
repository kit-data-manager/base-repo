/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.repo.service.impl;

import com.github.fge.jsonpatch.JsonPatch;
import com.monitorjbl.json.JsonResult;
import edu.kit.datamanager.entities.ContentElement;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.entities.messaging.DataResourceMessage;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.FeatureNotImplementedException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationContentUriSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationMediaTypeSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationMatchSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationMetadataSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationPermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationRelativePathSpecification;
import edu.kit.datamanager.repo.dao.spec.contentinformation.ContentInformationTagSpecification;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IContentCollectionProvider;
import edu.kit.datamanager.service.IContentProvider;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.IVersioningService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import edu.kit.datamanager.util.PatchUtil;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 *
 * @author jejkal
 */
public class ContentInformationService implements IContentInformationService{

  private final JsonResult json = JsonResult.instance();

  @Autowired
  private Logger logger;

  @Autowired
  private IContentInformationDao dao;

  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IMessagingService messagingService;
  @Autowired
  private IAuditService<ContentInformation> auditService;
  @Autowired
  private IVersioningService[] versioningServices;

  @Autowired
  private IContentProvider[] contentProviders;

  @Autowired
  private IContentCollectionProvider[] collectionContentProviders;

  @Override
  @Transactional
  public ContentInformation create(ContentInformation contentInformation, DataResource resource,
          String path,
          InputStream file,
          boolean force){
    logger.trace("Performing create({}, {}, {}, {}, {}).", contentInformation, "DataResource#" + resource.getId(), "<InputStream>", path, force);

    //check for existing content information
    //We use here no tags as tags are just for reflecting related content elements, but all tags are associated with the same content element.
    Page<ContentInformation> existingContentInformation = findAll(ContentInformation.createContentInformation(resource.getId(), path), PageRequest.of(0, 1));

    Map<String, String> options = new HashMap<>();
    options.put("force", Boolean.toString(force));

    ContentInformation contentInfo;
    Path toRemove = null;
    if(existingContentInformation.hasContent()){
      contentInfo = existingContentInformation.getContent().get(0);
      options.put("contentUri", contentInfo.getContentUri());
    } else{
      logger.trace("No existing content information found.");
      //no existing content information, create new or take provided
      contentInfo = (contentInformation != null) ? contentInformation : ContentInformation.createContentInformation(path);
      contentInfo.setId(null);
      contentInfo.setParentResource(resource);
      contentInfo.setRelativePath(path);
    }

    String newFileVersion = null;
    if(file != null){
      logger.trace("User upload detected. Preparing to consume data.");
      //file upload

      String versioningService = (contentInformation != null && contentInformation.getVersioningService() != null) ? contentInformation.getVersioningService() : applicationProperties.getDefaultVersioningService();
      contentInfo.setVersioningService(versioningService);
      boolean fileWritten = false;
      logger.trace("Trying to use versioning service named '{}' for writing file content.", versioningService);
      for(IVersioningService service : versioningServices){
        if(versioningService.equals(service.getServiceName())){
          logger.trace("Versioning service found, writing file content.");
          service.configure();
          try{
            service.write(resource.getId(), AuthenticationHelper.getPrincipal(), path, file, options);
          } catch(Throwable t){
            logger.error("Failed to write content using versioning service " + versioningService + ".", t);
            throw t;
          }
          logger.trace("File content successfully written.");
          fileWritten = true;
        } else{
          logger.trace("Skipping service '{}'", service.getServiceName());
        }
      }

      if(!fileWritten){
        logger.error("No versioning service found for name '{}'.", versioningService);
        throw new BadArgumentException("Versioning service '" + versioningService + "' not found.");
      }

      logger.trace("Obtaining file-specific information from versioning service response.");
      if(options.containsKey("size")){
        contentInfo.setSize(Long.parseLong(options.get("size")));
      }

      if(options.containsKey("checksum")){
        contentInfo.setHash(options.get("checksum"));
      }
      if(options.containsKey("contentUri")){
        contentInfo.setContentUri(options.get("contentUri"));
      }
      if(options.containsKey("mediaType")){
        contentInfo.setMediaType(options.get("mediaType"));
      }

      if(options.containsKey("fileVersion")){
        newFileVersion = options.get("fileVersion");
      }

      logger.trace("File successfully written using versioning service '{}'.", versioningService);
    } else{
      logger.trace("No user upload detected. Checking content URI in content information.");
      //no file upload, take data reference URI from provided content information
      if(contentInformation == null || contentInformation.getContentUri() == null){
        logger.error("No content URI provided in content information. Throwing BadArgumentException.");
        throw new BadArgumentException("Neither a file upload nor an external content URI were provided.");
      } else{
        logger.trace("Content URI {} detected. Checking URI scheme.", contentInfo.getContentUri());
        if("file".equals(URI.create(contentInfo.getContentUri()).getScheme().toLowerCase()) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.getValue())){
          logger.error("Content URI scheme is 'file' but caller has no ADMINISTRATOR role. Content information creation rejected. Throwing BadArgumentException.");
          throw new BadArgumentException("You are not permitted to add content information with URI scheme of type 'file'.");
        }
        logger.trace("Accepting attributed from provided content information.");
        //take content uri and provided checksum and size, if available
        contentInfo.setContentUri(contentInformation.getContentUri());
        logger.debug("Assigned content URI {} to content information.", contentInfo.getContentUri());
        contentInfo.setSize(contentInformation.getSize());
        logger.debug("Assigned size {} to content information.", contentInfo.getSize());
        contentInfo.setHash(contentInformation.getHash());
        logger.debug("Assigned hash {} to content information.", contentInfo.getHash());
      }
    }

    //copy metadata and tags from provided content information if available
    logger.trace("Checking for additional metadata.");
    if(contentInformation != null){
      if(contentInformation.getMetadata() != null){
        logger.trace("Additional metadata found. Transferring value.");
        contentInfo.setMetadata(contentInformation.getMetadata());
      }

      if(contentInformation.getTags() != null){
        logger.trace("User-provided tags found. Transferring value.");
        contentInfo.setTags(contentInformation.getTags());
      }
      if(contentInformation.getUploader() != null){
        logger.trace("User-provided uploader found. Transferring value.");
        contentInfo.setUploader(contentInformation.getUploader());
      }
    } else{
      String principal = AuthenticationHelper.getPrincipal();
      logger.trace("No content information provided. Setting uploader property from caller principal value {}.", principal);
      contentInfo.setUploader(principal);
    }

    long newMetadataVersion = (contentInfo.getId() != null) ? auditService.getCurrentVersion(Long.toString(contentInfo.getId())) + 1 : 1;
    logger.trace("Setting new version number of content information to {}.", newMetadataVersion);
    contentInfo.setVersion((int) newMetadataVersion);

    if(newFileVersion == null){
      logger.trace("No file version provided by versioning service. Using metadata version {} as file version.", newMetadataVersion);
      contentInfo.setFileVersion(Long.toString(newMetadataVersion));
    }

    logger.trace("Persisting content information.");
    ContentInformation result = getDao().save(contentInfo);

    logger.trace("Capturing audit information.");
    auditService.captureAuditInformation(result, AuthenticationHelper.getPrincipal());

    logger.trace("Sending CREATE event.");
    messagingService.send(DataResourceMessage.factoryCreateDataMessage(resource.getId(), result.getRelativePath(), result.getContentUri(), result.getMediaType(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
    return result;
  }

  @Override
  public void read(DataResource resource, String path, Long version, String acceptHeader, HttpServletResponse response){
    URI uri;
    if(path.endsWith("/") || path.isEmpty()){
      //collection download
      ContentInformation info = ContentInformation.createContentInformation(resource.getId(), path);
      Page<ContentInformation> page = findAll(info, PageRequest.of(0, Integer.MAX_VALUE));
      if(page.isEmpty()){
        //nothing to provide
        throw new ResourceNotFoundException("No content found at the provided location.");
      }

      MediaType acceptHeaderType = acceptHeader != null ? MediaType.parseMediaType(acceptHeader) : null;
      boolean provided = false;
      Set<MediaType> acceptableMediaTypes = new HashSet<>();
      for(IContentCollectionProvider provider : collectionContentProviders){
        if(acceptHeaderType != null && provider.supportsMediaType(acceptHeaderType)){
          List<ContentElement> elements = new ArrayList<>();
          page.getContent().forEach((c) -> {
            URI contentUri = URI.create(c.getContentUri());
            if(provider.canProvide(contentUri.getScheme())){
              String contextUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
              logger.trace("Adding collection mapping '{}':'{}' with checksum '{}' to list. Additionally providing context Uri {} and size {}.", c.getRelativePath(), contentUri, c.getHash(), contextUri, c.getSize());
              elements.add(ContentElement.createContentElement(resource.getId(), c.getRelativePath(), c.getContentUri(), c.getFileVersion(), c.getVersioningService(), c.getHash(), contextUri, c.getSize()));
            } else{
              logger.debug("Skip adding collection mapping '{}':'{}' to map as content provider {} is not capable of providing URI scheme.", c.getRelativePath(), contentUri, provider.getClass());
            }
          });
          logger.trace("Start providing content.");
          provider.provide(elements, MediaType.parseMediaType(acceptHeader), response);
          logger.trace("Content successfully provided.");
          provided = true;
        } else{
          Collection<MediaType> col = new ArrayList<>();
          Collections.addAll(col, provider.getSupportedMediaTypes());
          acceptableMediaTypes.addAll(col);
        }
        break;
      }

      if(!provided){
        //we are done here, content is already submitted
        logger.info("No content collection provider found for media type {} in Accept header. Throwing HTTP 415 (UNSUPPORTED_MEDIA_TYPE).", acceptHeaderType);
        throw new UnsupportedMediaTypeStatusException(acceptHeaderType, new ArrayList<>(acceptableMediaTypes));
      }
    } else{
      //try to obtain single content element matching path exactly
      ContentInformation contentInformation = getContentInformation(resource.getId(), path, version);
      uri = (contentInformation.getContentUri() != null) ? URI.create(contentInformation.getContentUri()) : null;
      String contentScheme = (uri != null) ? uri.getScheme() : "file";
      logger.debug("Trying to provide content at URI {} by any configured content provider.", uri);
      boolean provided = false;
      for(IContentProvider contentProvider : contentProviders){
        if(contentProvider.canProvide(contentScheme)){
          logger.trace("Using content provider {}.", contentProvider.getClass());
          String contextUri = ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
          contentProvider.provide(ContentElement.createContentElement(resource.getId(),
                  contentInformation.getRelativePath(), contentInformation.getContentUri(),
                  contentInformation.getFileVersion(),
                  contentInformation.getVersioningService(),
                  contentInformation.getHash(),
                  contextUri,
                  contentInformation.getSize()),
                  contentInformation.getMediaTypeAsObject(),
                  contentInformation.getFilename(),
                  response);
          provided = true;
          break;
        }
      }
      if(!provided){
        //obtain data uri and check for content to exist
        String dataUri = contentInformation.getContentUri();
        if(dataUri != null){
          uri = URI.create(dataUri);
          logger.info("No content provider found for URI {}. Returning URI in Content-Location header.", uri);
          HttpHeaders headers = new HttpHeaders();
          headers.add("Content-Location", uri.toString());
          Set<String> headerKeys = headers.keySet();
          headerKeys.forEach((headerKey) -> {
            headers.get(headerKey).forEach((value) -> {
              response.addHeader(headerKey, value);
            });
          });
          response.setStatus(HttpStatus.NO_CONTENT.value());
        } else{
          logger.info("No data URI found for resource with identifier {} and path {}. Returning HTTP 404.", resource.getId(), path);
          throw new ResourceNotFoundException("No data URI found for the addressed content.");
        }
      }
    }
  }

  @Override
  public ContentInformation getContentInformation(String identifier, String relativePath, Long version){
    logger.trace("Performing getContentInformation({}, {}).", identifier, relativePath);

    logger.trace("Performing findOne({}, {}).", identifier, relativePath);
    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(identifier, relativePath, true));
    Optional<ContentInformation> contentInformation = dao.findOne(spec);

    if(!contentInformation.isPresent()){
      //TODO: check later for collection download
      logger.error("No content found for resource {} at path {}. Throwing ResourceNotFoundException.", identifier, relativePath);
      throw new ResourceNotFoundException("No content information for identifier " + identifier + ", path " + relativePath + " found.");
    }
    ContentInformation result = contentInformation.get();
    
    if(applicationProperties.isAuditEnabled() && Objects.nonNull(version)){
      logger.trace("Obtained content information for identifier {}. Checking for shadow of version {}.", result.getId(), version);
      Optional<ContentInformation> optAuditResult = auditService.getResourceByVersion(Long.toString(result.getId()), version);
      if(optAuditResult.isPresent()){
        logger.trace("Shadow successfully obtained. Returning version {} of content information with id {}.", version, result.getId());
        return optAuditResult.get();
      } else{
        logger.info("Version {} of content information {} not found. Returning HTTP 404 (NOT_FOUND).", version, result.getId());
        throw new ResourceNotFoundException("Content information with identifier " + result.getId() + " is not available in version " + version + ".");
      }
    }

    return result;
  }

  @Override
  public Optional<String> getAuditInformationAsJson(String resourceIdentifier, Pageable pgbl){
    logger.trace("Performing getAuditInformation({}, {}).", resourceIdentifier, pgbl);
    return auditService.getAuditInformationAsJson(resourceIdentifier, pgbl.getPageNumber(), pgbl.getPageSize());
  }

  @Override
  public ContentInformation findById(String identifier) throws ResourceNotFoundException{
    logger.trace("Performing findById({}).", identifier);
    Long id = Long.parseLong(identifier);
    Optional<ContentInformation> contentInformation = getDao().findById(id);
    if(!contentInformation.isPresent()){
      //TODO: check later for collection download
      logger.error("No content found for id {}. Throwing ResourceNotFoundException.", id);
      throw new ResourceNotFoundException("No content information for id " + id + " found.");
    }
    return contentInformation.get();
  }

  @Override
  public Page<ContentInformation> findByExample(ContentInformation example,
          List<String> callerIdentities,
          boolean callerIsAdmin,
          Pageable pgbl){
    logger.trace("Performing findByExample({}, {}).", example, pgbl);
    Page<ContentInformation> page;

    if(example == null){
      //obtain all accessible content elements
      logger.trace("No example provided. Returning all accessible content elements.");
      Specification<ContentInformation> spec = Specification.where(ContentInformationPermissionSpecification.toSpecification(null, callerIdentities, PERMISSION.READ));
      page = dao.findAll(spec, pgbl);
    } else{
      Specification<ContentInformation> spec;

      if(example.getParentResource() != null && example.getParentResource().getId() != null){
        logger.trace("Parent resource with id {} provided in example. Searching for content in single resource.", example.getParentResource().getId());
        spec = Specification.where(ContentInformationPermissionSpecification.toSpecification(example.getParentResource().getId(), callerIdentities, PERMISSION.READ));
      } else{
        logger.trace("No parent resource provided in example. Searching for content in all resources.");
        spec = Specification.where(ContentInformationPermissionSpecification.toSpecification(null, callerIdentities, PERMISSION.READ));
      }

      logger.trace("Adding additional query specifications based on example {}.", example);

      if(example.getRelativePath() != null){
        logger.trace("Adding relateive path query specification for relative path {}.", example.getRelativePath());
        spec = spec.and(ContentInformationRelativePathSpecification.toSpecification(example.getRelativePath(), false));
      }

      if(example.getContentUri() != null){
        logger.trace("Adding content Uri query specification for metadata {}.", example.getContentUri());
        spec = spec.and(ContentInformationContentUriSpecification.toSpecification(example.getContentUri(), false));
      }

      if(example.getMediaType() != null){
        logger.trace("Adding mediatype query specification for media type {}.", example.getMediaType());
        spec = spec.and(ContentInformationMediaTypeSpecification.toSpecification(example.getMediaType(), false));
      }

      if(example.getMetadata() != null && !example.getMetadata().isEmpty()){
        logger.trace("Adding metadata query specification for metadata {}.", example.getMetadata());
        spec = spec.and(ContentInformationMetadataSpecification.toSpecification(example.getMetadata()));
      }

      if(example.getTags() != null && !example.getTags().isEmpty()){
        logger.debug("Adding tag query specification for tags {}.", example.getTags());
        spec = spec.and(ContentInformationTagSpecification.toSpecification(example.getTags().toArray(new String[]{})));
      }

      logger.trace("Calling findAll for collected specs and page information {}.", pgbl);
      page = dao.findAll(spec, pgbl);
    }

    logger.trace("Returning page content.");
    return page;
  }

  @Override
  public Page<ContentInformation> findAll(ContentInformation c, Instant lastUpdateFrom, Instant lastUpdateUntil, Pageable pgbl){
    logger.trace("Performing findAll({}, {}, {}, {}).", c, lastUpdateFrom, lastUpdateUntil, pgbl);
    logger.info("Obtaining content information from an lastUpdate range is not supported. Ignoring lastUpdate arguments.");
    return findAll(c, pgbl);
  }

  @Override
  public Page<ContentInformation> findAll(ContentInformation c, Pageable pgbl){
    logger.trace("Performing findAll({}, {}).", c, pgbl);

    if(c.getParentResource() == null){
      logger.error("Parent resource in template must not be null. Throwing CustomInternalServerError.");
      throw new CustomInternalServerError("Parent resource is missing from template.");
    }
    String parentId = c.getParentResource().getId();
    String relativePath = c.getRelativePath();
    Set<String> tags = c.getTags();
    //wrong header added!
    // eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(ContentInformation.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), pageSize));
    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(parentId, relativePath, false));
    if(tags != null && !tags.isEmpty()){
      logger.debug("Content information tags {} provided. Using TagSpecification.", tags);
      spec = spec.and(ContentInformationTagSpecification.toSpecification(tags.toArray(new String[]{})));
    }
    return dao.findAll(spec, pgbl);
  }

  @Override
  @Transactional
  public void patch(ContentInformation resource, JsonPatch patch, Collection<? extends GrantedAuthority> userGrants){
    logger.trace("Performing patch({}, {}, {}).", "ContentInformation#" + resource.getId(), patch, userGrants);
    ContentInformation updated = PatchUtil.applyPatch(resource, patch, ContentInformation.class, userGrants);
    logger.trace("Patch successfully applied.");

    long newVersion = auditService.getCurrentVersion(Long.toString(updated.getId())) + 1;
    logger.trace("Setting new version number of content information to {}.", newVersion);
    updated.setVersion((int) newVersion);

    ContentInformation result = getDao().save(updated);
    logger.trace("Resource successfully persisted.");

    logger.trace("Capturing audit information.");
    auditService.captureAuditInformation(result, AuthenticationHelper.getPrincipal());

    logger.trace("Sending UPDATE event.");
    messagingService.send(DataResourceMessage.factoryUpdateDataMessage(resource.getParentResource().getId(), updated.getRelativePath(), updated.getContentUri(), updated.getMediaType(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
  }

  @Override
  @Transactional
  public void delete(ContentInformation resource){
    logger.trace("Performing delete({}).", "ContentInformation#" + resource.getId());
    getDao().delete(resource);

    logger.trace("Deleting audit information.");
    auditService.deleteAuditInformation(Long.toString(resource.getId()), resource);

    logger.trace("Sending DELETE event.");
    messagingService.send(DataResourceMessage.factoryDeleteDataMessage(resource.getParentResource().getId(), resource.getRelativePath(), resource.getContentUri(), resource.getMediaType(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
  }

  protected IContentInformationDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    logger.trace("Obtaining health information.");
    boolean repositoryPathAvailable = true;
    URL basePath = applicationProperties.getBasepath();
    try{
      Path basePathAsPath = Paths.get(basePath.toURI());
      Path probe = Paths.get(basePathAsPath.toString(), "probe.txt");
      try{
        probe = Files.createFile(probe);
        Files.write(probe, "Success".getBytes());
      } catch(Throwable t){
        logger.error("Failed to check repository folder at " + basePath + ". Returning negative health status.", t);
        repositoryPathAvailable = false;
      } finally{
        try{
          Files.deleteIfExists(probe);
        } catch(Throwable ignored){
        }
      }
    } catch(URISyntaxException ex){
      logger.error("Invalid base path uri of " + basePath + ".", ex);
      repositoryPathAvailable = false;
    }
    if(repositoryPathAvailable){
      return Health.up().withDetail("ContentInformation", dao.count()).build();
    } else{
      return Health.down().withDetail("ContentInformation", 0).build();
    }
  }

  @Override
  public ContentInformation put(ContentInformation c, ContentInformation c1, Collection<? extends GrantedAuthority> clctn) throws UpdateForbiddenException{
    throw new FeatureNotImplementedException("PUT requests are not supported for this resource.");
  }
}
