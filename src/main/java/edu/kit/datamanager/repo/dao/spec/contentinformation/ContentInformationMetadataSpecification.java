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
package edu.kit.datamanager.repo.dao.spec.contentinformation;

import edu.kit.datamanager.repo.domain.ContentInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class ContentInformationMetadataSpecification{

  /**
   * Hidden constructor.
   */
  private ContentInformationMetadataSpecification(){
  }

  public static Specification<ContentInformation> toSpecification(final Map<String, String> metadata){
    Specification<ContentInformation> newSpec = Specification.where(null);
    if(metadata == null || metadata.isEmpty()){
      return newSpec;
    }

    return (Root<ContentInformation> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      MapJoin<ContentInformation, String, String> orderMap = root.joinMap("metadata");

      List<Predicate> predicates = new ArrayList<>();

      metadata.entrySet().forEach((entry) -> {
        predicates.add(builder.and(builder.equal(orderMap.key(), entry.getKey()), builder.like(orderMap.value(), "%" + entry.getValue() + "%")));
      });

      return builder.or(predicates.toArray(new Predicate[]{}));
    };
  }
}
