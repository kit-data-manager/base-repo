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
package edu.kit.datamanager.repo.dao;

import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.DataResource.State;
import java.util.List;
import edu.kit.datamanager.entities.PERMISSION;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author jejkal
 */
public interface IDataResourceDao extends JpaRepository<DataResource, String>, JpaSpecificationExecutor<DataResource>{

  /**
   * Find all data resources NOT having the provided state, having at least one
   * of the provided sids in their ACL list with the provided permission.
   *
   * @param state The state the resource should NOT have.
   * @param sids A list of sids from which at least one sid must be in a
   * matching resource's ACL.
   * @param permission The permission a matching sid must be allowed to access
   * the resource with.
   *
   * @return A list of data resources or an empty list.
   */
  public List<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, PERMISSION permission);

  /**
   * Find data resources NOT having the provided state, having at least one of
   * the provided sids in their ACL list with the provided permission in a
   * paginated form.
   *
   * @param state The state the resource should NOT have.
   * @param sids A list of sids from which at least one sid must be in a
   * matching resource's ACL.
   * @param permission The permission a matching sid must be allowed to access
   * the resource with.
   *
   * @return One page of data resources or an empty page.
   */
  public Page<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, PERMISSION permission, Pageable pgbl);

  /**
   * Find one or no data resource with a provided id accessible with the
   * provided permissions by at least one of the sids in the provided list.
   *
   * @param id The resource id.
   * @param sids A list of sids from which at least one sid must be in a
   * matching resource's ACL.
   * @param permission The permission a matching sid must be allowed to access
   * the resource with.
   *
   * @return An optional resource or no resource if no resource is matching the
   * query.
   */
  public Optional<DataResource> findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(String id, List<String> sids, PERMISSION permission);

}
