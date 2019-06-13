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
package edu.kit.datamanager.repo.service.impl;

import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.service.IAuditService;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.repository.jql.ShadowScope;
import org.javers.shadow.Shadow;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author jejkal
 */
@Service
public class DataResourceAuditService implements IAuditService<DataResource>{

  @Autowired
  private Logger LOGGER;
  private final Javers javers;
  private final ApplicationProperties applicationProperties;

  @Autowired
  public DataResourceAuditService(Javers javers, ApplicationProperties applicationProperties){
    this.javers = javers;
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void captureAuditInformation(DataResource resource, String principal){
    LOGGER.trace("Calling captureAuditInformation(DataResource#{}, {}).", resource.getId(), principal);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Skipping registration of resource.");
    } else{
      LOGGER.trace("Capturing audit information for resource {} modified by principal {}.", resource, principal);
      javers.commit(principal, resource);
      LOGGER.trace("Successfully committed audit information for resource with id {}.", resource.getId());
    }
  }

  @Override
  public Optional<String> getAuditInformationAsJson(String resourceId, int page, int resultsPerPage){
    LOGGER.trace("Calling getAuditInformationAsJson({}, {}, {}).", resourceId, page, resultsPerPage);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning empty result.");
      return Optional.empty();
    } else{
      JqlQuery query = QueryBuilder.byInstanceId(resourceId, DataResource.class).limit(resultsPerPage).skip(page * resultsPerPage).build();
      Changes result = javers.findChanges(query);

      LOGGER.trace("Obtained {} change elements. Returning them in serialized format.", result.size());
      return Optional.of(javers.getJsonConverter().toJson(result));
    }
  }

  @Override
  public Optional<DataResource> getResourceByVersion(String resourceId, long version){
    LOGGER.trace("Calling getResourceByVersion({}, {}).", resourceId, version);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning empty result.");
      return Optional.empty();
    } else{
      JqlQuery query = QueryBuilder.byInstanceId(resourceId, DataResource.class).withVersion(version).withShadowScope(ShadowScope.DEEP_PLUS).build();
      LOGGER.trace("Obtaining shadows from Javers repository.");
      List<Shadow<DataResource>> shadows = javers.findShadows(query);

      if(CollectionUtils.isEmpty(shadows)){
        LOGGER.warn("No version information found for resource id {}. Returning empty result.", resourceId);
        return Optional.empty();
      }

      LOGGER.trace("Shadow for resource id {} and version {} found. Returning result.", resourceId, version);
      Shadow<DataResource> versionShadow = shadows.get(0);
      LOGGER.trace("Returning shadow at index 0 with commit metadata {}.", versionShadow.getCommitMetadata());
      return Optional.of(versionShadow.get());
    }
  }

  @Override
  public long getCurrentVersion(String resourceId){
    LOGGER.trace("Calling getCurrentVersion({}).", resourceId);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning 0.");
      return 0l;
    } else{
      JqlQuery query = QueryBuilder.byInstanceId(resourceId, DataResource.class).limit(1).build();
      LOGGER.trace("Obtaining snapshots from Javers repository.");
      List<CdoSnapshot> snapshots = javers.findSnapshots(query);

      if(CollectionUtils.isEmpty(snapshots)){
        LOGGER.warn("No version information found for resource id {}. Returning 0.", resourceId);
        return 0;
      }

      long version = snapshots.get(0).getVersion();
      LOGGER.trace("Snapshot for resource id {} found. Returning version {}.", resourceId, version);
      return version;
    }
  }

  @Override
  public void deleteAuditInformation(String resourceId, DataResource resource){
    LOGGER.trace("Calling deleteAuditInformation({}, <resource>).", resourceId);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning without doing anything.");
    } else{
      LOGGER.trace("Performing shallow delete of resource with id {}.", resourceId);
      javers.commitShallowDelete(resourceId, resource);
      LOGGER.trace("Shallow delete executed.");
    }
  }

}
