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
import edu.kit.datamanager.exceptions.FeatureNotImplementedException;
import edu.kit.datamanager.repo.dao.spec.dataresource.AlternateIdentifierSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.CreatorSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.PermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.PrimaryIdentifierSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.commons.collections4.CollectionUtils;
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
      LOGGER.trace("Checking for unsupported fields in example resource.");
      checkForUnsupportedFields(example);

      LOGGER.trace("Creating permission and example specification.");
      if(result != null){
        result = result.and(new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType()))).and(CreatorSpecification.toSpecification(example.getCreators()));
        if(example.getIdentifier() != null && example.getIdentifier().getValue() != null){
          result = result.and(PrimaryIdentifierSpec.toSpecification(example.getIdentifier().getValue()));
        }

        if(example.getAlternateIdentifiers() != null){
          List<String> altIds = new ArrayList<>();
          example.getAlternateIdentifiers().stream().filter((id) -> (id.getValue() != null)).forEachOrdered((id) -> {
            altIds.add(id.getValue());
          });
          result = result.and(AlternateIdentifierSpec.toSpecification(altIds.toArray(new String[]{})));
        }

      } else{
        result = new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType())).and(CreatorSpecification.toSpecification(example.getCreators()));
      }
    }
    return result;
  }

  /**
   * Check for assigned attributes and if they are supported by search. If not,
   * throw a FeatureNotImplementedException.
   *
   * @param resource The resource to check.
   *
   * @throws FeatureNotImplementedException If at least one unsupported field is
   * assigned at 'resource'.
   */
  private static void checkForUnsupportedFields(DataResource resource) throws FeatureNotImplementedException{
    if(!CollectionUtils.isEmpty(resource.getAcls())
            || !CollectionUtils.isEmpty(resource.getContributors())
            || !CollectionUtils.isEmpty(resource.getDates())
            || !CollectionUtils.isEmpty(resource.getDescriptions())
            || !CollectionUtils.isEmpty(resource.getFormats())
            || !CollectionUtils.isEmpty(resource.getFundingReferences())
            || !CollectionUtils.isEmpty(resource.getGeoLocations())
            || !CollectionUtils.isEmpty(resource.getRelatedIdentifiers())
            || !CollectionUtils.isEmpty(resource.getRights())
            || !CollectionUtils.isEmpty(resource.getSizes())
            || !CollectionUtils.isEmpty(resource.getSubjects())){
      LOGGER.warn("Found unsupported field assigned in example {}. Throwing FeatureNotImplementedException exception.", resource);
      throw new FeatureNotImplementedException("Searching is not yet implemented for one of the provided resource attributes. "
              + "Currently, only support for identifiers, resourceType, creators and all primitive attributes is available.");
    }
  }
}
