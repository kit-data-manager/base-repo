/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.web;

import com.github.fge.jsonpatch.JsonPatch;
import com.monitorjbl.json.JsonResult;
import com.monitorjbl.json.JsonView;
import static com.monitorjbl.json.Match.match;
import io.swagger.annotations.Api;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import edu.kit.datamanager.controller.hateoas.event.PaginatedResultsRetrievedEvent;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.FeatureNotImplementedException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.EtagMismatchException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.UnauthorizedAccessException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.UnknownInformationConstants;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.repo.util.PathUtils;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.PatchUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/dataresources")
@Api(value = "Data Resource Management")
public class DataResourceController implements IDataResourceController{

  private final JsonResult json = JsonResult.instance();

  @Autowired
  private Logger LOGGER;
  @Autowired
  private final IDataResourceService dataResourceService;

  @Autowired
  private final IContentInformationService contentInformationService;

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private ApplicationProperties applicationProperties;

  public DataResourceController(IDataResourceService dataResourceService, IContentInformationService contentInformationService){
    super();
    this.dataResourceService = dataResourceService;
    this.contentInformationService = contentInformationService;
  }

  @Override
  public ResponseEntity<DataResource> create(@RequestBody DataResource resource, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Anonymous resource creation disabled.");
    }

    if(resource.getIdentifier() == null){
      //set placeholder identifier
      resource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER));
      //check alternate identifiers for internal identifier
      boolean haveAlternateInternalIdentifier = false;
      for(Identifier alt : resource.getAlternateIdentifiers()){
        if(Identifier.IDENTIFIER_TYPE.INTERNAL.equals(alt.getIdentifierType())){
          if(alt.getValue() == null){
            throw new BadArgumentException("Provided internal indentifier must not be null.");
          }
          resource.setResourceIdentifier(alt.getValue());
          haveAlternateInternalIdentifier = true;
          break;
        }
      }

      if(!haveAlternateInternalIdentifier){
        String altId = UUID.randomUUID().toString();
        LOGGER.info("No primary identifier assigned to resource and no alternate identifier of type INTERNAL was found. Assigning alternate INTERNAL identifier {}.", altId);
        resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(altId));
        resource.setResourceIdentifier(altId);
      }
    } else{
      resource.setResourceIdentifier(resource.getIdentifier().getValue());
    }

    //check resource by identifier
    DataResource expl = new DataResource();
    expl.setResourceIdentifier(resource.getResourceIdentifier());

    Page<DataResource> res = dataResourceService.findAll(expl, PageRequest.of(0, 1), true);

    if(res.hasContent()){
      throw new ResourceAlreadyExistException("There is already a resource with identifier " + resource.getResourceIdentifier());
    }

    //check mandatory datacite attributes
    if(resource.getCreators().isEmpty()){
      Agent creator = new Agent();
      creator.setGivenName(AuthenticationHelper.getFirstname());
      creator.setFamilyName(AuthenticationHelper.getLastname());
      resource.getCreators().add(creator);
    }

    if(resource.getTitles().isEmpty()){
      throw new BadArgumentException("No title assigned to provided document.");
    }

    if(resource.getResourceType() == null){
      throw new BadArgumentException("No resource type assigned to provided document.");
    }

    String caller = (String) AuthenticationHelper.getAuthentication().getPrincipal();

    //set auto-generateable fields
    if(resource.getPublisher() == null){
      resource.setPublisher(caller);
    }

    if(resource.getPublicationYear() == null){
      resource.setPublicationYear(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    }

    //check ACLs for caller
    AclEntry callerEntry = null;
    for(AclEntry entry : resource.getAcls()){
      if(caller.equals(entry.getSid())){
        callerEntry = entry;
        break;
      }
    }

    if(callerEntry == null){
      callerEntry = new AclEntry(caller, PERMISSION.ADMINISTRATE);
      resource.getAcls().add(callerEntry);
    } else{
      //make sure at least the caller has administrate permissions
      callerEntry.setPermission(PERMISSION.ADMINISTRATE);
    }

    boolean haveCreationDate = false;
    for(edu.kit.datamanager.repo.domain.Date d : resource.getDates()){
      if(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())){
        haveCreationDate = true;
        break;
      }
    }

    if(!haveCreationDate){
      LOGGER.trace("Resource has no creation date. Setting current date.");
      edu.kit.datamanager.repo.domain.Date creationDate = new edu.kit.datamanager.repo.domain.Date();
      creationDate.setType(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED);
      creationDate.setValue(Instant.now());
      resource.getDates().add(creationDate);
    }

    if(Objects.isNull(resource.getState())){
      LOGGER.trace("Setting initial resource state to VOLATILE.");
      resource.setState(DataResource.State.VOLATILE);
    }

    DataResource newResource = dataResourceService.createOrUpdate(resource);

    String etag = Integer.toString(resource.hashCode());
    return ResponseEntity.created(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).getById(newResource.getId(), request, response)).toUri()).eTag("\"" + etag + "\"").body(newResource);
  }

  @Override
  public ResponseEntity<DataResource> getById(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      LOGGER.debug("No data resource found for identifier {}. Returning HTTP 404.", id);
      return ResponseEntity.notFound().build();
    }

    DataResource resource = result.get();

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

