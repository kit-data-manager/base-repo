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

import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.DataResource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class CreatorSpecification{

  private static final Logger LOGGER = LoggerFactory.getLogger(CreatorSpecification.class);

  /**
   * Hidden constructor.
   */
  private CreatorSpecification(){
  }

  public static Specification<DataResource> toSpecification(final Set<Agent> creators){
    Specification<DataResource> newSpec = Specification.where(null);

    if(creators == null || creators.isEmpty()){
      LOGGER.trace("No creators found in example. Returning empty specification.");
      return newSpec;
    }

    LOGGER.trace("Including {} creator(s) from example.", creators.size());
    return (Root<DataResource> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);
      Specification<DataResource> creatorSpec = Specification.where(null);
      Join<DataResource, Agent> altJoin = root.join("creators", JoinType.INNER);
      List<Predicate> creatorPredicates = new ArrayList<>();

      creators.stream().map((creator) -> {
        LOGGER.trace("Adding new creator predicate.");
        return creator;
      }).forEachOrdered((creator) -> {
        if(creator.getGivenName() != null || creator.getFamilyName() != null || creator.getAffiliations() != null){
          List<Predicate> predicates = new ArrayList<>();
          if(creator.getFamilyName() != null){
            LOGGER.trace("Adding familyName predicate with value {}.", creator.getFamilyName());
            predicates.add(builder.like(altJoin.get("familyName"), "%" + creator.getFamilyName() + "%"));
          }
          if(creator.getGivenName() != null){
            LOGGER.trace("Adding givenName predicate with value {}.", creator.getGivenName());
            predicates.add(builder.like(altJoin.get("givenName"), "%" + creator.getGivenName() + "%"));
          }

          if(creator.getAffiliations() != null && !creator.getAffiliations().isEmpty()){
            LOGGER.trace("Adding affiliations predicate with value {}.", creator.getAffiliations());
            predicates.add(altJoin.join("affiliations").in(creator.getAffiliations()));
          }
          LOGGER.trace("Adding new creator predicate to list.");
          creatorPredicates.add(builder.and(predicates.toArray(new Predicate[]{})));
        } else{
          LOGGER.debug("Ignoring creator without given name, family name and affiliation.");
        }
      });
      LOGGER.trace("Returning OR connection of {} creator predicate(s).", creatorPredicates.size());
      return builder.or(creatorPredicates.toArray(new Predicate[]{}));
    };
  }
}
