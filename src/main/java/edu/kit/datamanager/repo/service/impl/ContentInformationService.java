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
import edu.kit.datamanager.repo.dao.ContentInformationMatchSpecification;
import edu.kit.datamanager.repo.dao.ContentInformationTagSpecification;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.service.IContentInformationService;
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
public class ContentInformationService implements IContentInformationService{

  @Autowired
  private IContentInformationDao dao;

  @PersistenceContext
  private EntityManager em;

  @Override
  @Transactional(readOnly = true)
  public Optional<ContentInformation> findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(Long id, String relativePath, String tag){

    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(id, relativePath, true));//new ByExampleSpecification(em).byExample(example));
    if(tag != null){
      spec = ContentInformationTagSpecification.andIfPermission(spec, tag);
    }

    return dao.findOne(spec);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContentInformation> findByParentResourceIdEqualsAndRelativePathLikeAndHasTag(Long id, String relativePath, String tag, Pageable pgbl){
    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(id, relativePath, false));
    if(tag != null){
      spec = ContentInformationTagSpecification.andIfPermission(spec, tag);
    }
    return dao.findAll(spec, pgbl);
  }

//  @Override
//  @Transactional(readOnly = true)
//  public Page<ContentInformation> findAll(ContentInformation example, Pageable pgbl){
//    if(example != null){
//      Specification<ContentInformation> spec = Specification.where(new ByExampleSpecification(em).byExample(example));
//      return getDao().findAll(spec, pgbl);
//    }
//    return getDao().findAll(pgbl);
//  }

  @Override
  public ContentInformation createOrUpdate(ContentInformation entity){
    return getDao().save(entity);
  }

  @Override
  public void delete(ContentInformation entity){
    getDao().delete(entity);
  }

  protected IContentInformationDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    return Health.up().withDetail("ContentInformation", dao.count()).build();
  }
}
