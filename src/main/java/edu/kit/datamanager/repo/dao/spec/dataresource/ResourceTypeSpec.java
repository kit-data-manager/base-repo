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
package edu.kit.datamanager.repo.dao.spec.dataresource;

import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.datacite.schema.kernel_4.Resource;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class ResourceTypeSpec{

  /**
   * Hidden constructor.
   */
  private ResourceTypeSpec(){
  }

  public static Specification<DataResource> toSpecification(final ResourceType resourceType){
    Specification<DataResource> newSpec = Specification.where(null);
    if(resourceType == null || (resourceType.getTypeGeneral() == null && resourceType.getValue() == null)){
      return newSpec;
    }
    return (Root<DataResource> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      Join<DataResource, Resource.AlternateIdentifiers.AlternateIdentifier> altJoin = root.join("resourceType", JoinType.INNER);

      if(resourceType.getTypeGeneral() != null && resourceType.getValue() == null){
        return builder.equal(altJoin.get("typeGeneral"), resourceType.getTypeGeneral());
      } else if(resourceType.getTypeGeneral() == null && resourceType.getValue() != null){
        return builder.equal(altJoin.get("value"), resourceType.getValue());
      }

      //both are not null
      return builder.and(builder.equal(altJoin.get("typeGeneral"), resourceType.getTypeGeneral()), builder.like(altJoin.get("value"), "%" + resourceType.getValue() + "%"));
    };
  }
}
