/*
 * Copyright 2017 Karlsruhe Institute of Technology.
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
//@Path("/auditEvents")
@Api(value = "Audit Event Handling")
@ApiResponses(
        value = {
            @ApiResponse(code = 401, message = "Unauthorized")
            ,
        @ApiResponse(code = 403, message = "Forbidden")
            ,
        @ApiResponse(code = 500, message = "Internal server error")
        })
public class AuditEventEndpoint {

//    @Context
//    SecurityContext securityContext;
//    @Context
//    Request request;
//
//    @POST
//    @Path(value = "/")
//    @ApiOperation(value = "Create a new audit event.",
//            notes = "Audit events can either be created by internal, trusted services, e.g. a base repository instance, or by external services. "
//            + "Their source (internal or external) is specified by the audit event's trigger type. If an untrusted service requests the creation "
//            + "of an internal audit event, the request will fail with HTTP status 400. "
//            + "Creating internal audit events for a data resource requires write access to the resource itself, for external events read access to "
//            + "the resource is, due to the reduced trust level of external events, sufficient. Thus, an Authorization header with a proper session id "
//            + "must be provided if not configured otherwise.", response = AuditEvent.class)
//    @ApiResponses(value = {
//        @ApiResponse(code = 201, message = "Successfully created audit event.", response = AuditEvent.class)
//        ,
//        @ApiResponse(code = 400, message = "Invalid request, e.g. mandatory field is missing.")
//    })
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    public Response createAuditEvent(
//            @ApiParam(value = "The JSON representation of the audit event.", required = true) AuditEvent data
//    ) {
//        //use arango id as audit event id
//
//        return Response.created(URI.create("http://localhost:8080/api/v1/auditEvents/1234-5678-90")).build();
//    }
//
//    @GET
//    @Path(value = "/")
//    @ApiOperation(value = "Get a subset of all audit events.",
//            notes = "The list of results should be refined by providing a query argument, e.g. for selecting only audit events for a certain resource identifier. "
//            + "Relevant results are returned pagewise with the provided number of max. results per page ordered ascending or descending by the provided sort field. "
//            + "Accessing audit events for a data resource requires at least read access to the resource itself. "
//            + "Thus, an Authorization header with a proper session id must be provided if not configured otherwise.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Success", response = AuditEvent[].class)
//    })
//    // @HalEmbedded(name = "dataArchives")
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    public Response getAuditEvents(
//            @ApiParam(value = "The query refining the result set.", required = false) @QueryParam("query") String query,
//            @ApiParam(value = "The page to show.", required = false) @QueryParam("page") @DefaultValue("1") Integer page,
//            @ApiParam(value = "The max. number of results per page.", required = false) @QueryParam("resultsPerPage") @DefaultValue("10") Integer resultsPerPage,
//            @ApiParam(value = "The field name by which the result set is sorted.", required = false) @QueryParam("sort") String sortField,
//            @ApiParam(value = "The result sort order (asc or desc).", required = false) @QueryParam("order") @DefaultValue("asc") String order
//    ) {
//
//        //add Link header for pagination 
//        return Response.ok().header("Link", "<http://localhost:8080/api/v1/auditEvents?page=2&resultsPerPage=10&sort=sortField&order=order>; rel=\"next\", "
//                + " <http://localhost:8080/api/v1/auditEvents?page=10&resultsPerPage=10&sort=sortField&order=order>; rel=\"last\"").build();
//    }
//
//    @GET
//    @Path(value = "/{auditEventId}")
//    @ApiOperation(value = "Get a single audit event by its internal event id.",
//            notes = "Accessing audit events for a data resource requires at least read access to the resource itself. "
//            + "Thus, an Authorization header with a proper session id must be provided if not configured otherwise.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Success", response = AuditEvent.class)
//    })
//    //@HalEmbedded(name = "dataArchives")
//    @Produces({"application/json", "application/xml", "application/hal+json"})
//    public Response getAuditEventById(@ApiParam(value = "The internal event id of the audit event.", required = true) @PathParam("auditEventId") String auditEventId//,
//    //  @ApiParam(value = "The ETag of the resource.", hidden = true) @HeaderParam(HttpHeaders.IF_NONE_MATCH) String etag
//    ) {
//
//        //add Link header for pagination 
//        return Response.ok().build();
//    }
}
