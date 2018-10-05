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
import com.monitorjbl.json.Match;
import static com.monitorjbl.json.Match.match;
import edu.kit.datamanager.controller.hateoas.event.PaginatedResultsRetrievedEvent;
import io.swagger.annotations.Api;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.EtagMismatchException;
import edu.kit.datamanager.exceptions.UnauthorizedAccessException;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.FeatureNotImplementedException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.service.IContentProvider;
import edu.kit.datamanager.service.impl.FileContentProvider;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.PatchUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.FileCopyUtils;
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
  private IContentProvider[] contentProviders;

  public DataResourceController(IDataResourceService dataResourceService, IContentInformationService contentInformationService){//, IContentProvider[] contentProviders){
    super();
    this.dataResourceService = dataResourceService;
    this.contentInformationService = contentInformationService;
  }

  @Override
  public ResponseEntity<DataResource> create(@RequestBody DataResource resource, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Anonymous resource creation disabled.");
    }

    DataResource result = dataResourceService.create(resource);

    return ResponseEntity.created(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).getById(result.getId(), request, response)).toUri()).eTag("\"" + Integer.toString(result.hashCode()) + "\"").body(result);
  }

  @Override
  public ResponseEntity<DataResource> getById(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    DataResource resource = dataResourceService.getById(id, PERMISSION.READ);
    //filter resource if necessary and return it automatically
    filterAndAutoReturnResource(resource);
    //trigger response creation and set etag...the response body is set automatically
    return ResponseEntity.ok().eTag("\"" + Integer.toString(resource.hashCode()) + "\"").build();
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
    final int pageSize = pgbl.getPageSize() <= 100 ? pgbl.getPageSize() : 100;

    LOGGER.debug("Rebuilding page request for page {}, size {} and sort {}.", pgbl.getPageNumber(), pageSize, pgbl.getSort());
    PageRequest request = PageRequest.of(pgbl.getPageNumber(), pageSize, pgbl.getSort());

    List<DataResource> resources = dataResourceService.findByExample(example, request, (Integer currentPage, Integer totalPages) -> {
      eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(DataResource.class, uriBuilder, response, currentPage, totalPages, pageSize));
    });

    filterAndAutoReturnResource(resources);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity patch(@PathVariable("id") final Long id, @RequestBody JsonPatch patch, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }

    dataResourceService.patch(id, (String t) -> request.checkNotModified(t), patch);

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity delete(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    if(AuthenticationHelper.isAnonymous()){
      LOGGER.debug("Anonymous access to DELETE permitted. Returning HTTP 401.");
      throw new UnauthorizedAccessException("Please login in order to be able to modify resources.");
    }

    dataResourceService.delete(id, (String t) -> request.checkNotModified(t));

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

    URI link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).handleFileDownload(resource.getId(), PageRequest.of(0, 1), request, response, uriBuilder)).toUri();

    try{
      contentInformationService.create(contentInformation, resource, (file != null) ? file.getInputStream() : null, path, force);
      URIBuilder builder = new URIBuilder(link);
      builder.setPath(builder.getPath().replace("**", path));
      return ResponseEntity.created(builder.build()).build();
    } catch(URISyntaxException ex){
      LOGGER.error("Failed to create location URI for path " + path + ". However, resource should be created.", ex);
      return ResponseEntity.created(link).build();
    } catch(IOException ex){
      LOGGER.error("Failed to open file input stream.", ex);
      throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
    }

    // ApplicationContext context = new AnnotationConfigApplicationContext(RabbitMQConfiguration.class);
