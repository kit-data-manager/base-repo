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
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.FeatureNotImplementedException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.dao.ContentInformationMatchSpecification;
import edu.kit.datamanager.repo.dao.ContentInformationTagSpecification;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.PathUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.PatchUtil;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.codec.binary.Hex;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

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

//  @PersistenceContext
//  private EntityManager em;
  @Autowired
  private ApplicationProperties applicationProperties;

//  @Override
//  @Transactional(readOnly = true)
//  public Page<ContentInformation> getContentInformation(Long id, String relativePath, String tag, Pageable pgbl){
//    logger.trace("Performing getContentInformation({}, {}, {}, {}).", id, relativePath, tag, pgbl);
//
//    //wrong header added!
//    // eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(ContentInformation.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), pageSize));
//    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(id, relativePath, false));
//    if(tag != null){
//      logger.debug("Content information tag {} provided. Using TagSpecification.", tag);
//      spec = ContentInformationTagSpecification.andIfTag(spec, tag);
//    }
//    return dao.findAll(spec, pgbl);
//  }
  @Override
  @Transactional(readOnly = true)
  public ContentInformation getContentInformation(String identifier, String relativePath){
    logger.trace("Performing getContentInformation({}, {}).", identifier, relativePath);

    logger.trace("Performing findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag({}, {}).", identifier, relativePath);
    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(identifier, relativePath, true));
    Optional<ContentInformation> contentInformation = dao.findOne(spec);

    if(!contentInformation.isPresent()){
      //TODO: check later for collection download
      logger.error("No content found for resource {} at path {}. Throwing ResourceNotFoundException.", identifier, relativePath);
      throw new ResourceNotFoundException("No content information for identifier " + identifier + ", path " + relativePath + " found.");
    }

    return contentInformation.get();
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
  public ContentInformation create(ContentInformation contentInformation, DataResource resource,
          String path,
          InputStream file,
          boolean force){
    logger.trace("Performing create({}, {}, {}, {}, {}).", contentInformation, "DataResource#" + resource.getId(), "<InputStream>", path, force);

    //check for existing content information
    //We use here no tags as tags are just for reflecting related content elements, but all tags are associated with the same content element.
    Page<ContentInformation> existingContentInformation = findAll(ContentInformation.createContentInformation(resource.getId(), path), PageRequest.of(0, 1));

    ContentInformation contentInfo;
    Path toRemove = null;
    if(existingContentInformation.hasContent()){
      logger.trace("Existing content information found. Checking 'force' flag.");
      //existing content, overwrite necessary
      if(!force){
        //conflict
        logger.error("Existing content information found for resource {} at path {} and 'force' flag not set. Throwing ResourceAlreadyExistException.", "DataResource#" + resource.getId(), path);
        throw new ResourceAlreadyExistException("There is already content registered at " + path + ". Provide force=true in order to replace the existing resource.");
      } else{
        logger.trace("'Force' flag set. Checking for local content to replace.");
        //overwrite...mark file for deletion
        contentInfo = existingContentInformation.getContent().get(0);
        URI contentUri = URI.create(contentInfo.getContentUri());
        if("file".equals(contentUri.getScheme())){
          //mark file for removal
          logger.debug("File content found. Marking current file at URI {} for replacement.", contentInfo.getContentUri());
          toRemove = Paths.get(URI.create(contentInfo.getContentUri()));
        }//content URI is not pointing to a file...just replace the entry
      }
    } else{
      logger.trace("No existing content information found.");
      //no existing content information, create new or take provided
      contentInfo = (contentInformation != null) ? contentInformation : ContentInformation.createContentInformation(path);
      contentInfo.setId(null);
      contentInfo.setParentResource(resource);
      contentInfo.setRelativePath(path);
    }

    if(file != null){
      logger.trace("User upload detected. Preparing to consume data.");
      //file upload
      URI dataUri = PathUtils.getDataUri(contentInfo.getParentResource(), contentInfo.getRelativePath(), applicationProperties);
      Path destination = Paths.get(dataUri);
      logger.trace("Preparing destination {} for storing user data.", destination);
      //store data
      OutputStream out = null;
      try{
        //read/write file, create checksum and calculate file size
        Files.createDirectories(destination.getParent());

        MessageDigest md = MessageDigest.getInstance("SHA1");
        logger.trace("Start reading user data from stream.");
        int cnt;
        long bytes = 0;
        byte[] buffer = new byte[1024];
        out = Files.newOutputStream(destination);
        while((cnt = file.read(buffer)) > -1){
          out.write(buffer, 0, cnt);
          md.update(buffer, 0, cnt);
          bytes += cnt;
        }

        logger.trace("Performing upload post-processing.");
        contentInfo.setHash("sha1:" + Hex.encodeHexString(md.digest()));
        logger.debug("Assigned hash {} to content information.", contentInfo.getHash());
        contentInfo.setSize(bytes);
        logger.debug("Assigned size {} to content information.", contentInfo.getSize());
        contentInfo.setContentUri(dataUri.toString());
        logger.debug("Assigned content URI {} to content information.", contentInfo.getContentUri());

        logger.trace("Trying to determine content type.");
        try(InputStream is = Files.newInputStream(destination); BufferedInputStream bis = new BufferedInputStream(is);){
          AutoDetectParser parser = new AutoDetectParser();
          Detector detector = parser.getDetector();
          Metadata md1 = new Metadata();
          md1.add(Metadata.RESOURCE_NAME_KEY, contentInfo.getFilename());
          org.apache.tika.mime.MediaType mediaType = detector.detect(bis, md1);
          contentInfo.setMediaType(mediaType.toString());
          logger.trace("Assigned media type {} to content information.", contentInfo.getMediaType());
        }
      } catch(IOException ex){
        logger.error("Failed to finish upload. Throwing CustomInternalServerError.", ex);
        throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
      } catch(NoSuchAlgorithmException ex){
        logger.error("Failed to initialize SHA1 message digest. Throwing CustomInternalServerError.", ex);
        throw new CustomInternalServerError("Internal digest initialization error. Unable to perform upload.");
      } finally{
        if(out != null){
          try{
            out.flush();
            out.close();
          } catch(IOException ignored){
          }
        }
      }
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
        logger.trace("Additional metadata found. Setting metadata.");
        contentInfo.setMetadata(contentInformation.getMetadata());
      }

      if(contentInformation.getTags() != null){
        logger.trace("User-provided tags found. Setting tags.");
        contentInfo.setTags(contentInformation.getTags());
      }
    }

    logger.trace("Persisting content information.");
    ContentInformation result = getDao().save(contentInfo);
    if(toRemove != null){
      try{
        logger.debug("Removing replaced content from {}.", toRemove);
        Files.deleteIfExists(toRemove);
        logger.trace("Content at {} successfully removed.", toRemove);
      } catch(IOException ex){
        logger.warn("Failed to remove previously existing content from " + toRemove + ". Manual removal required.", ex);
      }
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
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
      spec = ContentInformationTagSpecification.andIfTags(spec, tags.toArray(new String[]{}));
    }
    return dao.findAll(spec, pgbl);
  }

  @Override
  @Transactional(readOnly = false)
  public void patch(ContentInformation resource, JsonPatch patch, Collection<? extends GrantedAuthority> userGrants){
    logger.trace("Performing patch({}, {}, {}).", "ContentInformation#" + resource.getId(), patch, userGrants);
    ContentInformation updated = PatchUtil.applyPatch(resource, patch, ContentInformation.class, userGrants);
    logger.trace("Patch successfully applied. Persisting patched resource.");
    getDao().save(updated);
    logger.trace("Resource successfully persisted.");
  }

  @Override
  public void delete(ContentInformation resource){
    logger.trace("Performing delete({}).", "ContentInformation#" + resource.getId());
    getDao().delete(resource);
  }

  protected IContentInformationDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    return Health.up().withDetail("ContentInformation", dao.count()).build();
  }

  @Override
  public ContentInformation put(ContentInformation c, ContentInformation c1, Collection<? extends GrantedAuthority> clctn) throws UpdateForbiddenException{
    throw new FeatureNotImplementedException("PUT requests are not supported for this resource.");
  }
}
