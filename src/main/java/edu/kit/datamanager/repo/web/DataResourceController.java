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
import com.google.common.base.Objects;
import edu.kit.datamanager.controller.hateoas.event.PaginatedResultsRetrievedEvent;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceElsewhereException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
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
@Schema(description = "Data Resource Management")
public class DataResourceController implements IDataResourceController {

    // private final JsonResult json = JsonResult.instance();
    @Autowired
    private Logger LOGGER;

    @Autowired
    private final IDataResourceService dataResourceService;
    @Autowired
    private final IAuditService<DataResource> auditService;
    @Autowired
    private final IAuditService<ContentInformation> contentAuditService;
    @Autowired
    private final IContentInformationService contentInformationService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * Default constructor.
     *
     * @param dataResourceService Data resource service instance added e.g. via
     * dependency injection.
     * @param contentInformationService Content information service instance
     * added e.g. via dependency injection.
     */
    public DataResourceController(IDataResourceService dataResourceService, IAuditService<DataResource> auditService, IAuditService<ContentInformation> contentAuditService, IContentInformationService contentInformationService, ApplicationProperties applicationProperties) {
        super();
        this.dataResourceService = dataResourceService;
        this.contentInformationService = contentInformationService;
        this.auditService = auditService;
        this.contentAuditService = contentAuditService;
        this.applicationProperties = applicationProperties;

        if (!this.applicationProperties.isAuditEnabled() && !"none".equals(this.applicationProperties.getDefaultVersioningService())) {
            throw new IllegalArgumentException("Conflicting configuration properties detected. 'repo.audit.enabled' must be 'true' if 'repo.file.versioning.default' is not 'none'.");
        }
    }

