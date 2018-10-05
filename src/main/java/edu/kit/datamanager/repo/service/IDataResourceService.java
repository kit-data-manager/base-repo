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

import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.EtagMismatchException;
import edu.kit.datamanager.exceptions.PatchApplicationException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author jejkal
 */
public interface IDataResourceService extends HealthIndicator{

//  public List<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(DataResource.State state, List<String> sids, AclEntry.PERMISSION permission);
//
//  public Page<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(DataResource.State state, List<String> sids, AclEntry.PERMISSION permission, Pageable pgbl);
//
//  public Optional<DataResource> findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(Long id, List<String> sids, AclEntry.PERMISSION permission);
  public Page<DataResource> findAll(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean IncludeRevoked);

  public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean IncludeRevoked);

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
   *
   * @return The new resource with an id assigned.
   *
   * @throws BadArgumentException if the value of an provided internal
   * identifier is null or if no title and/or resource type are provided.
   * @throws ResourceAlreadyExistException if a resource with the same
   * identifier already exists.
   */
  DataResource create(DataResource resource) throws BadArgumentException, ResourceAlreadyExistException;

  /**
   * Apply the provided patch to the resource with the provided id. Before the
   * patch is applied, it should be checked if the resource exists and is
   * writable by the caller. Furthermore, the provided etagChecker should be
   * used for testing the current ETag of the resource against the ETag provided
   * by the caller. Afterwards, the patch should be applied if possible and the
   * resource should be persisted.
   *
   * @param id The numeric id of the resource to patch.
   * @param etagChecker The ETag checker used to check the current ETag of the
   * resource with the user-provided one.
   * @param patch The JsonPatch to apply.
   *
   * @throws ResourceNotFoundException if no resource exists for the provided id
   * or if the resource has been revoked and the caller has no ADMINISTRATE
   * permissions.
   * @throws EtagMismatchException if the user-provided ETag does not match the
   * current resource's state.
   * @throws AccessForbiddenException if the caller is not allowed to modify the
   * resource, e.g. if the resource is fixed and the caller has no ADMINISTRATE
   * permissions.
   * @throws PatchApplicationException if the patch cannot be applied for
   * unknown reasons.
   * @throws UpdateForbiddenException if the patch cannot be applied because a
   * field is affected that is not allowed to be changed, e.g. the numeric id.
   */
  void patch(Long id, Predicate<String> etagChecker, JsonPatch patch) throws ResourceNotFoundException, EtagMismatchException, AccessForbiddenException, PatchApplicationException, UpdateForbiddenException;

  /**
   * Delete the resource with the provided id. At first, it should be checked if
   * a resource for the provided id exists. If this is not the case, the method
   * should return silently without error. If a resource exists but the caller
   * has no ADMINISTRATE permissions or possesses the role ADMINISTRATOR, the
   * method should raise an UpdateForbiddenException. If the resource exists and
   * the user has sufficient permissions, the provided etagChecker should be
   * used for testing the current ETag of the resource against the ETag provided
   * by the caller. If both ETags are matching, the resource should be update to
   * state REVOKED instead of physical deletion.
   *
   * @param id The numeric id of the resource to patch.
   * @param etagChecker The ETag checker used to check the current ETag of the
   * resource with the user-provided one.
   *
   * @throws EtagMismatchException if the user-provided ETag does not match the
   * current resource's state.
   * @throws UpdateForbiddenException if the caller has no ADMINISTRATE
   * permissions for the resource and does not possess the role ADMINISTRATOR.
   */
  void delete(Long id, Predicate<String> etagChecker) throws EtagMismatchException, UpdateForbiddenException;

  /**
   * Find a resource by its numeric id. This method is meant to provide a plain
   * query to the underlying DAO implementation. This means, as long as the data
   * backend is available, this method should not produce any exception. The
   * result is an Optional of type DataResource. Before using, it should be
   * tested whether a value is present or not. Furthermore, particular access
   * checks should be done before delivering the result to a client.
   *
   * @param id The id of the resource.
   *
   * @return An optional holding the data resource with the provided id or which
   * has no value.
   */
  Optional<DataResource> findById(final Long id);

  /**
   * Get a resource by its numeric id. In contrast to {@link #findById(java.lang.Long)
   * } this method may throw exceptions if a resource with the provided id was
   * not found or if the caller has insufficient permissions. Basically, this
   * method must only return if an accessible resource for the provided id was
   * found and is accessible. However, this also means that this method is NOT
   * expected to return 'null'.
   *
   * @param id The id of the resource.
   * @param requestedPermission The permissions needed for resource access, e.g.
   * READ or WRITE.
   *
   * @return The resource with the provided id, if one exists.
   *
   * @throws ResourceNotFoundException if no resource with the provided id could
   * be found or if the resource has been revoked and the caller has not
   * ADMINISTRATE permissions.
   * @throws AccessForbiddenException if the caller has insufficient
   * permissions, e.g. if WRITE access was requested but the caller only has
   * READ access or if WRITE access was requested, but the resource is fixed and
   * the caller has no ADMINISTRATE permissions.
   */
  DataResource getById(Long id, PERMISSION requestedPermission) throws ResourceNotFoundException, AccessForbiddenException;

  /**
   */
  List<DataResource> findByExample(DataResource example, PageRequest request, BiConsumer<Integer, Integer> linkEventTrigger);

}