//      AmqpTemplate template = context.getBean(AmqpTemplate.class);
//      
//     rabbitTemplate.convertAndSend("topic_note", "note.data.update", path + "/" + file.getOriginalFilename());
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

    if(relPath.startsWith("/")){
      //remove leading slash if present, e.g. if path was empty
      relPath = relPath.substring(1);
    }
    //check resource and permission
    DataResource resource = dataResourceService.getById(id, PERMISSION.READ);

    //switch between collection and element listing
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

      List<ContentInformation> resultList = contentInformationService.getContentInformation(id, relPath, tag, pageRequest);
      filterAndAutoReturnContentInformation(resultList);
      LOGGER.debug("Obtained {} content information result(s).", resultList.size());
      return ResponseEntity.ok().build();
    } else{
      ContentInformation contentInformation = contentInformationService.getContentInformation(id, relPath, tag);
      filterAndAutoReturnContentInformation(contentInformation);
      LOGGER.debug("Obtained single content information result.");
      return ResponseEntity.ok().eTag("\"" + Integer.toString(resource.hashCode()) + "\"").build();
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

    DataResource resource = dataResourceService.getById(id, PERMISSION.READ);

    LOGGER.debug("Access to resource with identifier {} granted. Continue with content access.", resource.getResourceIdentifier());
    //try to obtain single content element matching path exactly
    ContentInformation contentInformation = contentInformationService.getContentInformation(id, path, null);
    //obtain data uri and check for content to exist
    String dataUri = contentInformation.getContentUri();
    URI uri = URI.create(dataUri);
    LOGGER.debug("Trying to provide content at URI {} by any configured content provider.", uri);
    for(IContentProvider contentProvider : contentProviders){
      if(contentProvider.canProvide(uri.getScheme())){
        return contentProvider.provide(uri, contentInformation.getMediaTypeAsObject(), contentInformation.getFilename());
      }
    }

    LOGGER.info("No content provider found for URI {}. Returning URI in Content-Location header.", uri);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", uri.toString());
    return new ResponseEntity<>(null, headers, HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity patchMetadata(@PathVariable(value = "id")
          final Long id,
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
    DataResource resource = dataResourceService.getById(id, PERMISSION.WRITE);
    LOGGER.debug("Successfully obtained resource with identifier {}. Continue with content check.", resource.getResourceIdentifier());

    //try to obtain single content element matching path exactly
    ContentInformation toUpdate = contentInformationService.getContentInformation(id, path, null);

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
  }

  @Override
  public ResponseEntity deleteContent(@PathVariable(value = "id")
          final Long id, WebRequest request, HttpServletResponse response){
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
    Optional<ContentInformation> contentInfoOptional = contentInformationService.findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(id, path, null);
    if(contentInfoOptional.isPresent()){
      LOGGER.debug("Content information entry found. Checking ETag.");
      ContentInformation contentInfo = contentInfoOptional.get();
      if(!request.checkNotModified(Integer.toString(resource.hashCode()))){
        throw new EtagMismatchException("ETag not matching, resource has changed.");
      }

      Path toRemove = null;
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

  private void filterAndAutoReturnResource(DataResource resource){
    Match match = match();
    if(!DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
      //exclude ACLs if not administrate or administrator permissions are set
      match = match.exclude("acls");
    } else{
      LOGGER.debug("Administrator access detected, keeping ACL information in resources.");
    }

    //transform and return JSON representation as next controller result
    json.use(JsonView.with(resource)
            .onClass(DataResource.class,
                    match))
            .returnValue();
  }

  private void filterAndAutoReturnResource(List<DataResource> resources){
    Match match = match();
    if(!AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
      //exclude ACLs if not administrate or administrator permissions are set
      match = match.exclude("acls");
    } else{
      LOGGER.debug("Administrator access detected, keeping ACL information in resources.");
    }

    //transform and return JSON representation as next controller result
    json.use(JsonView.with(resources)
            .onClass(DataResource.class,
                    match))
            .returnValue();
  }

  private void filterAndAutoReturnContentInformation(ContentInformation resource){
    //transform and return JSON representation as next controller result
    json.use(JsonView.with(resource)
            .onClass(DataResource.class, match().exclude("*").include("id")))
            .returnValue();
  }

  private void filterAndAutoReturnContentInformation(List<ContentInformation> resources){
    //transform and return JSON representation as next controller result
    json.use(JsonView.with(resources)
            .onClass(DataResource.class, match().exclude("*").include("id")))
            .returnValue();

  }

}
