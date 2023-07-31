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
package edu.kit.datamanager.repo.web;

import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.controller.IGenericResourceController;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.TabulatorLocalPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Data resource controller interface definition. In addition to the common
 * controller endpoints defined in IGenericResourceController, the data resource
 * controller also provides additional endpoints for content access. Access to
 * data resources and content is separated by /data/ within the endpoint URLs.
 *
 * @author jejkal
 */
public interface IDataResourceController extends IGenericResourceController<DataResource> {

    @Operation(operationId = "getResourceById",
            summary = "Get resource metadata by id.",
            description = "Obtain metadata of a single resource by one of its identifiers. This identifier can be either the internal identifier, "
            + "the primary identifier or one of the resource's alternate identifiers. The provided identifier must be properly URL-encoded. If enabled, "
            + "older versions of a resource can be accessed by providing the `version` query parameter. By default, the most recent version "
            + "is returned. Versions are numbered sequentially starting at 1 and are returned in the `Resource-Version` header field.<br/>"
            + "Furthermore, if enabled, authentication and authorization may restrict access to resources.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Override
    public ResponseEntity getById(@Parameter(description = "The resource identifier.", required = true) @PathVariable("id") final String id,
            @Parameter(description = "The version of the resource.", required = false) @RequestParam("version") final Long version,
            final WebRequest request,
            final HttpServletResponse response);

    @Operation(operationId = "getResourceByPid",
            summary = "Get resource metadata by pid.",
            description = "Obtain metadata of a single resource by one of its persistent identifier. This endpoint is used to access resources "
            + "with a PID (prefix/suffix) as identifier. If enabled, "
            + "older versions of a resource can be accessed by providing the `version` query parameter. By default, the most recent version "
            + "is returned. Versions are numbered sequentially starting at 1 and are returned in the `Resource-Version` header field.<br/>"
            + "Furthermore, if enabled, authentication and authorization may restrict access to resources.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/{prefix}/{suffix}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity getByPid(@Parameter(description = "The PID prefix.", required = true) @PathVariable("prefix") final String prefix,
            @Parameter(description = "The PID suffix.", required = true) @PathVariable("prefix") final String suffix,
            @Parameter(description = "The version of the resource.", required = false) @RequestParam("version") final Long version,
            final WebRequest request,
            final HttpServletResponse response);

    @Operation(operationId = "createResource",
            summary = "Create a new resource.",
            description = "Create a new resource with the given JSON representation."
            + " While many properties are optional, there are also some mandatory properties which have to be provided, i.e., at least one Title "
            + "and the ResourceType. Other mandatory elements, i.e., a Creator, publicationYear and publisher, are automatically assigned if not provided.<br/>"
            + "If enabled, authentication and authorization may restrict creation to existing users.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = {"/"}, method = {RequestMethod.POST}, consumes = {"application/json", "application/vnd.datacite.org+json", "application/vnd.zenodo.org+json"})
    @ResponseBody
    @Override
    public ResponseEntity<DataResource> create(@Parameter(description = "Json representation of the resource to create.", required = true)
            @RequestBody DataResource arg0, WebRequest arg1, HttpServletResponse arg2);

    @Operation(operationId = "listResourcesForTabulator",
            summary = "List all resources and return them in a format supported by the Tabulator.js library.",
            description = "List all resources in a paginated and/or sorted form. Possible queries are: listing with default values (X elements on first page sorted by database), "
            + "listing page wise, sorted query page wise, and combinations of the options above. "
            + "The total number of resources may differ between calls if single resources have access restrictions. "
            + "Furthermore, anonymous listing of resources may or may not be supported.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = {"application/tabulator+json"})
    @ResponseBody
    @PageableAsQueryParam
    public ResponseEntity<TabulatorLocalPagination> findAllForTabulator(
            @Parameter(description = "The UTC time of the earliest update of a returned resource.", example = "2017-05-10T10:41:00Z", required = false) @RequestParam(value = "The UTC time of the earliest update of a returned resource.", name = "from", required = false) final Instant lastUpdateFrom,
            @Parameter(description = "The UTC time of the latest update of a returned resource.", example = "2017-05-10T10:41:00Z", required = false) @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
            @Parameter(hidden = true) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);

     @Operation(operationId = "patchResourcePid",
            summary = "Patch a resource by pid.",
            description = "Patch a single or multiple fields of a resource. Patching information are provided in JSON Patch format using Content-Type 'application/json-patch+json'. "
            + "Patching a resource requires privileged access to the resource to patch or ADMIN permissions of the caller. "
            + "Depending on the resource, single fields might be protected and cannot be changed, e.g. the unique identifier. "
            + "If the patch tries to modify a protected field, HTTP BAD_REQUEST will be returned before persisting the result.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/{prefix}/{suffix}", method = RequestMethod.PATCH, consumes = "application/json-patch+json")
    @ResponseBody
    public ResponseEntity patchPid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable("prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable("suffix") final String suffix,
            @Parameter(description = "Json representation of a json patch document. The document must comply with RFC 6902 specified by the IETF.", required = true) @RequestBody JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response);

    @Operation(operationId = "updateResourcePid",
            summary = "Replace a resource.",
            description = "Replace a resource by a new resource provided by the user."
            + "Putting a resource requires privileged access to the resource to patch or ADMIN permissions of the caller. "
            + "Some resource fields might be protected and cannot be changed, e.g. the unique identifier. "
            + "If at least one protected field in the new resource does not match with the current value, HTTP BAD_REQUEST will be returned before persisting the result."
            + "Attention: Due to the availability of PATCH, PUT support is optional! If a resource won't provide PUT support, HTTP NOT_IMPLEMENTED is returned.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/{prefix}/{suffix}", method = RequestMethod.PUT, consumes = "application/json")
    @ResponseBody
    public ResponseEntity putPid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable("prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable("suffix") final String suffix,
            @Parameter(description = "Json representation of the new representation of the resource.", required = true) @RequestBody DataResource resource,
            final WebRequest request,
            final HttpServletResponse response);

    @Operation(operationId = "deleteResourcePid",
            summary = "Delete a resource by pid.",
            description = "Delete a single resource. Deleting a resource typically requires the caller to have ADMIN permissions. "
            + "In some cases, deleting a resource can also be available for the owner or other privileged users or can be forbidden. "
            + "For resources whose deletion may affect other resources or internal workflows, physical deletion might not be possible at all. "
            + "In those cases, the resource might be disabled/hidden but not removed from the database. This can then happen optionally at "
            + "a later point in time, either automatically or manually.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = "/{prefix}/{suffix}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity deletePid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable("prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable("suffix") final String suffix,
            final WebRequest request,
            final HttpServletResponse response);
    
    @Operation(operationId = "uploadContent",
            summary = "Assign content to a resource.",
            description = "This endpoint allows to upload or assign data and content metadata to the resource with the given id. "
            + "Uploaded data will be stored at the configured backend, typically the local hard disk. "
            + "Furthermore, it is possible to register data by reference by manually providing a content URI in the content metadata."
            + "<br/>"
            + "In any other case, providing content metadata is optional. Parts of the content metadata, e.g. content type or checksum, may be generated or "
            + "overwritten after a file upload if they not already exist or if "
            + "the configuration does not allow the user to provide particular content metadata entries, e.g. because a certain checksum digest is mandatory."
            + "<br/>"
            + "All uploaded data can be virtually structured by providing the relative path where they should be accessible within the request URL. "
            + "If a file at a given path already exists, HTTP CONFLICT will be returned unless overwriting is requested by setting the query parameter 'force' to true. "
            + "In that case, the existing file will be marked for deletion and is deleted after successful upload. "
            + "If the overwritten element only contains a reference URI, the entry is directly replaced by the user provided entry.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseBody
    public ResponseEntity createContent(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            @Parameter(description = "The file to upload. If no file is provided, a metadata document containing a reference URI to the externally hosted data is mandatory.", required = false) @RequestPart(name = "file", required = false) final MultipartFile file,
            @Parameter(description = "Json representation of a content information metadata document. Providing this metadata document is optional unless no file is uploaded.", required = false) @RequestPart(name = "metadata", required = false) final MultipartFile contentInformation,
            @Parameter(description = "Flag to indicate, that existing content at the same location should be overwritten.", required = false) @RequestParam(name = "force", defaultValue = "false") final boolean force,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);

     @Operation(operationId = "uploadContentPid",
            summary = "Assign content to a resource.",
            description = "This endpoint allows to upload or assign data and content metadata to the resource with the given id. "
            + "Uploaded data will be stored at the configured backend, typically the local hard disk. "
            + "Furthermore, it is possible to register data by reference by manually providing a content URI in the content metadata."
            + "<br/>"
            + "In any other case, providing content metadata is optional. Parts of the content metadata, e.g. content type or checksum, may be generated or "
            + "overwritten after a file upload if they not already exist or if "
            + "the configuration does not allow the user to provide particular content metadata entries, e.g. because a certain checksum digest is mandatory."
            + "<br/>"
            + "All uploaded data can be virtually structured by providing the relative path where they should be accessible within the request URL. "
            + "If a file at a given path already exists, HTTP CONFLICT will be returned unless overwriting is requested by setting the query parameter 'force' to true. "
            + "In that case, the existing file will be marked for deletion and is deleted after successful upload. "
            + "If the overwritten element only contains a reference URI, the entry is directly replaced by the user provided entry.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{prefix}/{suffix}/data/**", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseBody
    public ResponseEntity createContentPid(@Parameter(description = "The pid prefix.", required = true) @PathVariable(value = "prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable(value = "suffix") final String suffix,
            @Parameter(description = "The file to upload. If no file is provided, a metadata document containing a reference URI to the externally hosted data is mandatory.", required = false) @RequestPart(name = "file", required = false) final MultipartFile file,
            @Parameter(description = "Json representation of a content information metadata document. Providing this metadata document is optional unless no file is uploaded.", required = false) @RequestPart(name = "metadata", required = false) final MultipartFile contentInformation,
            @Parameter(description = "Flag to indicate, that existing content at the same location should be overwritten.", required = false) @RequestParam(name = "force", defaultValue = "false") final boolean force,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);
    
    @Operation(operationId = "getContentMetadata",
            summary = "Access content or content metadata.",
            description = "Obtain content metadata or download content, depending on the provided `Accept` header."
            + "By providing 'application/vnd.datamanager.content-information+json', only content metadata is returned, optionally in a sorted and paginated way."
            + "If no `Accept` header is present, the content at the adressed path is downloaded instead."
            + "<br/>"
            + "The content path, defining whether one or more content (metadata) element(s) is/are returned, is provided within the request URL. "
            + "Everything after 'data/' is expected to be either a virtual folder or single content element. "
            + "If the provided content path ends with a slash, it is expected to represent a virtual collection which should be listed or downloaded. "
            + "If the content path does not end with a slash, it is expected to refer to a single content element. "
            + "If no content exists at the provided path, HTTP NOT_FOUND is returned. "
            + "<br/>"
            + "While accessing content metadata, the user may provide custom sorting criteria for ordering the returned elements. If no sorting criteria is provided, "
            + "the default sorting is applied returning all matching elements in ascending order by hierarchy depth and alphabetically by their relative path."
            + "<br/>"
            + "If configured, this endpoint also supports versioning for both, data and metadata. This only applied while accessing single elements. "
            + "By default, the most recent version of a content or content metadata element are returned. To obtain a previous version, the `version` query "
            + "parameter can be provided. Versions are numbered sequentially starting at 1 and are returned in the `Resource-Version` header field.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET, produces = "application/vnd.datamanager.content-information+json")
    @ResponseBody
    @PageableAsQueryParam
    public ResponseEntity getContentMetadata(
            @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            @Parameter(description = "A single tag assigned to certain content elements.", required = false) @RequestParam(name = "tag", required = false) final String tag,
            @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
            @Parameter(hidden = true) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);

    @Operation(operationId = "getContentMetadata",
            summary = "Access content or content metadata.",
            description = "Obtain content metadata or download content, depending on the provided `Accept` header."
            + "By providing 'application/vnd.datamanager.content-information+json', only content metadata is returned, optionally in a sorted and paginated way."
            + "If no `Accept` header is present, the content at the adressed path is downloaded instead."
            + "<br/>"
            + "The content path, defining whether one or more content (metadata) element(s) is/are returned, is provided within the request URL. "
            + "Everything after 'data/' is expected to be either a virtual folder or single content element. "
            + "If the provided content path ends with a slash, it is expected to represent a virtual collection which should be listed or downloaded. "
            + "If the content path does not end with a slash, it is expected to refer to a single content element. "
            + "If no content exists at the provided path, HTTP NOT_FOUND is returned. "
            + "<br/>"
            + "While accessing content metadata, the user may provide custom sorting criteria for ordering the returned elements. If no sorting criteria is provided, "
            + "the default sorting is applied returning all matching elements in ascending order by hierarchy depth and alphabetically by their relative path."
            + "<br/>"
            + "If configured, this endpoint also supports versioning for both, data and metadata. This only applied while accessing single elements. "
            + "By default, the most recent version of a content or content metadata element are returned. To obtain a previous version, the `version` query "
            + "parameter can be provided. Versions are numbered sequentially starting at 1 and are returned in the `Resource-Version` header field.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{prefix}/{suffix}/data/**", method = RequestMethod.GET, produces = "application/vnd.datamanager.content-information+json")
    @ResponseBody
    @PageableAsQueryParam
    public ResponseEntity getContentMetadataPid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable(value = "prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable(value = "suffix") final String suffix,
            @Parameter(description = "A single tag assigned to certain content elements.", required = false) @RequestParam(name = "tag", required = false) final String tag,
            @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
            @Parameter(hidden = true) final Pageable pgbl,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);
    
    @Operation(operationId = "findContentMetadata",
            summary = "Search content metadata.",
            description = "Search for content metadata in a paginated and/or sorted form by example using an example content metadata document "
            + "provided in the request body. Searchable properties are: `parentResource.id`, `relativePath`, `contentUri`, `mediaType`, `metadata` and `tags`."
            + "For string values, '%' can be used as wildcard character."
            + "<br/>"
            + "If the example document is omitted, the response is identical to listing all resources. "
            + "If enabled, authentication and authorization my affect the number of returned elements depending on the "
            + "permission on the element's parent resource.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(value = {"/search/data"}, method = {RequestMethod.POST})
    @ResponseBody
    @PageableAsQueryParam
    public ResponseEntity<List<ContentInformation>> findContentMetadataByExample(
            @Parameter(description = "JSON representation of content metadata used as input for the search operation.", required = true) @RequestBody final ContentInformation c,
            @Parameter(hidden = true) final Pageable pgbl,
            final WebRequest wr,
            final HttpServletResponse hsr,
            final UriComponentsBuilder ucb);

    @Operation(operationId = "patchContentMetadata",
            summary = "Patch content metadata.",
            description = "This endpoint allows to patch single content metadata document associated with a data resource. "
            + "Therefor, an RFC-6902 compliant patch document must be provided within the request body."
            + "Furthermore, providing an `If-Match` header including the current ETag of the content element is required."
            + "As most of the content metadata attributes are typically automatically generated, their modification is restricted "
            + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE for the parent resource. "
            + "Users having WRITE permissions to the associated resource are only allowed to modify the properties `metadata` and `tags`."
            + "<br/>"
            + "Both restrictions only apply if authentication and authorization is enabled.",
            security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.PATCH, consumes = "application/json-patch+json")
    @ResponseBody
    public ResponseEntity patchContentMetadata(
            @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            @Parameter(description = "Json representation of a json patch document. The document must comply with RFC 6902 specified by the IETF.", required = true) @RequestBody final JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response);

     @Operation(operationId = "patchContentMetadataPid",
            summary = "Patch content metadata.",
            description = "This endpoint allows to patch single content metadata document associated with a data resource. "
            + "Therefor, an RFC-6902 compliant patch document must be provided within the request body."
            + "Furthermore, providing an `If-Match` header including the current ETag of the content element is required."
            + "As most of the content metadata attributes are typically automatically generated, their modification is restricted "
            + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE for the parent resource. "
            + "Users having WRITE permissions to the associated resource are only allowed to modify the properties `metadata` and `tags`."
            + "<br/>"
            + "Both restrictions only apply if authentication and authorization is enabled.",
            security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{prefix}/{suffix}/data/**", method = RequestMethod.PATCH, consumes = "application/json-patch+json")
    @ResponseBody
    public ResponseEntity patchContentMetadataPid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable(value = "prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable(value = "suffix") final String suffix,
            @Parameter(description = "Json representation of a json patch document. The document must comply with RFC 6902 specified by the IETF.", required = true) @RequestBody final JsonPatch patch,
            final WebRequest request,
            final HttpServletResponse response);
    
    @Operation(
            operationId = "downloadContent",
            summary = "Download content assigned to a resource.",
            description = "This endpoint allows to download the data associated with a data resource and located at a particular virtual part. The virtual path starts after 'data/' and should end with a filename. "
            + "Depending on the content located at the provided path, different response scenarios can occur. If the content is a locally stored, accessible file, the bitstream of the file is retured. If the file is (temporarily) not available, "
            + "HTTP 404 is returned. If the content referes to an externally stored resource accessible via http(s), the service will try if the resource is accessible. If this is the case, the service will return HTTP 303 (SEE_OTHER) together "
            + "with the resource URI in the 'Location' header. Depending on the client, the request is then redirected and the bitstream is returned. If the resource is not accessible or if the protocol is not http(s), the service "
            + "will either return the status received by accessing the resource URI, SERVICE_UNAVAILABLE if the request has failed or NO_CONTENT if not other status applies. In addition, the resource URI is returned in the 'Content-Location' header "
            + "in case the client wants to try to access the resource URI.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET)
    @ResponseBody
    public void getContent(
            @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);

    @Operation(
            operationId = "downloadContentPid",
            summary = "Download content assigned to a resource.",
            description = "This endpoint allows to download the data associated with a data resource and located at a particular virtual part. The virtual path starts after 'data/' and should end with a filename. "
            + "Depending on the content located at the provided path, different response scenarios can occur. If the content is a locally stored, accessible file, the bitstream of the file is retured. If the file is (temporarily) not available, "
            + "HTTP 404 is returned. If the content referes to an externally stored resource accessible via http(s), the service will try if the resource is accessible. If this is the case, the service will return HTTP 303 (SEE_OTHER) together "
            + "with the resource URI in the 'Location' header. Depending on the client, the request is then redirected and the bitstream is returned. If the resource is not accessible or if the protocol is not http(s), the service "
            + "will either return the status received by accessing the resource URI, SERVICE_UNAVAILABLE if the request has failed or NO_CONTENT if not other status applies. In addition, the resource URI is returned in the 'Content-Location' header "
            + "in case the client wants to try to access the resource URI.", security = {
                @SecurityRequirement(name = "bearer-jwt")})
    @RequestMapping(path = "/{prefix}/{suffix}/data/**", method = RequestMethod.GET)
    @ResponseBody
    public void getContentPid(
            @Parameter(description = "The pid prefix.", required = true) @PathVariable(value = "prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable(value = "suffix") final String suffix,
            @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
            final WebRequest request,
            final HttpServletResponse response,
            final UriComponentsBuilder uriBuilder);

    
    @Operation(operationId = "deleteContent",
            summary = "Remove content from a resource.",
            description = "This endpoint allows to remove single content elements from a data resource. It is NOT possible to remove multiple elements "
            + "in one call, e.g., by providing a path ending with a slash. Furthermore, providing an `If-Match` header including the current ETag "
            + "of the content element is required."
            + "<br/>"
            + "If authentication and authorization is enabled, temoving content is restricted "
            + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE for the parent resource.")
    @RequestMapping(path = "/{id}/data/**", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity deleteContent(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
            final WebRequest request,
            final HttpServletResponse response);
    
    @Operation(operationId = "deleteContentPid",
            summary = "Remove content from a resource.",
            description = "This endpoint allows to remove single content elements from a data resource. It is NOT possible to remove multiple elements "
            + "in one call, e.g., by providing a path ending with a slash. Furthermore, providing an `If-Match` header including the current ETag "
            + "of the content element is required."
            + "<br/>"
            + "If authentication and authorization is enabled, temoving content is restricted "
            + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE for the parent resource.")
    @RequestMapping(path = "/{prefix}/{suffix}/data/**", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity deleteContentPid(@Parameter(description = "The pid prefix.", required = true) @PathVariable(value = "prefix") final String prefix,
            @Parameter(description = "The pid suffix.", required = true) @PathVariable(value = "suffix") final String suffix,
            final WebRequest request,
            final HttpServletResponse response);

}
