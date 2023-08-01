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
package edu.kit.datamanager.repo.web.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.TabulatorLocalPagination;
import edu.kit.datamanager.repo.elastic.DataResourceRepository;
import edu.kit.datamanager.repo.elastic.ElasticWrapper;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.repo.util.EntityUtils;
import edu.kit.datamanager.repo.web.IDataResourceController;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for data resource endpoints.
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/dataresources")
@Schema(description = "Data Resource Management")
public class DataResourceController implements IDataResourceController {

    public static final String VERSION_HEADER = "Resource-Version";
    public static final String CONTENT_RANGE_HEADER = "Content-Range";
    // private final JsonResult json = JsonResult.instance();
    private final Logger LOGGER = LoggerFactory.getLogger(DataResourceController.class);

    private final IContentInformationService contentInformationService;

    @Autowired
    private final ApplicationProperties applicationProperties;

    @Autowired
    private IDataResourceDao dataResourceDao;
    @Autowired
    private IContentInformationDao contentInformationDao;

    private final IAuditService<DataResource> auditService;
    private final IAuditService<ContentInformation> contentAuditService;
    private final RepoBaseConfiguration repositoryProperties;
    @Autowired
    private Optional<DataResourceRepository> dataResourceRepository;

    /**
     * Default constructor.
     *
     * @param applicationProperties Properties object.
     * @param repositoryConfig Generic configuratation object.
     */
    public DataResourceController(ApplicationProperties applicationProperties,
            RepoBaseConfiguration repositoryConfig) {
        this.applicationProperties = applicationProperties;
        this.contentInformationService = repositoryConfig.getContentInformationService();
        auditService = repositoryConfig.getAuditService();
        contentAuditService = repositoryConfig.getContentInformationAuditService();
        repositoryProperties = repositoryConfig;
        LOGGER.trace("Show Config: {}", repositoryConfig);
    }

