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

import edu.kit.datamanager.controller.IControllerAuditSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Audit controller interface.
 *
 * @author jejkal
 */
public interface IDataResourceAuditController extends IControllerAuditSupport {

    @Operation(operationId = "getContentAuditInformation",
            summary = "Access audit information for a single content information resource.",
            description = "List audit information for a content information resource in a paginated form. Sorting can be supported but is optional. If no sorting is supported it is recommended to return audit "
            + "information sorted by version number in descending order. This endpoint is addressed if the caller provides content type "
            + "'application/vnd.datamanager.audit+json' within the 'Accept' header. If no audit support is enabled or no audit information are available for a certain resource, "
            + "an empty result should be returned.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET, produces = "application/vnd.datamanager.audit+json")
    @ResponseBody
    public ResponseEntity getContentAuditInformation(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            @Parameter(required = false) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);
}
