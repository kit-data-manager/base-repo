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

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.repo.web.IDataResourceAuditController;
import static edu.kit.datamanager.repo.web.impl.DataResourceController.VERSION_HEADER;
import edu.kit.datamanager.service.IAuditService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.function.Function;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for audit endpoints of data resource service.
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/audit")
@Schema(description = "Data Resource Audit Query")
public class DataResourceAuditController implements IDataResourceAuditController {

    private Logger LOGGER = LoggerFactory.getLogger(DataResourceController.class);

    private final IContentInformationService contentInformationService;
    @Autowired
    private ApplicationProperties applicationProperties;

    private final IAuditService<DataResource> auditService;
    private final IAuditService<ContentInformation> contentAuditService;
    private final RepoBaseConfiguration repositoryProperties;

    /**
     * Default constructor.
     *
     * @param applicationProperties The application properties.
     * @param repositoryConfig The repository config.
     */
    public DataResourceAuditController(ApplicationProperties applicationProperties,
            RepoBaseConfiguration repositoryConfig
    ) {
        this.applicationProperties = applicationProperties;
        this.contentInformationService = repositoryConfig.getContentInformationService();
        auditService = repositoryConfig.getAuditService();
        contentAuditService = repositoryConfig.getContentInformationAuditService();
        repositoryProperties = repositoryConfig;
        LOGGER.trace("Show Config: {}", repositoryConfig);
    }

    @Override
    public ResponseEntity getContentAuditInformation(@PathVariable("id") final String resourceIdentifier,
            @Parameter(required = false) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Performing getContentAuditInformation({}, {}).", resourceIdentifier, null);
        Function<String, String> getById;
        getById = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(DataResourceController.class).getContentMetadata(t, null, null, pgbl, request, response, uriBuilder)).toString();
        };
        String path = ContentDataUtils.getContentPathFromRequest(request);
        //check resource and permission
        DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, resourceIdentifier, null, getById);

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
        }
        LOGGER.trace("Path does not end with slash and/or is not empty. Assuming single element access.");
        ContentInformation contentInformation = contentInformationService.getContentInformation(resource.getId(), path, null);

        Optional<String> auditInformation = contentInformationService.getAuditInformationAsJson(Long.toString(contentInformation.getId()), pgbl);

        if (!auditInformation.isPresent()) {
            LOGGER.trace("No audit information found for resource {} and path {}. Returning empty JSON array.", resourceIdentifier, path);
            return ResponseEntity.ok().body("[]");
        }

        LOGGER.trace("Audit information found, returning result.");
        long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));

        return ResponseEntity.ok().header(VERSION_HEADER, Long.toString(currentVersion)).body(auditInformation.get());
    }
 
    @Override
    public ResponseEntity getAuditInformation(
            @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String resourceIdentifier,
            @Parameter(required = false) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder) {
        LOGGER.trace("Performing getAuditInformation({}, {}).", resourceIdentifier, pgbl);
        Function<String, String> getById;
        getById = (t) -> {
            return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(DataResourceController.class).getById(t, null, request, response)).toString();
        };
        Optional<String> auditInformation = DataResourceUtils.getAuditInformation(repositoryProperties, resourceIdentifier, pgbl, getById);

        if (!auditInformation.isPresent()) {
            LOGGER.trace("No audit information found for resource {}. Returning empty JSON array.", resourceIdentifier);
            return ResponseEntity.ok().body("[]");
        }

        long currentVersion = auditService.getCurrentVersion(resourceIdentifier);

        LOGGER.trace(
                "Audit information found, returning result.");
        return ResponseEntity.ok()
                .header(VERSION_HEADER, Long.toString(currentVersion)).body(auditInformation.get());
    }

}
