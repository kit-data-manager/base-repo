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
package edu.kit.dama.service.rest.impl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 *
 * @author jejkal
 */
//@Path("/dataResources")
@Api(value = "Data Resources Handling")
@ApiResponses(
        value = {
            @ApiResponse(code = 401, message = "Unauthorized")
            ,
        @ApiResponse(code = 403, message = "Forbidden")
            ,
        @ApiResponse(code = 500, message = "Internal server error")
        })
public class DataResourceEndpoint{// extends AbstractBaseResource<DataResource> {

//    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DataResourceEndpoint.class);
//
//    @Context
//    ContainerRequestContext context;
//
//    @Override
//    public ContainerRequestContext getContext() {
//        return context;
//    }
//
//    @POST
//    @Path(value = "/")
//    @ApiOperation(value = "Create a new data resource.", httpMethod = "POST",
//            notes = "Creating data resources typically requires an Authorization header with a proper session id if not configured otherwise.", response = DataResource.class)
//    @ApiResponses(value = {
//        @ApiResponse(code = 201, message = "Successfully created data resource.", response = DataResource.class)
//        ,
//        @ApiResponse(code = 303, message = "There is already a resource with the provided identifier. The link to the existing resource is returned in the Location header.")
//        ,
//        @ApiResponse(code = 400, message = "Bad request, e.g. resource or mandatory field is missing.")
//        ,
//      @ApiResponse(code = 409, message = "Conflict, e.g. there is already a resource with the provided primary identifier.")
//    })
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    @RoleRequired(Role.USER)
//    public Response createDataResource(
//            @ApiParam(value = "The JSON representation of the data resource.", required = true) DataResource resource
//    ) {
//        String sessionId = ServiceUtil.getSessionIdFromResource(this);
//
//        //check argument
//        if (resource == null) {
//            return ResponseBuilder.buildResponse(Response.Status.BAD_REQUEST, ErrorUtils.NO_RESOURCE_PROVIDED_ERROR);
//        }
//        //validate provided resource and obtain provided identifier
//        String identifier;
//        try {
//            resource.validate();
//            identifier = resource.getResourceIdentifier();
//        } catch (InvalidResourceException ex) {
//            LOGGER.error("Failed to validate provided resource " + resource, ex);
//            return ResponseBuilder.buildResponse(Response.Status.BAD_REQUEST, ErrorUtils.getInvalidResourceError(ex.getMessage()));
//        }
//
//        //check for existing resource with same identifier
//        DataResource existingResource = null;
//        try {
//            existingResource = getResourceService().read(identifier, sessionId);
//        } catch (ServiceException ex) {
//            if (ex.getStatusCode() != 404) {
//                LOGGER.error("Failed to check for existing resource with identifier " + identifier, ex);
//                return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.getInternalServerError(ex.getMessage()));
//            } else {
//                LOGGER.trace("No existing resource found. Continuing with resource creation.");
//            }
//        }
//
//        if (existingResource != null) {
//            LOGGER.trace("Existing data resource with identifier '{}' found. Redirecting caller to existing resource.", identifier);
//            //Build response with location referring to existing resource. The session id is provided as query parameter as headers are not forwarded at client-side redirection.
//            return Response.
//                    status(Response.Status.SEE_OTHER).
//                    location(ServiceUtil.getResourceUri(getContext(), identifier, sessionId)).
//                    build();
//        }
//
//        //New resource detected, check auto-assignable properties
//        if (resource.getPublicationYear() == null) {
//            String year = new SimpleDateFormat("YYYY").format(new Date());
//            LOGGER.trace("Resource has no publication year provided. Using current year '{}'.", year);
//            resource.setPublicationYear(year);
//        }
//
//        if (resource.getCreationDate() == null) {
//            LOGGER.trace("Resource has no creation date. Setting current date.");
//            edu.kit.dama.entities.dc40.Date creationDate = new edu.kit.dama.entities.dc40.Date();
//            creationDate.setType(edu.kit.dama.entities.dc40.Date.DATE_TYPE.CREATED);
//            creationDate.setDate(new Date());
//            resource.addDate(creationDate);
//        }
//        UserPrincipal principal = (UserPrincipal) getContext().getSecurityContext().getUserPrincipal();
//
//        if (resource.getCreator().isEmpty()) {
//            LOGGER.trace("Resource has no creator provided. Setting caller '{}' as creator.", resource, principal.getPrincipalUser());
//            resource.getCreator().add(principal.getPrincipalUser());
//        }
//
//        //now, the resource is valid and can be stored
//        LOGGER.trace("Storing resource in backend.");
//        try {
//            getResourceService().create(resource, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to store resource in backend.", ex);
//            return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.getInternalServerError(ex.getMessage()));
//        }
//
//        //set cache to one hour
//        CacheControl cc = new CacheControl();
//        cc.setMaxAge((int) DateUtils.MILLIS_PER_HOUR);
//        cc.setPrivate(true);
//        //return response including location, cache information and etag
//        return Response.created(ServiceUtil.getResourceUri(getContext(), resource.getResourceIdentifier())).
//                cacheControl(cc).
//                tag(Integer.toString(resource.hashCode())).
//                entity(resource).
//                build();
//    }
//
//    @GET
//    @Path(value = "/")
//    @ApiOperation(value = "Get a subset of all data resources.",
//            notes = "The result list can be refined by providing a query as argument. Relevant results are returned pagewise with the provided "
//            + "number of max. results per page ordered ascending or descending by the provided sort field."
//            + "Accessing data resources typically requires at least read access to the resource. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Success", response = DataResource[].class)
//    })
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    @RoleRequired(Role.GUEST)
//    public Response getDataResources(
//            @ApiParam(value = "The query refining the result set.", required = false) @QueryParam("query") String query,
//            @ApiParam(value = "The page to show.", required = false) @QueryParam("page") @DefaultValue("1") Integer page,
//            @ApiParam(value = "The max. number of results per page.", required = false) @QueryParam("resultsPerPage") @DefaultValue("10") Integer resultsPerPage,
//            @ApiParam(value = "The field name by which the result set is sorted.", required = false) @QueryParam("sort") String sortField,
//            @ApiParam(value = "The result sort order (asc or desc).", required = false) @QueryParam("order") @DefaultValue("ASC") IBaseDao.SORT_ORDER sortOrder
//    ) {
//        String sessionId = ServiceUtil.getSessionIdFromResource(this);
//
//        LOGGER.trace("Adding filter for revoked resource to query.");
//        String theQuery = ((query == null) ? "" : query + " && ") + " e.state != " + DataResource.State.REVOKED;
//
//        //Obtain all resource ids
//        QueryResult<DataResource> results;
//        try {
//            results = getResourceService().read(theQuery, null, page, resultsPerPage, sortField, sortOrder, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to perform query for data resources using query string " + query + ".", ex);
//            return ex.toResponse();
//        }
//        //obtain cache control
//        CacheControl cc = new CacheControl();
//        cc.setMaxAge((int) DateUtils.MILLIS_PER_HOUR);
//        cc.setPrivate(true);
//
//        int nextPage = (page * resultsPerPage > results.getOverallResults()) ? 0 : page + 1;
//
//        Collection<Link> links = new ArrayList<>();
//        if (nextPage > 0) {
//            //only add next page if there is one
//            links.add(Link.fromUri(ServiceUtil.getNextPageLink(getContext(), page, resultsPerPage, sortField, sortOrder, sessionId)).param("rel", "next").build());
//        }
//
//        links.add(Link.fromUri(ServiceUtil.getLastPageLink(getContext(), results.getOverallResults(), resultsPerPage, sortField, sortOrder, sessionId)).param("rel", "last").build());
//
//        //return results including cache information and links to next/prev page
//        return Response.ok(results.getResults().toArray(new DataResource[]{})).
//                cacheControl(cc).
//                links(links.toArray(new Link[]{}))
//                .build();
//    }
//
//    @GET
//    @Path(value = "/{dataResourceIdentifier}")
//    @ApiOperation(value = "Get the data resource with the provided primary identifier.",
//            notes = "Accessing data resources typically requires at least read access to the resource. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise."
//    )
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Success", response = DataResource.class)
//        ,
//        @ApiResponse(code = 404, message = "Not found, e.g. wrong primary identifier.")
//        ,
//            @ApiResponse(code = 410, message = "Gone, e.g. the resource has been revoked.")
//    })
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    @RoleRequired(Role.GUEST)
//    public Response getDataResourceById(
//            @ApiParam(value = "The primary identifier of the data resource.", required = true) @PathParam("dataResourceIdentifier") String dataResourceIdentifier,
//            @ApiParam(value = "The ETag of the resource.", hidden = true) @HeaderParam(HttpHeaders.IF_NONE_MATCH) String etag
//    ) {
//        String sessionId = ServiceUtil.getSessionIdFromResource(this);
//        //try to obtain the resource for the provided identifier
//        DataResource result;
//        try {
//            result = getResourceService().read(dataResourceIdentifier, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to obtain data resource with identifier " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//
//        if (DataResource.State.REVOKED.equals(result.getState())) {
//            LOGGER.info("Requested resource with identifier {} has been revoked. Returning HTTP 410.", dataResourceIdentifier);
//            return ResponseBuilder.buildResponse(Response.Status.GONE, ErrorUtils.RESOURCE_REVOKED_ERROR);
//        }
//
//        //check etag and return result only if resource has not been modified
//        EntityTag currentEtag = new EntityTag(Integer.toString(result.hashCode()));
//        Response.ResponseBuilder responseBuilder = getContext().getRequest().evaluatePreconditions(currentEtag);
//        if (responseBuilder == null) {
//            //return updated resource and new etag
//            return ResponseBuilder.buildResponse(Response.Status.OK, result, currentEtag.getValue());
//        }
//
//        //return response from response builder. Status should be NOT_MODIFIED.
//        return responseBuilder.build();
//    }
//
//    @PATCH
//    @Path(value = "/{dataResourceIdentifier}")
//    @ApiOperation(value = "Update the data resource with the provided unique identifier.",
//            notes = "The update is described by a patch document containing the update steps that should be applied. "
//            + "In order to check whether a resource has been updated in the meantime or not, an ETag must be provided with the update request. "
//            + "If the provided ETag does not match the current ETag, HTTP 409 will be returned."
//            + "Updating data resources typically requires write access to the resource. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 204, message = "No Content.")
//        ,
//        @ApiResponse(code = 404, message = "Resource not found, e.g. wrong primary data resource identifier.")
//        ,
//        @ApiResponse(code = 409, message = "Conflict, e.g. resource has changed.")
//        ,
//        @ApiResponse(code = 415, message = "Unsupported Media Type, e.g. unsupported patch document format.")
//    })
//    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    @RoleRequired(Role.USER)
//    public Response patchResource(
//            @ApiParam(value = "The primary identifier of the data resource.", required = true)
//            @PathParam("dataResourceIdentifier") String dataResourceIdentifier,
//            @ApiParam(value = "A JSON Patch document according to IETF RFC 6901.", example = "[\n"
//                    + "  { \"op\": \"replace\", \"path\": \"/identifier/value\", \"value\": \"NewValue\" }\n"
//                    + "]", required = true) String jsonPatchDocument,
//            @ApiParam(value = "The ETag of the resource.", hidden = true) @HeaderParam(HttpHeaders.IF_NONE_MATCH) String etag) {
//        //obtain the resource
//        DataResource resource;
//        String sessionId = ServiceUtil.getSessionIdFromRequest(getContext());
//        try {
//            resource = getResourceService().read(dataResourceIdentifier, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to obtain resource for identifier " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//
//        if (DataResource.State.FIXED.equals(resource.getState())) {
//            LOGGER.trace("Requested resource with identifier {} has been fixed. Patching not allowed.", dataResourceIdentifier);
//            return ResponseBuilder.buildResponse(423, ErrorUtils.RESOURCE_FIXED_ERROR);
//        } else if (DataResource.State.REVOKED.equals(resource.getState())) {
//            LOGGER.trace("Requested resource with identifier {} has been revoked. Patching not allowed.", dataResourceIdentifier);
//            return ResponseBuilder.buildResponse(Response.Status.GONE, ErrorUtils.RESOURCE_REVOKED_ERROR);
//        }
//
//        //check etag to avoid conflicts
//        ServiceUtil.checkETag(getContext().getRequest(), resource);
//        DateFormat f = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
//
//        //collect fields that can be changed only if the caller has write permissions...in that case, these fields are ignore, otherwise they are part of the validation hash
//        String forbiddenFields;
//        try {
//            Date creationDate = resource.getCreationDate();
//            if (creationDate == null) {
//                throw new InvalidResourceException(InvalidResourceException.ERROR_TYPE.NO_CREATION_DATE);
//            }
//            forbiddenFields = resource.getResourceIdentifier() + f.format(creationDate);
//        } catch (InvalidResourceException ex) {
//            LOGGER.error("Failed to patch resource with identifier " + dataResourceIdentifier + ". Resource did not return a proper creation date.", ex);
//            return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.getInvalidResourceError("Creation date missing."));
//        }
//        //hash 'locked' fields, e.g. identifier, identifier scheme and scheme URI
//        String hashBefore = ResourceUtils.hashElements(forbiddenFields);
//        try {
//            //apply patch to resource
//            resource = ServiceUtil.applyPatch(resource, jsonPatchDocument, DataResource.class);
//        } catch (IOException ex) {
//            LOGGER.error("Failed to apply patch '" + jsonPatchDocument + "' to resource '" + resource + "'", ex);
//            return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.getInvalidResourceError("Failed to apply patch."));
//        }
//
//        try {
//            Date creationDate = resource.getCreationDate();
//            if (creationDate == null) {
//                throw new InvalidResourceException(InvalidResourceException.ERROR_TYPE.NO_CREATION_DATE);
//            }
//            forbiddenFields = resource.getResourceIdentifier() + f.format(resource.getCreationDate());
//        } catch (InvalidResourceException ex) {
//            LOGGER.error("Creation date was removed by applying patch '" + jsonPatchDocument + "'.", ex);
//            return ResponseBuilder.buildResponse(Response.Status.BAD_REQUEST, ErrorUtils.getInvalidResourceError("Creation date modified."));
//        }
//
//        //create second hash of 'locked' fields
//        String hashAfter = ResourceUtils.hashElements(forbiddenFields);
//        //compare hashes
//        if (!hashBefore.equals(hashAfter)) {
//            //hashes not equal, invalid modification
//            LOGGER.error("Failed to apply patch '" + jsonPatchDocument + "' to resource '" + resource + "'. ");
//            //return HTTP UNPROCESSABLE_ENTITY
//            return Response.status(422).entity("Failed to apply JSON patch. Locked fields were affected.").build();
//        }
//        LOGGER.trace("Patch successfully applied to resource. Persisting updated resource.");
//        //Update successful, store the updated group and return.
//        try {
//            getResourceService().update(resource, sessionId);
//            LOGGER.trace("Updated resource successfully persisted.");
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to persist updated resource " + resource, ex);
//            return ex.toResponse();
//        }
//        return Response.status(Response.Status.NO_CONTENT).tag(Integer.toString(resource.hashCode())).build();
//    }
//
//    @POST
//    @Path(value = "/{dataResourceIdentifier}/{path : .*}")
//    @ApiOperation(value = "Upload binary data to the content area of the data resource with the provided primary identifier.",
//            notes = "Uploading data is possible as long as a resource is not fixed. Access to the uploaded data is possible as soon as the upload has been finished. "
//            + "However, some information that are extracted/generated after upload, e.g. checksums, might not be available immediately. "
//            + "The parameter 'path' can be used to organize the content of a data resource in a hierarchical way. If 'path' contains slashes they should NOT be escaped as %2F, at least if "
//            + "the service is deployed inside a Tomcat container. If escaping is desired/required, see http://tomcat.apache.org/security-6.html for more details."
//            + "The parameter 'path' should NOT contain the actual filename as it is extracted from the content disposition header."
//            + "Additional metadata, e.g. format, hash, or size, may or may not be provided within the metadata."
//            + "Uploading content requires write access to the data resource. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise."
//    )
//    @ApiResponses(value = {
//        @ApiResponse(code = 201, message = "Created")
//        ,
//        @ApiResponse(code = 404, message = "Not found, e.g. wrong primary data resource identifier.")
//    })
//    @ApiImplicitParams({
//        @ApiImplicitParam(name = "file", value = "The binary file to upload.",
//                required = true, dataType = "java.io.File", paramType = "form")
//        ,
//    @ApiImplicitParam(name = "metadata", value = "A metadata document in JSON format. "
//                + "The document can be accessed using the 'metadata' query parameter together with the download endpoint afterwards.",
//                required = false, dataType = "java.io.File", paramType = "form")})
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces(MediaType.APPLICATION_JSON)
//    @RoleRequired(Role.USER)
//    public Response uploadContent(
//            @ApiParam(value = "The primary identifier of the data resource.", required = true)
//            @PathParam("dataResourceIdentifier") String dataResourceIdentifier,
//            @ApiParam(value = "The path where the content is uploaded to. This path must not contain the filename itself, which has to be provided via content disposition header.", required = true)
//            @PathParam("path") String path,
//            @ApiParam(hidden = true)
//            @FormDataParam("file") InputStream fileInputStream,
//            @ApiParam(hidden = true)
//            @FormDataParam("file") FormDataContentDisposition cdh,
//            @ApiParam(value = "The content metadata associated with the uploaded file.", required = false)
//            @FormDataParam("metadata") ContentDescriptor metadata
//    // @ApiParam(hidden = true)
//    // @FormDataParam("metadata") InputStream mdFileInputStream,
//    // @ApiParam(hidden = true)
//    // @FormDataParam("metadata") FormDataContentDisposition mdCdh
//    ) {
//
//        LOGGER.trace("Checking for provided content..");
//        if (cdh == null) {
//            return ResponseBuilder.buildResponse(Response.Status.BAD_REQUEST, ErrorUtils.NO_DATA_ERROR);
//        }
//
//        //boolean haveMetadata = (mdCdh != null);
//        boolean haveMetadata = (metadata != null);
//        String sessionId = ServiceUtil.getSessionIdFromRequest(getContext());
//        DataResource resource;
//        try {
//            resource = getResourceService().read(dataResourceIdentifier, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to obtain resource for identifier " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//        if (DataResource.State.FIXED.equals(resource.getState())) {
//            return ResponseBuilder.buildResponse(423, ErrorUtils.RESOURCE_FIXED_ERROR);
//        } else if (DataResource.State.REVOKED.equals(resource.getState())) {
//            return ResponseBuilder.buildResponse(Response.Status.GONE, ErrorUtils.RESOURCE_REVOKED_ERROR);
//        }
//
//        String dataLocation;
//        try {
//            dataLocation = resource.getDataLocation();
//        } catch (InvalidResourceException ex) {
//            LOGGER.error("Failed to obtain data location for resource " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//
//        if (path.startsWith("/")) {
//            path = path.substring(1);
//        }
//
//        if (!path.endsWith("/")) {
//            path += "/";
//        }
//
//        if (cdh.getFileName() == null) {
//            return ResponseBuilder.buildResponse(Response.Status.BAD_REQUEST, ErrorUtils.CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME_ERROR);
//        }
//
//        //@TODO Make configurable
//        String basePath = "/Users/jejkal/tmp/baseRepo";
//        String resourcePath = dataLocation + "/" + path + "/" + cdh.getFileName();
//        String filePath = basePath + "/" + resourcePath;
//        File dataDestination = new File(filePath);
//
//        //check if parent exists first
//        if (!dataDestination.getParentFile().exists()) {
//            LOGGER.debug("Resource data folder at {} does not exist, yet. Creating folder structure.", dataDestination.getParentFile());
//            dataDestination.getParentFile().mkdirs();
//        }
//        boolean overwrite = false;
//        //check for existing file
//        if (dataDestination.exists() && dataDestination.isFile()) {
//            //overwrite existing file
//            LOGGER.info("Uploaded file named {} for resource {} already exists at {}. Overwriting existing file.", cdh.getFileName(), dataResourceIdentifier, dataDestination);
//            overwrite = true;
//        } else if (dataDestination.exists() && !dataDestination.isFile()) {
//            LOGGER.error("Upload not possible. Destination " + dataDestination + " is an existing folder.");
//            return ResponseBuilder.buildResponse(Response.Status.CONFLICT, ErrorUtils.INVALID_UPLOAD_DESTINATION_ERROR);
//        } else {
//            //destination does not exist, create new file
//            LOGGER.trace("Receiving new file named {} for resource {}. Storing file to {}.", cdh.getFileName(), dataResourceIdentifier, dataDestination);
//        }
//
//        LOGGER.trace("Writing data stream to destination {}.", dataDestination);
//        int dataSum = 0;
//        MessageDigest digest = null;
//        try {
//            LOGGER.trace("Creating SHA-1 digest.");
//            digest = MessageDigest.getInstance("SHA-1");
//            LOGGER.trace("SHA-1 digest created. Data file hashing supported.");
//        } catch (NoSuchAlgorithmException ex) {
//            LOGGER.warn("Failed to create SHA-1 digest. Data file hashing unsupported.", ex);
//        }
//        try (FileOutputStream out = new FileOutputStream(dataDestination)) {
//            try (InputStream in = fileInputStream) {
//                byte[] data = new byte[10 * 1024];
//                int read;
//                while ((read = in.read(data)) != -1) {
//                    dataSum += read;
//                    out.write(data, 0, read);
//                    if (haveMetadata && digest != null && read > 0) {
//                        digest.update(data, 0, read);
//                    }
//                    data = new byte[1024];
//                }
//            } catch (IOException ex) {
//                LOGGER.error("Failed to read data from stream.", ex);
//                return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.DATA_STREAM_READ_ERROR);
//
//            }
//        } catch (FileNotFoundException ex) {
//            LOGGER.error("Failed to open output stream for data upload destination " + dataDestination, ex);
//            return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.DATA_STREAM_WRITE_ERROR);
//        } catch (IOException ex) {
//            LOGGER.error("Failed to write to data upload destination " + dataDestination, ex);
//            return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.DATA_STREAM_WRITE_ERROR);
//        }
//        LOGGER.debug("Successfully received {} bytes of data stored at {}.", dataSum, dataDestination);
//
//        if (!overwrite) {
//            URI resourceUri = ServiceUtil.getResourceUri(getContext(), dataResourceIdentifier);
//            resourceUri = URI.create(resourceUri.toString() + "/" + path + "/" + cdh.getFileName());
//            LOGGER.debug("Adding related identifier for data URL {}.", resourceUri);
//            RelatedIdentifier relatedIdentifier = new RelatedIdentifier();
//            relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
//            relatedIdentifier.setRelationType(RelatedIdentifier.RELATION_TYPES.IS_PART_OF);
//            relatedIdentifier.setValue(resourceUri.toString());
//            resource.getRelatedIdentifier().add(relatedIdentifier);
//        }
//
//        if (haveMetadata) {
//            LOGGER.trace("Detected content metadata stream. Deserializing metadata document information.");
//            try {
//                ContentDescriptor fileMetadata = metadata;//new ObjectMapper().readValue(mdFileInputStream, ContentElement.class);
//                fileMetadata.setParentDataResourceIdentifier(dataResourceIdentifier);
//                fileMetadata.setContentPath(resourcePath);
//                fileMetadata.setSize(dataSum);
//                if (digest != null) {
//                    fileMetadata.setHash("sha1:" + Hex.encodeHexString(digest.digest()));
//                }
//                LOGGER.trace("Persisting content metadata.");
//                getContentDescriptorService().create(fileMetadata, sessionId);
//                LOGGER.trace("Content metadata successfully persisted.");
//                //} catch (IOException ex) {
//                //     LOGGER.error("Failed to deserialize content metadata document.", ex);
//                //      return Response.status(Response.Status.BAD_REQUEST).entity("Invalid metadata document.").type(MediaType.TEXT_PLAIN).build();
//            } catch (ServiceException ex) {
//                LOGGER.error("Failed to persist content metadata.", ex);
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to persist content metadata.").type(MediaType.TEXT_PLAIN).build();
//            }
//        }
//
//        LOGGER.trace("Adding 'updated' date to resource.");
//        edu.kit.dama.entities.dc40.Date updated = new edu.kit.dama.entities.dc40.Date();
//        updated.setType(edu.kit.dama.entities.dc40.Date.DATE_TYPE.UPDATED);
//        updated.setDate(new Date());
//        resource.addDate(updated);
//
//        try {
//            LOGGER.trace("Persisting updated resource.");
//            getResourceService().update(resource, sessionId);
//            LOGGER.trace("Resource successfully persisted.");
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to update data resource with identifier " + dataResourceIdentifier + " after data upload.", ex);
//            return ex.toResponse();
//        }
//
//        URI resourceUri = ServiceUtil.getResourceUri(getContext(), dataResourceIdentifier);
//        return Response.created(URI.create(resourceUri.toString() + "/" + path + "/" + cdh.getFileName())).build();
//    }
//
//    @GET
//    @Path(value = "/{dataResourceIdentifier}/{path: .*}")
//    @ApiOperation(value = "Download associated metadata or binary data from the content area of the data resource with the provided primary identifier.",
//            notes = "The parameter 'path' can be used to access hierarchically structured data elements. In order to access associated metadata, "
//            + "e.g. content type or creator, a flag 'metadata' must be provided as query parameter. "
//            + "Downloading content requires at least read access to the data resource. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Success")
//        ,
//        @ApiResponse(code = 404, message = "Not found, e.g. wrong data resource identifier or invalid path.")
//    })
//    @Produces("*/*")
//    @RoleRequired(Role.GUEST)
//    public Response downloadContent(
//            @ApiParam(value = "The primary identifier of the data resource.", required = true)
//            @PathParam("dataResourceIdentifier") String dataResourceIdentifier,
//            @ApiParam(value = "The path where the content is downloaded from.", required = true)
//            @PathParam("path") String path,
//            @ApiParam(value = "Flag to indicate metadata or data access.", required = false)
//            @QueryParam("metadata")
//            @DefaultValue("false") boolean metadata
//    ) {
//
//        DataResource resource;
//        try {
//            String sessionId = ServiceUtil.getSessionIdFromRequest(getContext());
//            resource = getResourceService().read(dataResourceIdentifier, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to obtain resource for identifier " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//        if (DataResource.State.REVOKED.equals(resource.getState())) {
//            return Response.status(Response.Status.GONE).entity("The resource has been revoked.").type(MediaType.TEXT_PLAIN).build();
//        }
//
//        String dataLocation;
//        try {
//            dataLocation = resource.getDataLocation();
//        } catch (InvalidResourceException ex) {
//            LOGGER.error("Failed to obtain data location for resource " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//
//        if (path.startsWith("/")) {
//            path = path.substring(1);
//        }
//
//        if (!path.endsWith("/")) {
//            path += "/";
//        }
//
//        //@TODO Make configurable
//        String basePath = "/Users/jejkal/tmp/baseRepo";
//
//        String filePath = basePath + "/" + dataLocation + "/" + path;
//
//        File dataSource = new File(filePath);
//
//        if (!dataSource.exists()) {
//            LOGGER.trace("Request to non-existing data at {}.", path);
//            return Response.status(Response.Status.NOT_FOUND).entity("No data found at at this location.").type(MediaType.TEXT_PLAIN).build();
//        }
//
//        String mediaType = MediaType.APPLICATION_OCTET_STREAM;
//        try {
//            LOGGER.trace("Obtaining content metadata in order to determine content type.");
//            ContentDescriptor contentDescriptor = getContentDescriptorService().read(dataResourceIdentifier);
//            if (metadata) {
//                return Response.ok(contentDescriptor).build();
//            }
//            mediaType = contentDescriptor.getMediaType();
//        } catch (ServiceException ex) {
//            if (!metadata) {
//                LOGGER.info("Unable to obtain content metadata for data resource with identfier " + dataResourceIdentifier + ". Using media type " + mediaType);
//            } else {
//                LOGGER.error("Unable to obtain content metadata for data resource with identfier " + dataResourceIdentifier + ". Returning HTTP 404.", ex);
//                return ResponseBuilder.buildResponse(Response.Status.NOT_FOUND, ErrorUtils.NO_CONTENT_METADATA_ERROR);
//            }
//        }
//
//        if (dataSource.isDirectory()) {
//            LOGGER.trace("Performing zipped stream collection download from {}.", dataSource);
//            try {
//                LOGGER.debug("Establishing piped streams.");
//                PipedInputStream in = new PipedInputStream();
//                final ZipOutputStream out = new ZipOutputStream(new PipedOutputStream(in));
//                LOGGER.debug("Starting streaming thread.");
//                new Thread(() -> {
//                    try {
//                        LOGGER.debug("Start zipping operation to piped output stream.");
//                        ZipUtils.zip(dataSource.listFiles(), dataSource.getAbsolutePath(), out);
//                        //compressDirectoryToZipfile(dataSource.getAbsolutePath(), dataSource.getAbsolutePath(), out);
//                        LOGGER.debug("Zipping operation finshed.");
//                    } catch (IOException ex) {
//                        LOGGER.error("Failed to write data to output stream.", ex);
//                    } finally {
//                        try {
//                            out.flush();
//                            out.close();
//                        } catch (IOException ex) {
//                            //ignore
//                        }
//                    }
//                }).start();
//
//                String resultName = FilenameUtils.escapeStringAsFilename(dataResourceIdentifier);
//
//                LOGGER.debug("Returning response file named '{}' in stream linked to piped zip stream.", resultName);
//                //Using piped input stream rather than streaming output via DowloadStreamWrapper as this seems not to work here. 
//                return Response.ok(in, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + resultName + ".zip\"").build();
//            } catch (IOException ex) {
//                LOGGER.error("Failed to write data to output stream.", ex);
//                return ResponseBuilder.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ErrorUtils.DATA_STREAM_READ_ERROR);
//            }
//        } else {
//            //simple download
//            try {
//                LOGGER.trace("Performing single stream download from {} with media type {}.", dataSource, mediaType);
//                return Response.ok(new FileInputStream(dataSource), mediaType).build();
//            } catch (FileNotFoundException ex) {
//                LOGGER.error("Failed to open data stream to file " + dataSource, ex);
//                return ResponseBuilder.buildResponse(Response.Status.NOT_FOUND, ErrorUtils.DATA_STREAM_READ_ERROR);
//            }
//        }
//    }
//
//    @DELETE
//    @Path(value = "/{dataResourceIdentifier}")
//    @ApiOperation(value = "Revoke the data resource with the provided identifier.", notes = "In order to be able to revoke a resource, "
//            + "the caller must authenticate as the session user using one of the configured authentication methods. Authentication information are "
//            + "provided depending on the authentication method, e.g. in the header.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 204, message = "No Content.")
//    })
//    @RoleRequired(Role.USER)
//    public Response deleteDataResource(@ApiParam(value = "The data resource identifier .", required = false)
//            @PathParam("dataResourceIdentifier") String dataResourceIdentifier,
//            @ApiParam(value = "The ETag of the resource.", hidden = true) @HeaderParam(HttpHeaders.IF_NONE_MATCH) String etag
//    ) {
//        String sessionId = ServiceUtil.getSessionIdFromRequest(getContext());
//        //obtain group entity
//        DataResource resource;
//        try {
//            resource = getResourceService().read(dataResourceIdentifier, sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to obtain resource for identifier " + dataResourceIdentifier, ex);
//            return ex.toResponse();
//        }
//
//        //check etag to avoid conflicts
//        ServiceUtil.checkETag(getContext().getRequest(), resource);
//
//        try {
//            getResourceService().delete(resource.getId(), sessionId);
//        } catch (ServiceException ex) {
//            LOGGER.error("Failed to delete data resource.", ex);
//            return ex.toResponse();
//        }
//
//        return Response.status(Response.Status.NO_CONTENT).build();
//    }
//
//    @Override
//    public IDataResourceServiceAdapter getResourceService() {
//        return ServiceUtil.getService(IDataResourceServiceAdapter.class);
//    }
//
//    private IContentDescriptorServiceAdapter getContentDescriptorService() throws ServiceException {
//        return ServiceUtil.getService(IContentDescriptorServiceAdapter.class);
//    }
//
////    public void compressZipfile(String sourceDir, String outputFile) throws IOException, FileNotFoundException {
////        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile));
////        compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
////        IOUtils.closeQuietly(zipFile);
////    }
////
////    private void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException, FileNotFoundException {
////        
////        for (File file : new File(sourceDir).listFiles()) {
////            if (file.isDirectory()) {
////                compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
////            } else {
////                ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
////                out.putNextEntry(entry);
////
////                FileInputStream in = new FileInputStream(sourceDir + File.separator + file.getName());
////                IOUtils.copy(in, out);
////                IOUtils.closeQuietly(in);
////            }
////        }
////    }
}