//    //check revokation state and access permissions
//    if(!resource.getState().equals(DataResource.State.REVOKED)){
//      LOGGER.debug("Resource is not revoked. Checking for READ permissions for principal identifiers {}.", AuthenticationHelper.getPrincipalIdentifiers());
//      //resource is not revoked, check READ permissions
//      if(!AclUtils.hasPermission(AuthenticationHelper.getPrincipalIdentifiers(), resource, AclEntry.PERMISSION.READ)){
//        throw new AccessForbiddenException("Resource access restricted by acl.");
//      } else{
//        LOGGER.debug("READ access granted.");
//      }
//    } else{
//      //resource is revoked, check ADMINISTRATE or ADMINISTRATOR permissions
//      LOGGER.debug("Resource has been revoked. Checking for ADMINISTRATE permissions for principal identifiers {}.", AuthenticationHelper.getPrincipalIdentifiers());
//      if(!AclUtils.hasPermission(AuthenticationHelper.getPrincipalIdentifiers(), resource, AclEntry.PERMISSION.ADMINISTRATE) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
//        //no access, return 404 as resource has been revoked
//        return ResponseEntity.notFound().build();
//      } else{
//        LOGGER.debug("READ access to revoked resource granted.");
//      }
//    }
    DataResource modResource = resource;

    if(!DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
      //exclude ACLs if not administrate or administrator permissions are set
      modResource = json.use(JsonView.with(resource)
              .onClass(DataResource.class, match().exclude("acls")))
              .returnValue();
    } else{
      LOGGER.debug("Administrator access detected, keepting ACL information in resources.");
    }
    String etag = Integer.toString(resource.hashCode());
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(modResource);
  }

  @Override
  public ResponseEntity<List<DataResource>> findAll(Pageable pgbl, WebRequest request, final HttpServletResponse response, final UriComponentsBuilder uriBuilder){
    return findByExample(null, pgbl, request, response, uriBuilder);
  }

  @Override
  public ResponseEntity<List<DataResource>> findByExample(@RequestBody DataResource example,
          Pageable pgbl,
          WebRequest req,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    int pageSize = pgbl.getPageSize();
    if(pageSize > 100){
      LOGGER.debug("Restricting user-provided page size {} to max. page size 100.", pageSize);
      pageSize = 100;
    }

    LOGGER.debug("Rebuilding page request for page {}, size {} and sort {}.", pgbl.getPageNumber(), pageSize, pgbl.getSort());
    PageRequest request = PageRequest.of(pgbl.getPageNumber(), pageSize, pgbl.getSort());
    Page<DataResource> page;

    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      //do find all
      LOGGER.debug("Administrator access detected. Calling findAll() with example {} and page request {}.", example, request);
      page = dataResourceService.findAll(example, request, true);
    } else{
      //query based on membership
      LOGGER.debug("Non-Administrator access detected. Calling findAll() with READ permissions, example {}, principal identifiers {} and page request {}.", example, AuthenticationHelper.getAuthorizationIdentities(), request);
      page = dataResourceService.findAll(example, AuthenticationHelper.getAuthorizationIdentities(), PERMISSION.READ, request, false);
    }

    if(pgbl.getPageNumber() > page.getTotalPages()){
      LOGGER.debug("Requested page number {} is too large. Number of pages is: {}. Returning empty list.", pgbl.getPageNumber(), page.getTotalPages());
    }

    eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(DataResource.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), pageSize));
    //publish listing event??
    List<DataResource> modResources = page.getContent();
    if(modResources.isEmpty()){
      LOGGER.debug("No data resource found for example {} and principal identifiers {}. Returning empty result.", example, AuthenticationHelper.getAuthorizationIdentities());
    }
    if(!AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
      //exclude ACLs if no administrator permissions are set
      modResources = json.use(JsonView.with(page.getContent())
              .onClass(DataResource.class, match().exclude("acls")))
              .returnValue();
    } else{
      LOGGER.debug("Administrator access detected, keepting ACL information in resources.");
    }
    return ResponseEntity.ok(modResources);
  }

  @Override
  public ResponseEntity patch(@PathVariable("id") final Long id, @RequestBody JsonPatch patch, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }

    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      LOGGER.debug("No data resource found for identifier {}. Returning HTTP 404.", id);
      return ResponseEntity.notFound().build();
    }

    DataResource resource = result.get();

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

    if(!request.checkNotModified(Integer.toString(resource.hashCode()))){
      LOGGER.debug("Provided etag is not matching resource etag {}. Returning HTTP 412.", Integer.toString(resource.hashCode()));
      throw new EtagMismatchException("ETag not matching, resource has changed.");
    }

    PERMISSION callerPermission = DataResourceUtils.getAccessPermission(resource);
    boolean callerIsAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());

    Collection<GrantedAuthority> userGrants = new ArrayList<>();
    userGrants.add(new SimpleGrantedAuthority(callerPermission.getValue()));

    if(callerIsAdmin){
      LOGGER.debug("Administrator access detected. Adding ADMINISTRATOR role to granted authorities.");
      userGrants.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.getValue()));
    }
    DataResource updated = PatchUtil.applyPatch(resource, patch, DataResource.class, userGrants);

    LOGGER.info("Persisting patched resource.");
    dataResourceService.createOrUpdate(updated);
    LOGGER.info("Resource successfully persisted.");
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity delete(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      LOGGER.debug("Anonymous access to DELETE permitted. Returning HTTP 401.");
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }

    Optional<DataResource> result = dataResourceService.findById(id);
    if(result.isPresent()){
      DataResource resource = result.get();
      if(DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) || AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
        if(!request.checkNotModified(Integer.toString(resource.hashCode()))){
          LOGGER.debug("Provided etag is not matching resource etag {}. Returning HTTP 412.", Integer.toString(resource.hashCode()));
          throw new EtagMismatchException("ETag not matching, resource has changed.");
        }
        LOGGER.debug("Setting resource state to REVOKED.");
        resource.setState(DataResource.State.REVOKED);
        LOGGER.debug("Persisting revoked resource.");
        dataResourceService.createOrUpdate(resource);
      } else{
        throw new UpdateForbiddenException("Insufficient role. ADMINISTRATE permission or ROLE_ADMINISTRATOR required.");
      }
    } else{
      LOGGER.debug("No data resource found for identifier {}. Returning HTTP 204.", id);
    }
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity handleFileUpload(@PathVariable(value = "id") final Long id,
          @RequestPart(name = "file", required = false) MultipartFile file,
          @RequestPart(name = "metadata", required = false) ContentInformation contentInformation,
          @RequestParam(name = "force", defaultValue = "false") boolean force,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Please login in order to be able to upload data.");
    }
    //obtain relative path
    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }

    //@TODO Escape path for file system access
    String path = requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
    if(path == null || path.endsWith("/")){
      throw new BadArgumentException("Provided path is invalid. Path must not be empty and must not end with a slash.");
    }
    //check data resource and permissions
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      return ResponseEntity.notFound().build();
    }
    DataResource resource = result.get();
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);
    //check for existing content information
    ContentInformation contentInfo;

    Optional<ContentInformation> existingContentInformation = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, path, null);

    Path toRemove = null;
    if(existingContentInformation.isPresent()){
      //existing content, overwrite necessary
      if(!force){
        //conflict
        throw new ResourceAlreadyExistException("There is already content registered at " + path + ". Provide force=true in order to overwrite the existing resource.");
      } else{
        //overwrite...mark file for deletion
        contentInfo = existingContentInformation.get();
        URI contentUri = URI.create(contentInfo.getContentUri());
        if("file".equals(contentUri.getScheme())){
          //mark file for removal
          toRemove = Paths.get(URI.create(contentInfo.getContentUri()));
        }//content URI is not pointing to a file...just replace the entry
      }
    } else{
      //no existing content information, create new or take provided
      contentInfo = (contentInformation != null) ? contentInformation : ContentInformation.createContentInformation(path);
      contentInfo.setId(null);
      contentInfo.setParentResource(resource);
      contentInfo.setRelativePath(path);
    }

    if(file != null){
      //file upload
      URI dataUri = PathUtils.getDataUri(contentInfo.getParentResource(), contentInfo.getRelativePath(), applicationProperties);
      Path destination = Paths.get(dataUri);
      //store data
      OutputStream out = null;
      try{
        //read/write file, create checksum and calculate file size
        Files.createDirectories(destination.getParent());

        InputStream in = file.getInputStream();
        MessageDigest md = MessageDigest.getInstance("SHA1");

        int cnt;
        long bytes = 0;
        byte[] buffer = new byte[1024];
        out = Files.newOutputStream(destination);
        while((cnt = in.read(buffer)) > -1){
          out.write(buffer, 0, cnt);
          md.update(buffer, 0, cnt);
          bytes += cnt;
        }

        contentInfo.setHash("sha1:" + Hex.encodeHexString(md.digest()));
        contentInfo.setSize(bytes);
        contentInfo.setContentUri(dataUri.toString());

        try(InputStream is = Files.newInputStream(destination); BufferedInputStream bis = new BufferedInputStream(is);){
          AutoDetectParser parser = new AutoDetectParser();
          Detector detector = parser.getDetector();
          Metadata md1 = new Metadata();
          md1.add(Metadata.RESOURCE_NAME_KEY, contentInfo.getFilename());
          org.apache.tika.mime.MediaType mediaType = detector.detect(bis, md1);
          contentInfo.setMediaType(mediaType.toString());
        }
      } catch(IOException ex){
        LOGGER.error("Failed to finish upload.", ex);
        throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
      } catch(NoSuchAlgorithmException ex){
        LOGGER.error("Failed to initialize SHA1 message digest. File upload won't be possible.", ex);
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
      //no file upload, take data reference URI from provided content information
      if(contentInformation == null || contentInformation.getContentUri() == null){
        throw new BadArgumentException("Neither a file upload nor an external content URI were provided.");
      } else{
        if("file".equals(URI.create(contentInfo.getContentUri()).getScheme().toLowerCase()) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
          throw new BadArgumentException("You are not permitted to add content information with URI scheme of type 'file'.");
        }
        //take content uri and provided checksum and size, if available
        contentInfo.setContentUri(contentInformation.getContentUri());
        contentInfo.setSize(contentInformation.getSize());
        contentInfo.setHash(contentInformation.getHash());
      }
    }

    //copy metadata and tags from provided content information if available
    if(contentInformation != null){
      if(contentInformation.getMetadata() != null){
        contentInfo.setMetadata(contentInformation.getMetadata());
      }
      if(contentInformation.getTags() != null){
        contentInfo.setTags(contentInformation.getTags());
      }
    }

    URI link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).handleFileDownload(id, PageRequest.of(0, 1), request, response, uriBuilder)).toUri();

    try{
      contentInformationService.createOrUpdate(contentInfo);
      URIBuilder builder = new URIBuilder(link);
      builder.setPath(builder.getPath().replace("**", path));
      return ResponseEntity.created(builder.build()).build();
    } catch(URISyntaxException ex){
      LOGGER.error("Failed to create location URI for path " + path + ". However, resource should be created.", ex);
      return ResponseEntity.created(link).build();
    } finally{
      if(toRemove != null){
        try{
          Files.deleteIfExists(toRemove);
        } catch(IOException ex){
          LOGGER.warn("Failed to remove previously existing data at " + toRemove + ". Manual removal required.", ex);
        }
      }
    }

    // ApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfiguration.class);
    //  AmqpTemplate template = context.getBean(AmqpTemplate.class);
    // rabbitTemplate.convertAndSend("topic_note", "note.data.update", path + "/" + file.getOriginalFilename());
  }

  @Override
  public ResponseEntity handleMetadataAccess(@PathVariable(value = "id") final Long id,
          @RequestParam(name = "tag", required = false) String tag,
          final Pageable pgbl,
          WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    //obtain accessed path
    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }
    String relPath = requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
    //check resource and permission
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      return ResponseEntity.notFound().build();
    }
    DataResource resource = result.get();
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    if(relPath.startsWith("/")){
      //remove leading slash if present, e.g. if path was empty
      relPath = relPath.substring(1);
    }

    if(relPath.endsWith("/") || relPath.length() == 0){
      //collection listing
      relPath += "%";
      //sanitize page request
      int pageSize = pgbl.getPageSize();
      if(pageSize > 100){
        LOGGER.debug("Restricting user-provided page size {} to max. page size 100.", pageSize);
        pageSize = 100;
      }

      LOGGER.debug("Rebuilding page request for page {}, size {} and sort {}.", pgbl.getPageNumber(), pageSize, pgbl.getSort());
      Sort pgblSort = pgbl.getSort();
      if(pgblSort.equals(Sort.unsorted())){
        pgblSort = Sort.by(Sort.Order.asc("depth"), Sort.Order.asc("relativePath"));
      }

      PageRequest pageRequest = PageRequest.of(pgbl.getPageNumber(), pageSize, pgblSort);

      //obtain page according to request
      Page<ContentInformation> page = contentInformationService.findByParentResourceIdEqualsAndRelativePathLikeAndHasTag(id, relPath, tag, pageRequest);

      //wrong header added!
      // eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(ContentInformation.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), pageSize));
      //result list found, remove data resource information except id and return
      List<ContentInformation> resultList = json.use(JsonView.with(page.getContent())
              .onClass(DataResource.class, match().exclude("*").include("id")))
              .returnValue();
      return ResponseEntity.ok(resultList);
    } else{
      //try to obtain single content element matching path exactly
      Optional<ContentInformation> existingContentInformation = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, relPath, tag);

      if(existingContentInformation.isPresent()){
        //single entry found, remove data resource information except id and return
        ContentInformation contentInformation = existingContentInformation.get();

        contentInformation = json.use(JsonView.with(contentInformation)
                .onClass(DataResource.class, match().exclude("*").include("id")))
                .returnValue();
        String etag = Integer.toString(resource.hashCode());
        return ResponseEntity.ok().eTag("\"" + etag + "\"").body(contentInformation);
      } else{
        return ResponseEntity.notFound().build();
      }
    }
  }

  @Override
  public ResponseEntity handleFileDownload(@PathVariable(value = "id") final Long id,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }
    String path = requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());

    //check resource and permission
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      return ResponseEntity.notFound().build();
    }
    DataResource resource = result.get();
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    //try to obtain single content element matching path exactly
    Optional<ContentInformation> existingContentInformation = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, path, null);
    if(existingContentInformation.isPresent()){
      //single entry found, remove data resource information except id and return
      ContentInformation contentInformation = existingContentInformation.get();
      //obtain data uri and check for content to exist
      String dataUri = contentInformation.getContentUri();
      URI uri = URI.create(dataUri);
      if(null == uri.getScheme()){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Location", uri.toString());
        return new ResponseEntity<>(null, headers, HttpStatus.NO_CONTENT);
      } else{
        //perform direct download if scheme is file
        switch(uri.getScheme()){
          case "file":
            if(!Files.exists(Paths.get(uri))){
              throw new ResourceNotFoundException("The provided resource was not found on the server.");
            }
            return ResponseEntity.
                    ok().
                    contentType((contentInformation.getMediaTypeAsObject() != null) ? contentInformation.getMediaTypeAsObject() : MediaType.APPLICATION_OCTET_STREAM).
                    header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + contentInformation.getFilename() + "\"").
                    body(new FileSystemResource(new File(uri)));
          case "http":
          case "https": {
            //try to redirect transfer if scheme is not file
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(uri.toString());
            HttpHeaders headers = new HttpHeaders();
            HttpStatus returnedStatus;
            try{
              CloseableHttpResponse httpResponse = client.execute(httpGet);
              HttpStatus responseStatus = HttpStatus.resolve(httpResponse.getStatusLine().getStatusCode());

              if(responseStatus == null){
                LOGGER.warn("Received unknown response status " + httpResponse.getStatusLine().getStatusCode() + " while accessing resource URI " + uri.toString() + ". Returning Content-Location header with value " + uri.toString() + " and HTTP SERVICE_UNAVAILABLE.");
                headers.add("Content-Location", uri.toString());
                returnedStatus = HttpStatus.SERVICE_UNAVAILABLE;
              } else{
                returnedStatus = responseStatus;
                switch(responseStatus){
                  case OK: {
                    //add location header in order to trigger redirect
                    returnedStatus = HttpStatus.SEE_OTHER;
                    headers.add("Location", uri.toString());
                    break;
                  }
                  case SEE_OTHER:
                  case FOUND:
                  case MOVED_PERMANENTLY:
                  case TEMPORARY_REDIRECT:
                  case PERMANENT_REDIRECT: {
                    Header location = httpResponse.getFirstHeader("Location");
                    if(location != null){
                      headers.add("Location", location.getValue());
                    } else{
                      LOGGER.error("Received status " + responseStatus + " but no location header.");
                      returnedStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                    }
                    break;
                  }
                  default: {
                    LOGGER.warn("Received status " + responseStatus + ". Returning Content-Location header with value " + uri.toString() + " and HTTP NO_CONTENT.");
                    returnedStatus = HttpStatus.NO_CONTENT;
                    headers.add("Content-Location", uri.toString());
                  }
                }
              }
            } catch(IOException ex){
              LOGGER.error("Failed to resolve content URI " + uri + ". Sending HTTP SERVICE_UNAVAILABLE.", ex);
              returnedStatus = HttpStatus.SERVICE_UNAVAILABLE;
              headers.add("Content-Location", uri.toString());
            }
            return new ResponseEntity<>(null, headers, returnedStatus);
          }
          default:
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Location", uri.toString());

            return new ResponseEntity<>(null, headers, HttpStatus.NO_CONTENT);
        }
      }
    } else{
      ///distinguish between 'nothing found, even no collection' and 'collection found but download not implemented'
      //no single result was found, path is representing a virtual folder
      //add zipped download here later
      throw new FeatureNotImplementedException("There is no data resource at this location and collection download is not implemented, yet.");
    }
  }

  @Override
  public ResponseEntity patchMetadata(@PathVariable(value = "id") final Long id,
          @RequestBody JsonPatch patch,
          final WebRequest request,
          final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }
    //obtain accessed path
    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }
    String path = requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
    //check resource and permission
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      return ResponseEntity.notFound().build();
    }
    DataResource resource = result.get();
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);
    //try to obtain single content element matching path exactly
    Optional<ContentInformation> existingContentInformation = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, path, null);
    if(existingContentInformation.isPresent()){
      ContentInformation toUpdate = existingContentInformation.get();
      if(!request.checkNotModified(Integer.toString(resource.hashCode()))){
        throw new EtagMismatchException("ETag not matching, resource has changed.");
      }

      PERMISSION callerPermission = DataResourceUtils.getAccessPermission(resource);
      boolean callerIsAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());
      Collection<GrantedAuthority> userGrants = new ArrayList<>();
      userGrants.add(new SimpleGrantedAuthority(callerPermission.getValue()));

      if(callerIsAdmin){
        LOGGER.debug("Admin access detected. Adding ADMINISTRATOR role to granted authorities.");
        userGrants.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.getValue()));
      }

      ContentInformation updated = PatchUtil.applyPatch(toUpdate, patch, ContentInformation.class, userGrants);
      LOGGER.info("Persisting patched content information.");
      contentInformationService.createOrUpdate(updated);
      LOGGER.info("Content information successfully persisted.");
      return ResponseEntity.noContent().build();
    } else{
      throw new ResourceNotFoundException("No content information found for resource with identifier " + id + " at path " + path);
    }
  }

  @Override
  public ResponseEntity deleteContent(@PathVariable(value = "id") final Long id, WebRequest request, HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }

    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }
    String path = requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
    //check resource and permission
    Optional<DataResource> result = dataResourceService.findById(id);
    if(!result.isPresent()){
      return ResponseEntity.notFound().build();
    }
    DataResource resource = result.get();
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.ADMINISTRATE);
    //try to obtain single content element matching path exactly
    Optional<ContentInformation> existingContentInformation = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, path, null);
    if(existingContentInformation.isPresent()){
      if(!request.checkNotModified(Integer.toString(resource.hashCode()))){
        throw new EtagMismatchException("ETag not matching, resource has changed.");
      }

      Path toRemove = null;
      ContentInformation contentInfo = existingContentInformation.get();
      URI contentUri = URI.create(contentInfo.getContentUri());
      if("file".equals(contentUri.getScheme())){
        //mark file for removal
        toRemove = Paths.get(URI.create(contentInfo.getContentUri()));
      }//content URI is not pointing to a file...just replace the entry

      contentInformationService.delete(contentInfo);

      if(toRemove != null){
        try{
          Files.deleteIfExists(toRemove);
        } catch(IOException ex){
          LOGGER.warn("Failed to remove data at " + toRemove + ". Manual removal required.", ex);
        }
      }
    }

    return ResponseEntity.noContent().build();
  }

}
