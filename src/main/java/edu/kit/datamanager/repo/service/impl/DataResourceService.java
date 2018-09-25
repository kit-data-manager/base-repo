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
package edu.kit.datamanager.repo.service.impl;

import edu.kit.datamanager.dao.ByExampleSpecification;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.PermissionSpecification;
import edu.kit.datamanager.repo.dao.StateSpecification;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IDataResourceService;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author jejkal
 */
public class DataResourceService implements IDataResourceService{

  @Autowired
  private IDataResourceDao dao;

  @PersistenceContext
  private EntityManager em;

  public DataResourceService(){
    super();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DataResource> findById(final Long id){
    return getDao().findById(id);
  }

//  @Override
//  @Transactional(readOnly = true)
//  public List<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, AclEntry.PERMISSION permission){
//    return getDao().findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(state, sids, permission);
//  }
//
//  @Override
//  @Transactional(readOnly = true)
//  public Page<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, AclEntry.PERMISSION permission, Pageable pgbl){
//    return getDao().findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(state, sids, permission, pgbl);
//  }
//
//  @Override
//  @Transactional(readOnly = true)
//  public Optional<DataResource> findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(Long id, List<String> sids, AclEntry.PERMISSION permission){
//    return getDao().findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(id, sids, permission);
//  }
  @Override
  @Transactional(readOnly = true)
  public Page<DataResource> findAll(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean includeRevoked){
    Specification<DataResource> spec;
    if(example != null){
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission)).and(new ByExampleSpecification(em).byExample(example));
    } else{
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission));
    }
    if(!includeRevoked){
      spec = spec.and(StateSpecification.toSpecification());
    }
    return getDao().findAll(spec, pgbl);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean includeRevoked){
    Specification<DataResource> spec = null;
    if(example != null){
      spec = Specification.where(new ByExampleSpecification(em).byExample(example));
    }

    if(!includeRevoked){
      if(spec == null){
        spec = StateSpecification.toSpecification();
      } else{
        spec = spec.and(StateSpecification.toSpecification());
      }
    }

    return getDao().findAll(spec, pgbl);
  }

  @Override
  public DataResource createOrUpdate(DataResource entity){
    return getDao().save(entity);
  }

  @Override
  public void delete(DataResource entity){
    getDao().delete(entity);
  }

  protected IDataResourceDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    return Health.up().withDetail("DataResources", dao.count()).build();
  }
}
