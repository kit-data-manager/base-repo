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
package edu.kit.datamanager.repo.dao.spec;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 * @param <C> Generic type
 */
public class StringFieldSpecification<C>{

  public static <C> Specification<C> createSpecification(final String fieldName, String fieldValue, final boolean exactMatch){
    Specification<C> newSpec = Specification.where(null);
    if(fieldName == null || fieldValue == null){
      return newSpec;
    }

    return (Root<C> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      if(!exactMatch){
        return builder.like(root.get(fieldName), "%" + fieldValue + "%");
      } else{
        return builder.equal(root.get(fieldName), fieldValue);
      }
    };
  }

  public static <C> Specification<C> createSpecification(final String fieldName, String... fieldValues){
    Specification<C> newSpec = Specification.where(null);
    if(fieldName == null || fieldValues == null || fieldValues.length == 0){
      return newSpec;
    }

    return (Root<C> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      return builder.and(root.get(fieldName).in((String[]) fieldValues));
    };
  }
}