    @Override
    public ResponseEntity<DataResource> create(@RequestBody final DataResource resource,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Create request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();
        DataResource result = dataResourceService.create(resource,
                (String) AuthenticationHelper.getAuthentication().getPrincipal(),
                AuthenticationHelper.getFirstname(),
                AuthenticationHelper.getLastname());
        try {
            LOGGER.trace("Creating controller link for resource identifier {}.", result.getId());
            //do some hacking in order to properly escape the resource identifier
            //if escaping in beforehand, WebMvcLinkBuilder will escape again, which invalidated the link
            String uriLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById("WorkaroundPlaceholder", applicationProperties.isAuditEnabled() ? 1l : null, request, response)).toString();
            //replace placeholder with escaped identifier in order to ensure single-escaping
            uriLink = uriLink.replaceFirst("WorkaroundPlaceholder", URLEncoder.encode(result.getId(), "UTF-8"));
            uriLink = uriLink.substring(0, uriLink.lastIndexOf("?"));

            LOGGER.trace("Created resource link is: {}", uriLink);
            return ResponseEntity.created(URI.create(uriLink)).eTag("\"" + result.getEtag() + "\"").header("Resource-Version", Long.toString(1l)).body(result);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Failed to encode resource identifier " + result.getId() + ".", ex);
            throw new CustomInternalServerError("Failed to decode resource identifier " + result.getId() + ", but resource has been created.");
        }
    }

    @Override
    public ResponseEntity<DataResource> getById(@PathVariable("id") final String identifier,
            @RequestParam(name = "version", required = false) final Long version,
            final WebRequest request,
            final HttpServletResponse response) {
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, version, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, version, request, response)).toString();
        });
        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

        long currentVersion = auditService.getCurrentVersion(identifier);

        if (currentVersion > 0) {
            //trigger response creation and set etag...the response body is set automatically
            return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").header("Resource-Version", Long.toString((version != null) ? version : currentVersion)).body(filterResource(resource));
        } else {
            return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").body(filterResource(resource));
        }
    }

    @Override
    public ResponseEntity<List<DataResource>> findAll(@RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
            @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        return findByExample(null, lastUpdateFrom, lastUpdateUntil, pgbl, request, response, uriBuilder);
    }

    @Override
    public ResponseEntity<List<DataResource>> findByExample(@RequestBody DataResource example,
            @RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
            @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
            final Pageable pgbl,
            final WebRequest req,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);
        Page<DataResource> page = dataResourceService.findByExample(example, lastUpdateFrom, lastUpdateUntil, AuthenticationHelper.getAuthorizationIdentities(),
                AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString()),
                request);
        if (example != null) {
            //comming via dataresources/search
            eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(DataResource.class, "search", uriBuilder, response, page.getNumber(), page.getTotalPages(), request.getPageSize()));
        } else {
            //comming via dataresources/ ... don't add search suffix
            eventPublisher.publishEvent(new PaginatedResultsRetrievedEvent<>(DataResource.class, uriBuilder, response, page.getNumber(), page.getTotalPages(), request.getPageSize()));
        }
        //set content-range header for react-admin (index_start-index_end/total
        response.addHeader("Content-Range", ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
        return ResponseEntity.ok().body(filterResources(page.getContent()));
    }

    @Override
    public ResponseEntity patch(@PathVariable("id") final String identifier,
            @RequestBody final JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Patch request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();

        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patch(t, patch, request, response)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

        ControllerUtils.checkEtag(request, resource);

        dataResourceService.patch(resource, patch, getUserAuthorities(resource));

        long currentVersion = auditService.getCurrentVersion(identifier);
        if (currentVersion > 0) {
            return ResponseEntity.noContent().header("Resource-Version", Long.toString(currentVersion)).build();
        } else {
            return ResponseEntity.noContent().build();

        }
    }

    @Override
    public ResponseEntity put(@PathVariable("id") final String identifier,
            @RequestBody final DataResource newResource,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Put request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).put(t, newResource, request, response)).toString();
        });
        DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

        ControllerUtils.checkEtag(request, resource);
        newResource.setId(resource.getId());

        DataResource result = dataResourceService.put(resource, newResource, getUserAuthorities(resource));

        long currentVersion = auditService.getCurrentVersion(identifier);

        if (currentVersion > 0) {
            //trigger response creation and set etag...the response body is set automatically
            return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").header("Resource-Version", Long.toString(currentVersion)).body(filterResource(result));
        } else {
            return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").body(filterResource(result));

        }
    }

    @Override
    public ResponseEntity delete(@PathVariable("id") final String identifier,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Delete request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();

        try {
            DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
                return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).delete(t, request, response)).toString();
            });
            LOGGER.trace("Resource found. Checking for permission {} or role {}.", PERMISSION.ADMINISTRATE, RepoUserRole.ADMINISTRATOR);
            if (DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) || AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.getValue())) {
                LOGGER.trace("Permissions found. Continuing with DELETE operation.");
                ControllerUtils.checkEtag(request, resource);
                if (!DataResource.State.REVOKED.equals(resource.getState()) || AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.getValue()) || AuthenticationHelper.isPrincipal("SELF")) {
                    //call delete if resource not revoked (to revoke it) or if it is revoked and role is administrator or caller is repository itself (to set state to GONE)
                    dataResourceService.delete(resource);
                }
            } else {
                throw new UpdateForbiddenException("Insufficient permissions. ADMINISTRATE permission or ROLE_ADMINISTRATOR required.");
            }
        } catch (ResourceNotFoundException ex) {
            //ignored
            LOGGER.info("Resource with identifier {} not found. Returning with HTTP NO_CONTENT.", identifier);
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity createContent(@PathVariable(value = "id") final String identifier,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestPart(name = "metadata", required = false) final ContentInformation contentInformation,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Create content request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();
        String path = getContentPathFromRequest(request);
        //@TODO escape path properly
        if (path == null || path.length() == 0 || path.endsWith("/")) {
            throw new BadArgumentException("Provided path is invalid. Path must not be empty and must not end with a slash.");
        }
        //check data resource and permissions
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).createContent(t, file, contentInformation, force, request, response, uriBuilder)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

        URI link = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(resource.getId(), null, applicationProperties.isAuditEnabled() ? 1l : null, null, request, response, uriBuilder)).toUri();

        try {
            ContentInformation result = contentInformationService.create(contentInformation, resource, path, (file != null) ? file.getInputStream() : null, force);

            URIBuilder builder = new URIBuilder(link);
            builder.setPath(builder.getPath().replace("**", path));
            URI resourceUri = null;

            try {
                resourceUri = builder.build();
            } catch (URISyntaxException ex) {
                LOGGER.error("Failed to create location URI for path " + path + ". However, resource should be created.", ex);
                throw new CustomInternalServerError("Resource creation successful, but unable to create resource linkfor path " + path + ".");
            }

            long currentVersion = contentAuditService.getCurrentVersion(Long.toString(result.getId()));
            if (currentVersion > 0) {
                return ResponseEntity.created(resourceUri).header("Resource-Version", Long.toString(currentVersion)).build();
            } else {
                return ResponseEntity.created(resourceUri).build();
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to open file input stream.", ex);
            throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
        }
    }

    @Override
    public ResponseEntity getContentMetadata(@PathVariable(value = "id") final String identifier,
            @RequestParam(name = "tag", required = false) final String tag,
            @RequestParam(name = "version", required = false) final Long version,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        String path = getContentPathFromRequest(request);
        //check resource and permission
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, tag, version, pgbl, request, response, uriBuilder)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

        LOGGER.trace("Checking provided path {}.", path);
        if (path.startsWith("/")) {
            LOGGER.debug("Removing leading slash from path {}.", path);
            //remove leading slash if present, e.g. if path was empty
            path = path.substring(1);
        }

        //switch between collection and element listing
        if (path.endsWith("/") || path.length() == 0) {
            LOGGER.trace("Path ends with slash or is empty. Performing collection access.");
            //collection listing
            path += "%";
            //sanitize page request

            PageRequest pageRequest = ControllerUtils.checkPaginationInformation(pgbl, pgbl.getSort().equals(Sort.unsorted()) ? Sort.by(Sort.Order.asc("depth"), Sort.Order.asc("relativePath")) : pgbl.getSort());

            LOGGER.trace("Obtaining content information page for parent resource {}, path {} and tag {}. Page information are: {}", resource.getId(), path, tag, pageRequest);
            Page<ContentInformation> resultList = contentInformationService.findAll(ContentInformation.createContentInformation(resource.getId(), path, tag), pageRequest);

            LOGGER.trace("Obtained {} content information result(s).", resultList.getContent().size());
            List<ContentInformation> result = filterContentInformation(resultList.getContent());
            return ResponseEntity.ok().body(result);
        } else {
            LOGGER.trace("Path does not end with slash and/or is not empty. Assuming single element access.");
            ContentInformation contentInformation = contentInformationService.getContentInformation(resource.getId(), path, version);

            long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));

            LOGGER.trace("Obtained single content information result.");
            if (currentVersion > 0) {
                return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").header("Resource-Version", Long.toString(currentVersion)).body(filterContentInformation(contentInformation));
            } else {
                return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").body(filterContentInformation(contentInformation));
            }
        }
    }

    @Override
    public ResponseEntity<List<ContentInformation>> findContentMetadataByExample(@RequestBody final ContentInformation example,
            final Pageable pgbl,
            final WebRequest wr,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);

        Page<ContentInformation> page = contentInformationService.findByExample(example, AuthenticationHelper.getAuthorizationIdentities(),
                AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString()), pgbl);

        response.addHeader("Content-Range", ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
        return ResponseEntity.ok().body(filterContentInformation(page.getContent()));
    }

    @Override
    public ResponseEntity patchContentMetadata(@PathVariable(value = "id") final String identifier,
            final @RequestBody JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Patch content metadata request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();

        String path = getContentPathFromRequest(request);

        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patchContentMetadata(t, patch, request, response)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

        ControllerUtils.checkEtag(request, resource);

        ContentInformation toUpdate = contentInformationService.getContentInformation(resource.getId(), path, null);

        contentInformationService.patch(toUpdate, patch, getUserAuthorities(resource));
        long currentVersion = contentAuditService.getCurrentVersion(Long.toString(toUpdate.getId()));

        if (currentVersion > 0) {
            return ResponseEntity.noContent().header("Resource-Version", Long.toString(currentVersion)).build();
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @Override
    public void getContent(@PathVariable(value = "id") final String identifier,
            @RequestParam(value = "version", required = false) Long version,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        String path = getContentPathFromRequest(request);
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, null, applicationProperties.isAuditEnabled() ? 1l : null, null, request, response, uriBuilder)).toString();
        });
        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);
        LOGGER.debug("Access to resource with identifier {} granted. Continue with content access.", resource.getId());
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        contentInformationService.read(resource, path, version, acceptHeader, response);
    }

    @Override
    public ResponseEntity deleteContent(@PathVariable(value = "id")
            final String identifier,
            final WebRequest request,
            final HttpServletResponse response) {
        if (applicationProperties.isReadOnly()) {
            LOGGER.info("Repository is in read-only mode. Delete content request denied.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        ControllerUtils.checkAnonymousAccess();

        String path = getContentPathFromRequest(request);

        //check resource and permission
        DataResource resource = getResourceByIdentifierOrRedirect(identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).deleteContent(t, request, response)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.ADMINISTRATE);

        ControllerUtils.checkEtag(request, resource);

        //try to obtain single content element matching path exactly
        Page<ContentInformation> contentInfoOptional = contentInformationService.findAll(ContentInformation.createContentInformation(resource.getId(), path), PageRequest.of(0, 1));
        if (contentInfoOptional.hasContent()) {
            LOGGER.debug("Content information entry found. Checking ETag.");

            ContentInformation contentInfo = contentInfoOptional.getContent().get(0);

            Path localContentToRemove = null;
            URI contentUri = URI.create(contentInfo.getContentUri());
            LOGGER.trace("Checking if content URI {} is pointing to a local file.", contentInfo);
            if ("file".equals(contentUri.getScheme())) {
                //mark file for removal
                localContentToRemove = Paths.get(URI.create(contentInfo.getContentUri()));
            } else {
                //content URI is not pointing to a file...just replace the entry
                LOGGER.trace("Content to delete is pointing to {}. Local content deletion will be skipped.", contentInfo.getContentUri());
            }
            contentInformationService.delete(contentInfo);

            if (localContentToRemove != null) {
                try {
                    LOGGER.trace("Removing content file {}.", localContentToRemove);
                    Files.deleteIfExists(localContentToRemove);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to remove data at " + localContentToRemove + ". Manual removal required.", ex);
                }
            } else {
                LOGGER.trace("No local content file exists. Returning from DELETE.");
            }
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity getAuditInformation(@PathVariable("id") final String resourceIdentifier,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder ucb) {
        LOGGER.trace("Performing getAuditInformation({}, {}).", resourceIdentifier, pgbl);
        DataResource resource = getResourceByIdentifierOrRedirect(resourceIdentifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, null, request, response)).toString();
        });
        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

        Optional<String> auditInformation = dataResourceService.getAuditInformationAsJson(resourceIdentifier, pgbl);

        if (!auditInformation.isPresent()) {
            LOGGER.trace("No audit information found for resource {}. Returning empty JSON array.", resourceIdentifier);
            return ResponseEntity.ok().body("[]");
        }

        long currentVersion = auditService.getCurrentVersion(resourceIdentifier);

        LOGGER.trace("Audit information found, returning result.");
        return ResponseEntity.ok().header("Resource-Version", Long.toString(currentVersion)).body(auditInformation.get());
    }

    @Override
    public ResponseEntity getContentAuditInformation(@PathVariable("id") final String resourceIdentifier,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Performing getContentAuditInformation({}, {}).", resourceIdentifier, pgbl);

        String path = getContentPathFromRequest(request);
        //check resource and permission
        DataResource resource = getResourceByIdentifierOrRedirect(resourceIdentifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, null, null, pgbl, request, response, uriBuilder)).toString();
        });

        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

        LOGGER.trace("Checking provided path {}.", path);
        if (path.startsWith("/")) {
            LOGGER.debug("Removing leading slash from path {}.", path);
            //remove leading slash if present, which should actually never happen
            path = path.substring(1);
        }

        //switch between collection and element listing
        if (path.endsWith("/") || path.length() == 0) {
            LOGGER.error("Path ends with slash or is empty. Obtaining audit information for collection elements is not supported.");
            throw new BadArgumentException("Provided path is invalid for obtaining audit information. Path must not be empty and must not end with a slash.");
        } else {
            LOGGER.trace("Path does not end with slash and/or is not empty. Assuming single element access.");
            ContentInformation contentInformation = contentInformationService.getContentInformation(resource.getId(), path, null);

            Optional<String> auditInformation = contentInformationService.getAuditInformationAsJson(Long.toString(contentInformation.getId()), pgbl);

            if (!auditInformation.isPresent()) {
                LOGGER.trace("No audit information found for resource {} and path {}. Returning empty JSON array.", resourceIdentifier, path);
                return ResponseEntity.ok().body("[]");
            }

            long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));

            LOGGER.trace("Audit information found, returning result.");
            return ResponseEntity.ok().header("Resource-Version", Long.toString(currentVersion)).body(auditInformation.get());
        }
    }

    /**
     * Helper methods for internal use.*
     */
    private DataResource getResourceByIdentifierOrRedirect(String identifier,
            Long version,
            Function<String, String> supplier) {
        String decodedIdentifier;
        try {
            LOGGER.trace("Performing getResourceByIdentifierOrRedirect({}, {}, #Function).", identifier, version);
            decodedIdentifier = URLDecoder.decode(identifier, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Failed to decode resource identifier " + identifier + ".", ex);
            throw new CustomInternalServerError("Failed to decode provided identifier " + identifier + ".");
        }
        LOGGER.trace("Decoded resource identifier: {}", decodedIdentifier);
        DataResource resource = dataResourceService.findByAnyIdentifier(decodedIdentifier, version);
        //check if resource was found by resource identifier 
        if (Objects.equal(decodedIdentifier, resource.getId())) {
            //resource was found by resource identifier...return and proceed
            LOGGER.trace("Resource for identifier {} found. Returning resource #{}.", decodedIdentifier, resource.getId());
            return resource;
        }
        //resource was found by another identifier...redirect
        String encodedIdentifier;
        try {
            encodedIdentifier = URLEncoder.encode(resource.getId(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Failed to encode resource identifier " + resource.getId() + ".", ex);
            throw new CustomInternalServerError("Failed to encode resource identifier " + resource.getId() + ".");
        }
        LOGGER.trace("No resource for identifier {} found. Redirecting to resource with identifier {}.", identifier, encodedIdentifier);
        throw new ResourceElsewhereException(supplier.apply(encodedIdentifier));
    }

    private String getContentPathFromRequest(WebRequest request) {
        String requestedUri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
        if (requestedUri == null) {
            throw new CustomInternalServerError("Unable to obtain request URI.");
        }
        return requestedUri.substring(requestedUri.indexOf("data/") + "data/".length());
    }

    private Collection<? extends GrantedAuthority> getUserAuthorities(DataResource resource) {
        LOGGER.trace("Determining user grants from authorization context.");
        Collection<GrantedAuthority> userGrants = new ArrayList<>();
        userGrants.add(new SimpleGrantedAuthority(DataResourceUtils.getAccessPermission(resource).getValue()));

        if (AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())) {
            LOGGER.trace("Administrator access detected. Adding role {} to granted authorities.", RepoUserRole.ADMINISTRATOR.getValue());
            userGrants.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.getValue()));
        }

        return userGrants;
    }

    private DataResource filterResource(DataResource resource) {
        if (!AuthenticationHelper.isAuthenticatedAsService() && !DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())) {
            LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
            //exclude ACLs if not administrate or administrator permissions are set
            resource.setAcls(null);
        } else {
            LOGGER.debug("Administrator access detected, keeping ACL information in resources.");
        }

        return resource;
    }

    private List<DataResource> filterResources(List<DataResource> resources) {

        if (!AuthenticationHelper.isAuthenticatedAsService() && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())) {
            LOGGER.debug("Removing ACL information from resources due to non-administrator access.");
            //exclude ACLs if not administrate or administrator permissions are set
            resources.forEach((resource) -> {
                resource.setAcls(null);
            });
        } else {
            LOGGER.debug("Administrator access detected, keeping ACL information in resources.");
        }

        return resources;
    }

    private ContentInformation filterContentInformation(ContentInformation resource) {
        //hide all attributes but the id from the parent data resource in the content information entity

        String id = resource.getParentResource().getId();
        resource.setParentResource(DataResource.factoryNewDataResource(id));
        return resource;
    }

    private List<ContentInformation> filterContentInformation(List<ContentInformation> resources) {
        //hide all attributes but the id from the parent data resource in all content information entities
        resources.forEach((resource) -> {
            String id = resource.getParentResource().getId();
            resource.setParentResource(DataResource.factoryNewDataResource(id));
        });
        return resources;
    }
}