    @Override
    public ResponseEntity<DataResource> create(@RequestBody final DataResource resource,
            final WebRequest request,
            final HttpServletResponse response) {

        LOGGER.trace("Creating resource with record '{}'.", resource);
        Function<String, String> getById;
        getById = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, 1l, request, response)).toString();
        };

        LOGGER.trace("Removing user-provided @Ids from resource.");
        EntityUtils.removeIds(resource);

        DataResource result = DataResourceUtils.createResource(repositoryProperties, resource);
        try {
            LOGGER.trace("Creating controller link for resource identifier {}.", result.getId());
            //do some hacking in order to properly escape the resource identifier
            //if escaping in beforehand, WebMvcLinkBuilder will escape again, which invalidated the link
            String uriLink = getById.apply("WorkaroundPlaceholder");
            //replace placeholder with escaped identifier in order to ensure single-escaping
            uriLink = uriLink.replaceFirst("WorkaroundPlaceholder", URLEncoder.encode(result.getId(), "UTF-8"));
            // remove version flag if version is nor supported
            if (!applicationProperties.isAuditEnabled()) {
                // Remove path parameter version
                int qmIndex = uriLink.lastIndexOf("?");
                if (qmIndex > 0) {
                    uriLink = uriLink.substring(0, qmIndex);
                }
            }

            indexResource(resource.getId(), false);

            LOGGER.trace("Created resource link is: {}", uriLink);
            return ResponseEntity.created(URI.create(uriLink)).eTag("\"" + result.getEtag() + "\"").header(VERSION_HEADER, Long.toString(1l)).body(result);
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
        LOGGER.trace("Get resource by id '{}' and version '{}'.", identifier, version);
        Function<String, String> getById = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, version, request, response)).toString();
        };
        return DataResourceUtils.readResource(repositoryProperties, identifier, version, getById);
    }

    @Override
    public ResponseEntity<DataResource> getByPid(@PathVariable("prefix") final String prefix,
            @PathVariable("suffix") final String suffix,
            @RequestParam(name = "version", required = false) final Long version,
            final WebRequest request,
            final HttpServletResponse response) {
        return getById(prefix + "/" + suffix, version, request, response);
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
    public ResponseEntity<TabulatorLocalPagination> findAllForTabulator(@RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
            @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        //change from 1-based to 0-based index as required by tabulator
        int startPage = pgbl.getPageNumber() - 1;
        startPage = startPage < 0 ? 0 : startPage;
        PageRequest pr = PageRequest.of(startPage, pgbl.getPageSize(), pgbl.getSort());
        Page<DataResource> page = DataResourceUtils.readAllResourcesFilteredByExample(repositoryProperties, null, lastUpdateFrom, lastUpdateUntil, pr, response, uriBuilder);
        PageRequest pageRequest = ControllerUtils.checkPaginationInformation(pgbl);
        response.addHeader(CONTENT_RANGE_HEADER, ControllerUtils.getContentRangeHeader(page.getNumber(), pageRequest.getPageSize(), page.getTotalElements()));
        TabulatorLocalPagination tabulatorLocalPagination = TabulatorLocalPagination.builder()
                .lastPage(page.getTotalPages())
                .data(DataResourceUtils.filterResources(page.getContent()))
                .build();
        return ResponseEntity.ok().body(tabulatorLocalPagination);
    }

    @Override
    public ResponseEntity<List<DataResource>> findByExample(@RequestBody DataResource example,
            @RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
            @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
            final Pageable pgbl,
            final WebRequest req,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Find resource by example '{}' from '{}' until '{}'", example, lastUpdateFrom, lastUpdateUntil);
        Page<DataResource> page = DataResourceUtils.readAllResourcesFilteredByExample(repositoryProperties, example, lastUpdateFrom, lastUpdateUntil, pgbl, response, uriBuilder);

        //set content-range header for react-admin (index_start-index_end/total
        PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);
        response.addHeader(CONTENT_RANGE_HEADER, ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
        return ResponseEntity.ok().body(DataResourceUtils.filterResources(page.getContent()));
    }

    @Override
    public ResponseEntity patch(@PathVariable("id") final String identifier,
            @RequestBody final JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        LOGGER.trace("Patch resource with id '{}': Patch '{}'", identifier, patch);
        Function<String, String> patchDataResource = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patch(t, patch, request, response)).toString();
        };
        //String path = ContentDataUtils.getContentPathFromRequest(request);
        String eTag = ControllerUtils.getEtagFromHeader(request);
        DataResourceUtils.patchResource(repositoryProperties, identifier, patch, eTag, patchDataResource);

        indexResource(identifier, true);

        long currentVersion = auditService.getCurrentVersion(identifier);
        if (currentVersion > 0) {
            return ResponseEntity.noContent().header(VERSION_HEADER, Long.toString(currentVersion)).build();
        } else {
            return ResponseEntity.noContent().build();

        }
    }

    @Override
    public ResponseEntity patchPid(@PathVariable("prefix") final String prefix,
            @PathVariable("suffix") final String suffix,
            @RequestBody final JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        return patch(prefix + "/" + suffix, patch, request, response);
    }

    @Override
    public ResponseEntity put(@PathVariable("id") final String identifier,
            @RequestBody final DataResource newResource,
            final WebRequest request,
            final HttpServletResponse response) {
        LOGGER.trace("Update resource with id '{}': new resource: '{}'", identifier, newResource);
        Function<String, String> putWithId;
        putWithId = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).put(t, newResource, request, response)).toString();
        };
        DataResource result = DataResourceUtils.updateResource(repositoryProperties, identifier, newResource, request, putWithId);

        indexResource(identifier, true);

        long currentVersion = repositoryProperties.getAuditService().getCurrentVersion(result.getId());

        if (currentVersion > 0) {
            //trigger response creation and set etag...the response body is set automatically
            return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").header(VERSION_HEADER, Long.toString(currentVersion)).body(DataResourceUtils.filterResource(result));
        } else {
            return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").body(DataResourceUtils.filterResource(result));
        }
    }

    @Override
    public ResponseEntity putPid(@PathVariable("prefix") final String prefix,
            @PathVariable("suffix") final String suffix,
            @RequestBody final DataResource newResource,
            final WebRequest request,
            final HttpServletResponse response) {
        return put(prefix + "/" + suffix, newResource, request, response);
    }

    @Override
    public ResponseEntity delete(@PathVariable("id") final String identifier,
            final WebRequest request,
            final HttpServletResponse response) {
        LOGGER.trace("Delete resource with id '{}'", identifier);
        Function<String, String> getById = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, 1l, request, response)).toString();
        };
        DataResourceUtils.deleteResource(repositoryProperties, identifier, request, getById);

        unindexResource(identifier);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity deletePid(@PathVariable("prefix") final String prefix,
            @PathVariable("suffix") final String suffix,
            final WebRequest request,
            final HttpServletResponse response) {
        return delete(prefix + "/" + suffix, request, response);
    }

    @Override
    public ResponseEntity createContent(@PathVariable(value = "id") final String identifier,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestPart(name = "metadata", required = false) final MultipartFile contentInformation,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Create content for resource with id '{}'. Force: '{}'", identifier, force);
        Function<String, String> createContent = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).createContent(t, file, contentInformation, force, request, response, uriBuilder)).toString();
        };
        DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, createContent);
        String path = ContentDataUtils.getContentPathFromRequest(request);

        ContentInformation info = null;
        if (contentInformation != null) {
            LOGGER.trace("Reading user-provided content information.");
            try {
                info = new ObjectMapper().readValue(contentInformation.getInputStream(), ContentInformation.class);
                LOGGER.trace("Removing user-provided @Ids from content information.");
                EntityUtils.removeIds(info);
            } catch (IOException ex) {
                LOGGER.error("Unable to read content information metadata.", ex);
                return ResponseEntity.badRequest().body("Invalid ContentInformation metadata provided.");
            }
        }
        ContentInformation result = ContentDataUtils.addFile(repositoryProperties, resource, file, path, info, force, createContent);

        URI link = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(resource.getId(), null, 1l, null, request, response, uriBuilder)).toUri();

        URIBuilder builder = new URIBuilder(link);
        builder.setPath(builder.getPath().replace("**", path));
        URI resourceUri = null;

        try {
            resourceUri = builder.build();
        } catch (URISyntaxException ex) {
            LOGGER.error("Failed to create location URI for path " + path + ". However, resource should be created.", ex);
            throw new CustomInternalServerError("Resource creation successful, but unable to create resource linkfor path " + path + ".");
        }

        indexResource(identifier, true);

        long currentVersion = contentAuditService.getCurrentVersion(Long.toString(result.getId()));
        if (currentVersion > 0) {
            return ResponseEntity.created(resourceUri).header(VERSION_HEADER, Long.toString(currentVersion)).eTag("\"" + result.getEtag() + "\"").build();
        } else {
            return ResponseEntity.created(resourceUri).eTag("\"" + result.getEtag() + "\"").build();
        }
    }

    @Override
    public ResponseEntity createContentPid(@PathVariable(value = "prefix") final String prefix,
            @PathVariable(value = "suffix") final String suffix,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestPart(name = "metadata", required = false) final MultipartFile contentInformation,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        return createContent(prefix + "/" + suffix, file, contentInformation, force, request, response, uriBuilder);
    }

    @Override
    public ResponseEntity getContentMetadata(@PathVariable(value = "id") final String identifier,
            @RequestParam(name = "tag", required = false) final String tag,
            @RequestParam(name = "version", required = false) final Long version,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Get content metadata for resource with id '{}' and version '{}'", identifier, version);

        Function<String, String> getContentMetadata = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, tag, version, pgbl, request, response, uriBuilder)).toString();
        };
        //check resource and permission
        DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, getContentMetadata);
        String path = ContentDataUtils.getContentPathFromRequest(request);

        List<ContentInformation> result = ContentDataUtils.readFiles(repositoryProperties, resource, path, tag, version, pgbl, getContentMetadata);

        if (path.endsWith("/") || path.length() == 0) {
            LOGGER.trace("Obtained {} content information result(s).", result.size());
            return ResponseEntity.ok().body(fixContentInformation(result, version));
        } else {
            LOGGER.trace("Obtained single content information result.");
            ContentInformation contentInformation = result.get(0);

            long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));
            if (currentVersion > 0) {
                return ResponseEntity.ok().eTag("\"" + contentInformation.getEtag() + "\"").header(VERSION_HEADER, Long.toString(currentVersion)).body(fixContentInformation(contentInformation, version));
            } else {
                return ResponseEntity.ok().eTag("\"" + contentInformation.getEtag() + "\"").body(fixContentInformation(contentInformation, version));
            }
        }

    }

    @Override
    public ResponseEntity getContentMetadataPid(@PathVariable(value = "prefix") final String prefix,
            @PathVariable(value = "suffix") final String suffix,
            @RequestParam(name = "tag", required = false) final String tag,
            @RequestParam(name = "version", required = false) final Long version,
            final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        return getContentMetadata(prefix + "/" + suffix, tag, version, pgbl, request, response, uriBuilder);
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

        response.addHeader(CONTENT_RANGE_HEADER, ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
        return ResponseEntity.ok().body(fixContentInformation(page.getContent(), null));
    }

    @Override
    public ResponseEntity patchContentMetadata(@PathVariable(value = "id") final String identifier,
            final @RequestBody JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        Function<String, String> patchContentMetadata = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patchContentMetadata(t, patch, request, response)).toString();
        };
        String path = ContentDataUtils.getContentPathFromRequest(request);
        String eTag = ControllerUtils.getEtagFromHeader(request);
        ContentInformation toUpdate = ContentDataUtils.patchContentInformation(repositoryProperties, identifier, path, patch, eTag, patchContentMetadata);

        indexResource(identifier, true);

        long currentVersion = contentAuditService.getCurrentVersion(Long.toString(toUpdate.getId()));
        if (currentVersion > 0) {
            return ResponseEntity.noContent().header(VERSION_HEADER, Long.toString(currentVersion)).build();
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @Override
    public ResponseEntity patchContentMetadataPid(@PathVariable(value = "prefix") final String prefix,
            @PathVariable(value = "suffix") final String suffix,
            final @RequestBody JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response) {
        return patchContentMetadata(prefix + "/" + suffix, patch, request, response);
    }

    @Override
    public void getContent(@PathVariable(value = "id") final String identifier,
            @RequestParam(value = "version", required = false) Long version,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Get content for resource with id '{}' and version '{}'", identifier, version);
        String path = ContentDataUtils.getContentPathFromRequest(request);
        LOGGER.trace("Path: '{}'", path);
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, null, 1l, null, request, response, uriBuilder)).toString();
        });
        DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);
        LOGGER.debug("Access to resource with identifier {} granted. Continue with content access.", resource.getId());
        contentInformationService.read(resource, path, version, acceptHeader, response);
    }

    @Override
    public void getContentPid(@PathVariable(value = "prefix") final String prefix,
            @PathVariable(value = "suffix") final String suffix,
            @RequestParam(value = "version", required = false) Long version,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        getContent(prefix + "/" + suffix, version, request, response, uriBuilder);
    }

    @Override
    public ResponseEntity deleteContent(@PathVariable(value = "id")
            final String identifier,
            final WebRequest request,
            final HttpServletResponse response) {
        String path = ContentDataUtils.getContentPathFromRequest(request);
        String eTag = ControllerUtils.getEtagFromHeader(request);
        Function<String, String> deleteContent = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).deleteContent(t, request, response)).toString();
        };
        ContentDataUtils.deleteFile(repositoryProperties, identifier, path, eTag, deleteContent);

        indexResource(identifier, true);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity deleteContentPid(@PathVariable(value = "prefix") final String prefix,
            @PathVariable(value = "suffix") final String suffix,
            final WebRequest request,
            final HttpServletResponse response) {
        return deleteContent(prefix + "/" + suffix, request, response);
    }

    private ContentInformation fixContentInformation(ContentInformation resource, Long version) {
        //hide all attributes but the id from the parent data resource in the content information entity
        String id = resource.getParentResource().getId();
        resource.setParentResource(DataResource.factoryNewDataResource(id));
        // fix content URI if URI points to a local file
        if (resource.getContentUri() != null && resource.getContentUri().startsWith("file:/")) {
            Long fileVersion = version != null ? version : 1l;
            String contentUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(id, null, fileVersion, null, null, null, null)).toString();
            contentUri = contentUri.replaceAll("\\*\\*", resource.getRelativePath());
            if ((version == null) || !applicationProperties.isAuditEnabled()) {
                // Remove path parameter version
                int qmIndex = contentUri.lastIndexOf("?");
                if (qmIndex > 0) {
                    contentUri = contentUri.substring(0, qmIndex);

                }
            }
            resource.setContentUri(contentUri);
        }
        return resource;
    }

    private List<ContentInformation> fixContentInformation(List<ContentInformation> resources, Long version) {
        //hide all attributes but the id from the parent data resource in all content information entities
        resources.forEach((resource) -> {
            fixContentInformation(resource, version);
        });
        return resources;
    }

    private void indexResource(
            String identifier,
            boolean includeContent) {
        if (dataResourceRepository.isPresent()) {
            LOGGER.trace("Indexing data resource {} {} content information.", identifier, (includeContent ? "with" : "without"));
            Optional<DataResource> resource = dataResourceDao.findById(identifier);
            ElasticWrapper wrapper;

            if (includeContent) {
                LOGGER.trace("Reading content information for resource {}.", identifier);
                Page<ContentInformation> page = contentInformationDao.findByParentResource(resource.get(), PageRequest.of(0, Integer.MAX_VALUE));
                List<ContentInformation> infoList = page.toList();
                LOGGER.trace("Obtained {} content information element(s). Shortening resource to reference.", infoList.size());
                infoList.forEach(info -> {
                    DataResource res = DataResource.factoryNewDataResource(info.getParentResource().getId());
                    info.setParentResource(res);
                });
                LOGGER.trace("Creating Elastic wrapper with data resource and content information.");
                wrapper = new ElasticWrapper(resource.get(), infoList);

            } else {
                LOGGER.trace("Creating Elastic wrapper with data resource.");
                wrapper = new ElasticWrapper(resource.get());
            }
            LOGGER.trace("Indexing Elastic wrapper.");
            dataResourceRepository.get().save(wrapper);
        } else {
            LOGGER.trace("No Elastic repository found. Skipping indexing of resource.");
        }
    }

    private void unindexResource(
            String id) {
        if (dataResourceRepository.isPresent()) {
            dataResourceRepository.get().deleteById(id);
        }
    }
}
