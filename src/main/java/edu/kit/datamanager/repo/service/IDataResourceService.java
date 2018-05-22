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
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
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

  public Page<DataResource> findAll(DataResource example, List<String> sids, AclEntry.PERMISSION permission, Pageable pgbl, boolean IncludeRevoked);

  public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean IncludeRevoked);

  DataResource createOrUpdate(final DataResource entity);

  Optional<DataResource> findById(final Long id);

  void delete(final DataResource entity);
}
