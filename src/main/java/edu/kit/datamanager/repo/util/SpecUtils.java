/*
 * Copyright 2019 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.util;

import edu.kit.datamanager.dao.ByExampleSpecification;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.dao.AlternateIdentifierSpec;
import edu.kit.datamanager.repo.dao.CreatorSpecification;
import edu.kit.datamanager.repo.dao.PermissionSpecification;
import edu.kit.datamanager.repo.dao.PrimaryIdentifierSpec;
import edu.kit.datamanager.repo.dao.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.StateSpecification;
import edu.kit.datamanager.repo.domain.DataResource;
import java.util.List;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class SpecUtils{

  private static final Logger LOGGER = LoggerFactory.getLogger(SpecUtils.class);

  public static Specification<DataResource> getByExampleSpec(DataResource example, EntityManager em, List<String> sids, PERMISSION permission){
    Specification<DataResource> result = null;

    if(sids != null && permission != null){
      LOGGER.trace("Creating permission specification.");
      result = Specification.where(PermissionSpecification.toSpecification(sids, permission));
    } else{
      LOGGER.trace("No permission information provided. Skip creating permission specification.");
    }

    if(example != null){
      LOGGER.trace("Creating permission and example specification.");
      if(result != null){
        result = result.and(new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType()))).and(CreatorSpecification.toSpecification(example.getCreators()));
      } else{
        result = new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType()));
      }
    }
    return result;
  }
}
