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
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.service.IAuditService;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.javers.common.exception.JaversException;
import org.javers.common.exception.JaversExceptionCode;
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
public class ContentInformationAuditService implements IAuditService<ContentInformation>{

  @Autowired
  private Logger LOGGER;
  private final Javers javers;
  private final ApplicationProperties applicationProperties;

  @Autowired
  public ContentInformationAuditService(Javers javers, ApplicationProperties applicationProperties){
    this.javers = javers;
    this.applicationProperties = applicationProperties;
  }

  @Override
  public void captureAuditInformation(ContentInformation contentInformation, String principal){
    LOGGER.trace("Calling captureAuditInformation(ContentInformation#{}, {}).", contentInformation.getId(), principal);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Skipping registration of content information.");
    } else{
      LOGGER.trace("Capturing audit information for content information {} modified by principal {}.", contentInformation, principal);
      javers.commit(principal, contentInformation);
      LOGGER.trace("Successfully committed audit information for content information with id {}.", contentInformation.getId());
    }
  }

  @Override
  public Optional<String> getAuditInformationAsJson(String contentInformationId, int page, int resultsPerPage){
    LOGGER.trace("Calling getAuditInformationAsJson({}, {}, {}).", contentInformationId, page, resultsPerPage);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning empty result.");
      return Optional.empty();
    } else{
      JqlQuery query = QueryBuilder.byInstanceId(Long.parseLong(contentInformationId), ContentInformation.class).limit(resultsPerPage).skip(page * resultsPerPage).build();
      Changes result = javers.findChanges(query);

      LOGGER.trace("Obtained {} change elements. Returning them in serialized format.", result.size());
      return Optional.of(javers.getJsonConverter().toJson(result));
    }
  }

  @Override
  public Optional<ContentInformation> getResourceByVersion(String contentInformationId, long version){
    LOGGER.trace("Calling getResourceByVersion({}, {}).", contentInformationId, version);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning empty result.");
      return Optional.empty();
    } else{
      LOGGER.trace("Querying for content information with instance id {} and version {}.", contentInformationId, version);
      JqlQuery query = QueryBuilder.byInstanceId(Long.parseLong(contentInformationId), ContentInformation.class).withVersion(version).withShadowScope(ShadowScope.DEEP_PLUS).build();
      LOGGER.trace("Obtaining shadows from Javers repository.");
      List<Shadow<ContentInformation>> shadows = javers.findShadows(query);

      if(CollectionUtils.isEmpty(shadows)){
        LOGGER.warn("No version information found for content information id {}. Returning empty result.", contentInformationId);
        return Optional.empty();
      }

      LOGGER.trace("Shadow for content information id {} and version {} found. Returning result.", contentInformationId, version);
      Shadow<ContentInformation> versionShadow = shadows.get(0);
      LOGGER.trace("Returning shadow at index 0 with commit metadata {}.", versionShadow.getCommitMetadata());
      return Optional.of(versionShadow.get());
    }
  }

  @Override
  public long getCurrentVersion(String contentInformationId){
    LOGGER.trace("Calling getCurrentVersion({}).", contentInformationId);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning 0.");
      return 0l;
    } else{
      JqlQuery query = QueryBuilder.byInstanceId(Long.parseLong(contentInformationId), ContentInformation.class).limit(1).build();
      LOGGER.trace("Obtaining snapshots from Javers repository.");
      List<CdoSnapshot> snapshots = javers.findSnapshots(query);

      if(CollectionUtils.isEmpty(snapshots)){
        LOGGER.warn("No version information found for content information id {}. Returning 0.", contentInformationId);
        return 0;
      }

      long version = snapshots.get(0).getVersion();
      LOGGER.trace("Snapshot for content information id {} found. Returning version {}.", contentInformationId, version);
      return version;
    }
  }

  @Override
  public void deleteAuditInformation(String contentInformationId, ContentInformation resource){
    LOGGER.trace("Calling deleteAuditInformation({}, <contentInformation>).", contentInformationId);
    if(!applicationProperties.isAuditEnabled()){
      LOGGER.trace("Audit is disabled. Returning without doing anything.");
    } else{
      LOGGER.trace("Performing shallow delete of content information with id {}.", contentInformationId);
      try{
        javers.commitShallowDelete(contentInformationId, resource);
        LOGGER.trace("Shallow delete executed.");
      } catch(JaversException ex){
        if(JaversExceptionCode.CANT_DELETE_OBJECT_NOT_FOUND.equals(ex.getCode())){
          LOGGER.info("Unable to delete versioning information for content id " + contentInformationId + ". No versioning information found.");
        } else{
          LOGGER.error("Failed to delete versioning information. Please remove manually if needed.", ex);
        }

      }
    }
  }
}
