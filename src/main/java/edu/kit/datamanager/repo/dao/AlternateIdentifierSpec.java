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

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.repo.domain.DataResource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.datacite.schema.kernel_4.Resource.AlternateIdentifiers.AlternateIdentifier;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class AlternateIdentifierSpec{

  public static Specification<DataResource> andIfPermission(Specification<DataResource> specifications, final String primaryIdentifier){
    specifications = specifications.and(toSpecification(primaryIdentifier));
    return specifications;
  }

  public static Specification<DataResource> toSpecification(final String primaryIdentifier){

    return (Root<DataResource> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      Join<DataResource, AlternateIdentifier> altJoin = root.join("alternateIdentifiers", JoinType.INNER);
      return builder.and(builder.notEqual(altJoin.get("identifierType"), Identifier.IDENTIFIER_TYPE.INTERNAL), altJoin.get("value").in(primaryIdentifier));
    };
  }
}
