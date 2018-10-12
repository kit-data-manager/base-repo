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
package edu.kit.datamanager.repo.service;

import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.service.IGenericService;
import java.util.List;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author jejkal
 */
public interface IDataResourceService extends IGenericService<DataResource>, HealthIndicator{

  /**
   * Create a new data resource using the provided template. Where possible,
   * assigned fields of the provided resource are used. Other fields, e.g. the
   * numeric id, are overwritten if they are already assigned. Some fields,
   * which are mandatory according to the DataCite specification, e.g. at least
   * one title and resource type, must be already provided and should be
   * checked. Furthermore, the value of the internal identifier, if provided,
   * must not be null as the internal identifier might be used as resource
   * identifier if no DOI is provided as primary identifier.
   *
   * Other fields, mandatory according to the DataCite specification, should be
   * automatically filled, if not provided, e.g. the creator, publisher or
   * publication year.
   *
   * @param resource The resource template to be used to create a new resource.
   * @param callerPrincipal The principal of the caller, e.g. the user- or
   * servicename.
   * @param callerFirstName The firstname of the caller, if available.
   * @param callerLastName The lastname of the caller, if available.
   *
   * @return The new resource with an id assigned.
   *
   * @throws BadArgumentException if the value of an provided internal
   * identifier is null or if no title and/or resource type are provided.
   * @throws ResourceAlreadyExistException if a resource with the same
   * identifier already exists.
   */
  DataResource create(DataResource resource, String callerPrincipal, String callerFirstName, String callerLastName) throws BadArgumentException, ResourceAlreadyExistException;

  /**
   * Create a new data resource using the provided template. Where possible,
   * assigned fields of the provided resource are used. Other fields, e.g. the
   * numeric id, are overwritten if they are already assigned. Some fields,
   * which are mandatory according to the DataCite specification, e.g. at least
   * one title and resource type, must be already provided and should be
   * checked. Furthermore, the value of the internal identifier, if provided,
   * must not be null as the internal identifier might be used as resource
   * identifier if no DOI is provided as primary identifier.
   *
   * Other fields, mandatory according to the DataCite specification, should be
   * automatically filled, if not provided, e.g. the creator, publisher or
   * publication year.
   *
   * @param resource The resource template to be used to create a new resource.
   * @param callerPrincipal The principal of the caller, e.g. the user- or
   * servicename.
   *
   * @return The new resource with an id assigned.
   *
   * @throws BadArgumentException if the value of an provided internal
   * identifier is null or if no title and/or resource type are provided.
   * @throws ResourceAlreadyExistException if a resource with the same
   * identifier already exists.
   */
  DataResource create(DataResource resource, String callerPrincipal) throws BadArgumentException, ResourceAlreadyExistException;

  /**
   * Basic find by example method. An implementation of this method is not
   * intended to imply any specific context or authentication information. It is
   * expected to use the provided information in order to create a query to the
   * data backend and to return appropriate results.
   *
   * The example is used to create a query to the data backend. It depends on
   * the implementation which fields of the example are evaluated, at least
   * simple fields should be evaluated. Resources in state REVOKED may or may
   * not be included depending on the 'icludeRevoked' flag.
   *
   * The result can be requested in a paginated form using the pgbl argument.
   *
   * @param example The example resource used to build the query for assigned
   * values.
   * @param pgbl The pageable object containing pagination information.
   * @param includeRevoked If TRUE, resources in state 'REVOKED' are included,
   * otherwise they are ignored. Typically, only privileged users should see
   * revoked resources.
   *
   * @return A page object containing all matching resources on the current
   * page. The list of results might be empty, but the result should NOT be
   * 'null'.
   */
  Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean includeRevoked);

  /**
   * Basic find by example method supporting permission check. An implementation
   * of this method is not intended to imply any specific context or
   * authentication information. It is expected to use the provided information
   * in order to create a query to the data backend and to return appropriate
   * results.
   *
   * The example is used to create a query to the data backend. It depends on
   * the implementation which fields of the example are evaluated, at least
   * simple fields should be evaluated. In addition, the result can be filtered
   * by permission for the provided list of sids and resources in state REVOKED
   * may or may not be included depending on the 'icludeRevoked' flag.
   *
   * The result can be requested in a paginated form using the pgbl argument.
   *
   * @param example The example resource used to build the query for assigned
   * values.
   * @param sids A list of subject ids identifying caller. This list may or may
   * not contain multiple entries identifying either a user or a group of users.
   * @param permission The minimum permission at least one subject in the list
   * of sids must possess.
   * @param pgbl The pageable object containing pagination information.
   * @param includeRevoked If TRUE, resources in state 'REVOKED' are included,
   * otherwise they are ignored. Typically, only privileged users should see
   * revoked resources.
   *
   * @return A page object containing all matching resources on the current
   * page. The list of results might be empty, but the result should NOT be
   * 'null'.
   */
  Page<DataResource> findAllFiltered(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean includeRevoked);

  /**
   * Find a data resource by the provided example. The example is used to create
   * a query to the data backend. It depends on the implementation which fields
   * of the example are evaluated, at least simple fields should be evaluated.
   * The result as list containing matching elements according to the provided
   * pagination information.
   *
   * If no element is matching the provided example, an empty list should be
   * returned. This method is NOT expected to return 'null'.
   *
   * This method is intended toprovide a high level wrapper for {@link #findAll(edu.kit.datamanager.repo.domain.DataResource, org.springframework.data.domain.Pageable, boolean, java.util.function.BiConsumer)
   * } and {@link #findAllFiltered(edu.kit.datamanager.repo.domain.DataResource, java.util.List, edu.kit.datamanager.entities.PERMISSION, org.springframework.data.domain.Pageable, boolean, java.util.function.BiConsumer)
   * } and may use them internally. In addition it may perform security checks
   * in order to determine, which of the two findAll implementations should be
   * called, e.g. for privileged or unprivileged access.
   *
   * @param example The example resource used to build the query for assigned
   * values.
   * @param pgbl The pageable object containing pagination information.
   * @param callerIdentities A list of caller identities, e.g. principal and
   * active group name.
   * @param callerIsAdministrator If TRUE, the caller was checked for role
   * ADMINISTRATOR and will receive resource access w/o ACL check. Otherwise,
   * the provided identities are used for ACL check.
   *
   * @return A page of data resources matching the example or an empty page.
   */
  Page<DataResource> findByExample(DataResource example, List<String> callerIdentities, boolean callerIsAdministrator, Pageable pgbl);
}
