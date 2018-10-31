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
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.service.IContentProvider;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.websocket.server.PathParam;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
  private IContentProvider[] contentProviders;

  /**
   * Default constructor.
   *
   * @param dataResourceService Data resource service instance added e.g. via
   * dependency injection.
   * @param contentInformationService Content information service instance added
   * e.g. via dependency injection.
   */
  public DataResourceController(IDataResourceService dataResourceService, IContentInformationService contentInformationService){
    super();
    this.dataResourceService = dataResourceService;
    this.contentInformationService = contentInformationService;
  }

  @Override
  public ResponseEntity<DataResource> create(@RequestBody DataResource resource, WebRequest request, final HttpServletResponse response){
    ControllerUtils.checkAnonymousAccess();

    DataResource result = dataResourceService.create(resource,
            (String) AuthenticationHelper.getAuthentication().getPrincipal(),
            AuthenticationHelper.getFirstname(),
            AuthenticationHelper.getLastname());

    return ResponseEntity.created(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).getById(result.getId(), request, response)).toUri()).eTag("\"" + result.getEtag() + "\"").body(result);
  }

  @Override
  public ResponseEntity<DataResource> getById(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    //filter resource if necessary and return it automatically
    filterAndAutoReturnResource(resource);
    //trigger response creation and set etag...the response body is set automatically
    return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").build();
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
    PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);

    Page<DataResource> page = dataResourceService.findByExample(example, AuthenticationHelper.getAuthorizationIdentities(),
            AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString()),
            request);

    eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(DataResource.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), request.getPageSize()));

    filterAndAutoReturnResources(page.getContent());

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity patch(@PathVariable("id") final Long id, @RequestBody JsonPatch patch, WebRequest request, final HttpServletResponse response){
    ControllerUtils.checkAnonymousAccess();

    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

    ControllerUtils.checkEtag(request, resource);

    dataResourceService.patch(resource, patch, getUserAuthorities(resource));

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity delete(@PathVariable("id") final Long id, WebRequest request, final HttpServletResponse response){
    ControllerUtils.checkAnonymousAccess();

    try{
      DataResource resource = dataResourceService.findById(id);
      LOGGER.trace("Resource found. Checking for permission {} or role {}.", PERMISSION.ADMINISTRATE, RepoUserRole.ADMINISTRATOR);
      if(DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) || AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.getValue())){
        LOGGER.trace("Permissions found. Continuing with DELETE operation.");
        ControllerUtils.checkEtag(request, resource);

        dataResourceService.delete(resource);
      } else{
        throw new UpdateForbiddenException("Insufficient permissions. ADMINISTRATE permission or ROLE_ADMINISTRATOR required.");
      }
    } catch(ResourceNotFoundException ex){
      //ignored
      LOGGER.info("Resource with id {} not found. Returning with HTTP NO_CONTENT.", id);
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
    ControllerUtils.checkAnonymousAccess();

    String path = getContentPathFromRequest(request);

    //@TODO escape path properly
    if(path == null || path.endsWith("/")){
      throw new BadArgumentException("Provided path is invalid. Path must not be empty and must not end with a slash.");
    }
    //check data resource and permissions
    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

    URI link = ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(this.getClass()).handleFileDownload(resource.getId(), PageRequest.of(0, 1), request, response, uriBuilder)).toUri();

    try{
      contentInformationService.create(contentInformation, resource, path, (file != null) ? file.getInputStream() : null, force);
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
  }

  @Override
  public ResponseEntity handleMetadataAccess(@PathVariable(value = "id") final Long id,
          @RequestParam(name = "tag", required = false) String tag,         
          final Pageable pgbl,
          WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    String path = getContentPathFromRequest(request);
    //check resource and permission
    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    LOGGER.trace("Checking provided path {}.", path);
    if(path.startsWith("/")){
      LOGGER.debug("Removing leading slash from path {}.", path);
      //remove leading slash if present, e.g. if path was empty
      path = path.substring(1);
    }

    //switch between collection and element listing
    if(path.endsWith("/") || path.length() == 0){
      LOGGER.trace("Path ends with slash or is empty. Performing collection access.");
      //collection listing
      path += "%";
      //sanitize page request

      PageRequest pageRequest = ControllerUtils.checkPaginationInformation(pgbl, pgbl.getSort().equals(Sort.unsorted()) ? Sort.by(Sort.Order.asc("depth"), Sort.Order.asc("relativePath")) : pgbl.getSort());

      LOGGER.trace("Obtaining content information page for parent resource {}, path {} and tag {}. Page information are: {}", id, path, tag, pageRequest);
      Page<ContentInformation> resultList = contentInformationService.findAll(ContentInformation.createContentInformation(id, path, tag), pageRequest);

      LOGGER.trace("Obtained {} content information result(s).", resultList.getContent().size());
      filterAndAutoReturnContentInformation(resultList.getContent());
      return ResponseEntity.ok().build();
    } else{
      LOGGER.trace("Path does not end with slash and/or is not empty. Assuming single element access.");
      ContentInformation contentInformation = contentInformationService.getContentInformation(id, path);
      filterAndAutoReturnContentInformation(contentInformation);
      LOGGER.trace("Obtained single content information result.");
      return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").build();
    }
  }

  @Override
  public ResponseEntity handleFileDownload(@PathVariable(value = "id") final Long id,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder){
    String path = getContentPathFromRequest(request);

    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    LOGGER.debug("Access to resource with identifier {} granted. Continue with content access.", resource.getResourceIdentifier());
    //try to obtain single content element matching path exactly
    ContentInformation contentInformation = contentInformationService.getContentInformation(id, path);
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
    ControllerUtils.checkAnonymousAccess();

    String path = getContentPathFromRequest(request);

    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    ControllerUtils.checkEtag(request, resource);

    ContentInformation toUpdate = contentInformationService.getContentInformation(id, path);

    contentInformationService.patch(toUpdate, patch, getUserAuthorities(resource));

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity deleteContent(@PathVariable(value = "id")
          final Long id, WebRequest request, HttpServletResponse response){
    ControllerUtils.checkAnonymousAccess();

    String path = getContentPathFromRequest(request);

    //check resource and permission
    DataResource resource = dataResourceService.findById(id);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.ADMINISTRATE);

    ControllerUtils.checkEtag(request, resource);

    //try to obtain single content element matching path exactly
    Page<ContentInformation> contentInfoOptional = contentInformationService.findAll(ContentInformation.createContentInformation(id, path), PageRequest.of(0, 1));
    if(contentInfoOptional.hasContent()){
      LOGGER.debug("Content information entry found. Checking ETag.");

      ContentInformation contentInfo = contentInfoOptional.getContent().get(0);

      Path localContentToRemove = null;
      URI contentUri = URI.create(contentInfo.getContentUri());
      LOGGER.trace("Checking if content URI {} is pointing to a local file.", contentInfo);
      if("file".equals(contentUri.getScheme())){
        //mark file for removal
        localContentToRemove = Paths.get(URI.create(contentInfo.getContentUri()));
      } else{
        //content URI is not pointing to a file...just replace the entry
        LOGGER.trace("Content to delete is pointing to {}. Local content deletion will be skipped.", contentInfo.getContentUri());
      }
      contentInformationService.delete(contentInfo);

      if(localContentToRemove != null){
        try{
          LOGGER.trace("Removing content file {}.", localContentToRemove);
          Files.deleteIfExists(localContentToRemove);
        } catch(IOException ex){
          LOGGER.warn("Failed to remove data at " + localContentToRemove + ". Manual removal required.", ex);
        }
      } else{
        LOGGER.trace("No local content file exists. Returning from DELETE.");
      }
    }

    return ResponseEntity.noContent().build();
  }

  private String getContentPathFromRequest(WebRequest request){
    String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    if(requestedUri == null){
      throw new CustomInternalServerError("Unable to obtain request URI.");
    }
    return requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
  }

  private Collection<? extends GrantedAuthority> getUserAuthorities(DataResource resource){
    LOGGER.trace("Determining user grants from authorization context.");
    Collection<GrantedAuthority> userGrants = new ArrayList<>();
    userGrants.add(new SimpleGrantedAuthority(DataResourceUtils.getAccessPermission(resource).getValue()));

    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.trace("Administrator access detected. Adding role {} to granted authorities.", RepoUserRole.ADMINISTRATOR.getValue());
      userGrants.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.getValue()));
    }

    return userGrants;
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
    json.use(JsonView.with(resource).onClass(DataResource.class, match));
  }

  private void filterAndAutoReturnResources(List<DataResource> resources){
    Match match = match();
    if(!AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
      //exclude ACLs if not administrate or administrator permissions are set
      match = match.exclude("acls");
    } else{
      LOGGER.debug("Administrator access detected, keeping ACL information in resources.");
    }

    //transform and return JSON representation as next controller result
    json.use(JsonView.with(resources).onClass(DataResource.class, match));
  }

  private void filterAndAutoReturnContentInformation(ContentInformation resource){
    //hide all attributes but the id from the parent data resource in the content information entity
    json.use(JsonView.with(resource).onClass(DataResource.class, match().exclude("*").include("id")));
  }

  private void filterAndAutoReturnContentInformation(List<ContentInformation> resources){
    //hide all attributes but the id from the parent data resource in all content information entities
    json.use(JsonView.with(resources).onClass(DataResource.class, match().exclude("*").include("id")));
  }

}
