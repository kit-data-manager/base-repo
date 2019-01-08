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

import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.dao.ByExampleSpecification;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.messaging.DataResourceMessage;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.dao.AlternateIdentifierSpec;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.InternalIdentifierSpec;
import edu.kit.datamanager.repo.dao.PermissionSpecification;
import edu.kit.datamanager.repo.dao.PrimaryIdentifierSpec;
import edu.kit.datamanager.repo.dao.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.StateSpecification;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.UnknownInformationConstants;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import edu.kit.datamanager.util.PatchUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for the IDataResourceService interface.
 *
 * @author jejkal
 */
public class DataResourceService implements IDataResourceService{

  @Autowired
  private IDataResourceDao dao;
  @Autowired
  private Logger logger;

  @Autowired
  private IMessagingService messagingService;

  @PersistenceContext
  private EntityManager em;

  /**
   * Default constructor.
   */
  public DataResourceService(){
    super();
  }

  @Override
  @Transactional(readOnly = false)
  public DataResource create(DataResource resource, String callerPrincipal){
    return create(resource, callerPrincipal, null, null);
  }

  @Override
  @Transactional(readOnly = false)
  public DataResource create(DataResource resource, String callerPrincipal, String callerFirstName, String callerLastName){
    logger.trace("Performing create({}, {}, {}, {}).", resource, callerPrincipal, callerFirstName, callerLastName);

    //check for provided DOI
    boolean hasDoi = true;
    if(resource.getIdentifier() == null){
      hasDoi = false;
    } else{
      for(UnknownInformationConstants constant : UnknownInformationConstants.values()){
        if(constant.getValue().equals(resource.getIdentifier().getValue())){
          hasDoi = false;
          break;
        }
      }
    }

    if(!hasDoi){
      logger.debug("No primary identifier assigned to resource. Using placeholder '{}'.", UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER);
      //set placeholder identifier

      //resource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER));
      resource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER.getValue()));
      //check alternate identifiers for internal identifier
      boolean haveAlternateInternalIdentifier = false;
      for(Identifier alt : resource.getAlternateIdentifiers()){
        if(Identifier.IDENTIFIER_TYPE.INTERNAL.equals(alt.getIdentifierType())){
          if(alt.getValue() == null){
            logger.error("Found alternate identifier of type INTERNAL with value 'null'. Throwing BadArgumentException.");
            throw new BadArgumentException("Provided internal identifier must not be null.");
          }
          logger.debug("Setting resource identifier to provided internal identifier with value {}.", alt.getValue());
          resource.setId(alt.getValue());
          haveAlternateInternalIdentifier = true;
          break;
        }
      }

      if(!haveAlternateInternalIdentifier){
        String altId = UUID.randomUUID().toString();
        logger.debug("No primary identifier assigned to resource and no alternate identifier of type INTERNAL was found. Assigning alternate INTERNAL identifier {}.", altId);
        resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(altId));
        resource.setId(altId);
      }
    } else{
      logger.debug("Primary identifier found. Setting resource identifier to primary identifier {}.", resource.getIdentifier().getValue());
      resource.setId(resource.getIdentifier().getValue());
    }
    logger.trace("Checking for existing resource with identifier {}.", resource.getId());
    //check resource by identifier
    long cnt = getDao().count(PrimaryIdentifierSpec.toSpecification(resource.getId()).or(AlternateIdentifierSpec.toSpecification(resource.getId()).or(InternalIdentifierSpec.toSpecification(resource.getId()))));
    logger.trace("Found {} existing resources conflicting with provided identifier {}.", cnt, resource.getId());
    if(cnt != 0){
      logger.error("Number of conflicting identifiers with identifier {} is neq 0. Throwing ResourceAlreadyExistException.", resource.getId());
      throw new ResourceAlreadyExistException("There is already a resource with identifier " + resource.getId());
    }
    logger.trace("Checking for mandatory element 'titles'.");
    if(resource.getTitles().isEmpty()){
      logger.error("No titles found. Throwing BadArgumentException.");
      throw new BadArgumentException("No title assigned to provided document.");
    }

    logger.trace("Checking for mandatory element 'resourceType'.");
    if(resource.getResourceType() == null){
      logger.error("No resource type provided found. Throwing BadArgumentException.");
      throw new BadArgumentException("No resource type assigned to provided document.");
    }

    logger.trace("Checking for mandatory element 'creators'.");
    //check mandatory datacite attributes
    if(resource.getCreators().isEmpty()){
      logger.trace("No creators found. Adding creator based on authentication context.");

      Agent creator = new Agent();
      if(callerFirstName == null && callerLastName == null){
        logger.trace("Both, first and last name of authentication context are 'null'. Using caller principal '{}' as first name.", callerPrincipal);
        creator.setGivenName(callerPrincipal);
        creator.setFamilyName(null);
      } else{
        logger.trace("Setting firstname {} and lastname {} as caller.", callerFirstName, callerLastName);
        creator.setGivenName(callerFirstName);
        creator.setFamilyName(callerLastName);
      }
      logger.debug("Adding new creator {} to resource.", creator);
      resource.getCreators().add(creator);
    }

    logger.trace("Checking for mandatory element 'publisher'.");
    //set auto-generateable fields
    if(resource.getPublisher() == null){
      logger.debug("Setting caller principal {} as publisher.", callerPrincipal);
      resource.setPublisher(callerPrincipal);
    }

    logger.trace("Checking for mandatory element 'publicationYear'.");
    if(resource.getPublicationYear() == null){
      String thisYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
      logger.debug("Setting current year {} as publicationYear.", thisYear);
      resource.setPublicationYear(thisYear);
    }

    logger.trace("Checking resource for caller acl entry.");
    //check ACLs for caller
    AclEntry callerEntry = null;
    for(AclEntry entry : resource.getAcls()){
      if(callerPrincipal.equals(entry.getSid())){
        logger.trace("Acl entry for caller {} found: {}", callerPrincipal, entry);
        callerEntry = entry;
        break;
      }
    }

    if(callerEntry == null){
      logger.debug("Adding caller entry with ADMINISTRATE permissions.");
      callerEntry = new AclEntry(callerPrincipal, PERMISSION.ADMINISTRATE);
      resource.getAcls().add(callerEntry);
    } else{
      logger.debug("Ensuring ADMINISTRATE permissions for acl entry {}.", callerEntry);
      //make sure at least the caller has administrate permissions
      callerEntry.setPermission(PERMISSION.ADMINISTRATE);
    }
    logger.trace("Checking for creation date.");
    boolean haveCreationDate = false;
    for(edu.kit.datamanager.repo.domain.Date d : resource.getDates()){
      if(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())){
        logger.trace("Creation date entry found.");
        haveCreationDate = true;
        break;
      }
    }

    if(!haveCreationDate){
      Instant now = Instant.now();
      logger.debug("Adding current date {} as creation date.", now);
      edu.kit.datamanager.repo.domain.Date creationDate = new edu.kit.datamanager.repo.domain.Date();
      creationDate.setType(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED);
      creationDate.setValue(now);
      resource.getDates().add(creationDate);
    }

    logger.trace("Checking resource state.");
    if(Objects.isNull(resource.getState())){
      logger.debug("Setting initial resource state to {}.", DataResource.State.VOLATILE);
      resource.setState(DataResource.State.VOLATILE);
    } else{
      logger.trace("Resource state found. State is: {}", resource.getState());
    }

    logger.trace("Persisting created resource.");
    resource = getDao().save(resource);

    logger.trace("Sending CREATE event.");
    messagingService.send(DataResourceMessage.factoryCreateMessage(resource.getId(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
    return resource;
  }

  @Override
  @Transactional(readOnly = true)
  public DataResource findById(final String id){
    logger.trace("Performing findById({}).", id);
    Optional<DataResource> result = getDao().findById(id);

    if(!result.isPresent()){
      logger.error("No data resource found for identifier {}. Throwing ResourceNotFoundException.", id);
      throw new ResourceNotFoundException("Data resource with id " + id + " was not found.");
    }

    return result.get();
  }

  @Override
  @Transactional(readOnly = true)
  public DataResource findByAnyIdentifier(final String resourceIdentifier){
    logger.trace("Performing findOne(resourceIdentifier == '{}').", resourceIdentifier);
    Optional<DataResource> result = getDao().findOne(InternalIdentifierSpec.toSpecification(resourceIdentifier));

    if(!result.isPresent()){
      logger.error("No data resource found for resource identifier {}. Checking primary and alternate identifiers.", resourceIdentifier);
      logger.trace("Performing findOne(primaryIdentifier == '{}' || alternateIdentifier == '{}').", resourceIdentifier, resourceIdentifier);
      try{
        result = getDao().findOne(AlternateIdentifierSpec.toSpecification(resourceIdentifier).or(PrimaryIdentifierSpec.toSpecification(resourceIdentifier)));
      } catch(IncorrectResultSizeDataAccessException ex){
        logger.error("!!!POTENTIAL INCONSISTENCY DETECTED!!! Multiple resources with primary/alternate identifier {} " + resourceIdentifier + " detected.");
        throw new CustomInternalServerError("Inconsistent state detected. The provided identifier is mapping to multiple resources.");
      }
      if(!result.isPresent()){
        throw new ResourceNotFoundException("Data resource with identifier " + resourceIdentifier + " was not found.");
      }
    }

    return result.get();
  }

  @Override
  public Page<DataResource> findByExample(DataResource example, List<String> callerIdentities, boolean callerIsAdministrator, Pageable pgbl){
    logger.trace("Performing findByExample({}, {}).", example, pgbl);
    Page<DataResource> page;
    if(callerIsAdministrator){
      //do find all
      logger.trace("Administrator access detected. Calling findAll({}, {}, {}).", example, pgbl, Boolean.TRUE);
      page = findAll(example, pgbl, true);
    } else{
      //query based on membership
      logger.trace("Non-Administrator access detected. Calling findAllFiltered({}, {}, {}, {}, {}).", example, callerIdentities, PERMISSION.READ, pgbl, Boolean.FALSE);
      page = findAllFiltered(example, callerIdentities, PERMISSION.READ, pgbl, false);
    }

    logger.trace("Returning page content.");
    return page;
  }

  @Override
  public Page<DataResource> findAllFiltered(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean includeRevoked){
    logger.trace("Performing findAllFiltered({}, {}, {}, {}, {}).", example, sids, permission, pgbl, includeRevoked);
    Specification<DataResource> spec;
    if(example != null){
      logger.trace("Adding permission specification and example specification to query.");
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission)).and(new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType())));
    } else{
      logger.trace("Adding permission specification to query.");
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission));
    }

    return doFind(spec, pgbl, includeRevoked);
  }

  @Override
  public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean pIncludeRevoked){
    logger.trace("Performing findAll({}, {}, {}).", example, pgbl, pIncludeRevoked);
    Specification<DataResource> spec = null;
    if(example != null){
      logger.trace("Adding example specification to query.");
      spec = Specification.where(new ByExampleSpecification(em).byExample(example).and(ResourceTypeSpec.toSpecification(example.getResourceType())));
    }

    return doFind(spec, pgbl, pIncludeRevoked);
  }

  @Override
  public Page<DataResource> findAll(DataResource resource, Pageable pgbl){
    logger.trace("Performing findAll({}).", "DataResource#" + resource.getId());
    return findAll(resource, pgbl, false);
  }

  /**
   * Private helper used by findAll and findAllFiltered.
   */
  @Transactional(readOnly = true)
  private Page<DataResource> doFind(Specification<DataResource> spec, Pageable pgbl, boolean includeRevoked){
    logger.trace("Performing doFind({}, {}, {}).", spec, pgbl, includeRevoked);
    if(!includeRevoked){
      logger.trace("Adding StateSpecification in order to exclude revoked resources.");
      if(spec == null){
        logger.trace("Specification is currently null. Setting specification to StateSpecification.");
        //spec is currently null, therefore only the StateSpec is used
        spec = StateSpecification.toSpecification();
      } else{
        logger.trace("Appending StateSpecification via AND operator.");
        //spec is not null, connect StateSpec by AND
        spec = spec.and(StateSpecification.toSpecification());
      }
    }
    logger.trace("Querying DAO implementation using final spec and pageable information {}.", pgbl);
    return getDao().findAll(spec, pgbl);
  }

  @Override
  @Transactional(readOnly = false)
  public void patch(DataResource resource, JsonPatch patch, Collection<? extends GrantedAuthority> userGrants){
    logger.trace("Performing patch({}, {}, {}).", "DataResource#" + resource.getId(), patch, userGrants);

    List<String> identifierListBefore = getUniqueIdentifiers(resource);
    logger.trace("Resource identifiers before patch: {}", identifierListBefore);

    DataResource updated = PatchUtil.applyPatch(resource, patch, DataResource.class, userGrants);
    List<String> identifierListAfter = getUniqueIdentifiers(updated);
    logger.trace("Resource identifiers after patch: {}", identifierListAfter);

    checkUniqueIdentifiers(identifierListBefore, identifierListAfter);

    //AclEntry[] acls_before = updated.getAcls().toArray(new AclEntry[]{});
    getDao().save(updated);
    // AclEntry[] acls_after = updated.getAcls().toArray(new AclEntry[]{});

    logger.trace("Sending UPDATE event.");
    messagingService.send(DataResourceMessage.factoryUpdateMessage(resource.getId(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
  }

  @Override
  @Transactional(readOnly = false)
  public DataResource put(DataResource resource, DataResource newResource, Collection<? extends GrantedAuthority> userGrants) throws UpdateForbiddenException{
    logger.trace("Performing put({}, {}, {}).", "DataResource#" + resource.getId(), "DataResource#" + newResource.getId(), userGrants);
    List<String> identifierListBefore = getUniqueIdentifiers(resource);
    logger.trace("Resource identifiers before update: {}", identifierListBefore);
    List<String> identifierListAfter = getUniqueIdentifiers(newResource);
    logger.trace("Resource identifiers after update: {}", identifierListAfter);

    checkUniqueIdentifiers(identifierListBefore, identifierListAfter);

    //check if any forbidden field has been updated
    //use PatchUtil as it does exactly this check
    if(!PatchUtil.canUpdate(resource, newResource, userGrants)){
      throw new UpdateForbiddenException("Update not applicable. At least one unmodifiable field has been changed.");
    }

    if(newResource.getAcls() == null || newResource.getAcls().isEmpty()){
      logger.warn("Empty ACL provided for update. Using former value.");
      newResource.setAcls(resource.getAcls());
    }

    if(newResource.getTitles() == null || newResource.getTitles().isEmpty()){
      logger.warn("Empty title list provided for update. Using former value.");
      newResource.setTitles(resource.getTitles());
    }
    if(newResource.getCreators() == null || newResource.getCreators().isEmpty()){
      logger.warn("Empty creators list provided for update. Using former value.");
      newResource.setCreators(resource.getCreators());
    }
    if(newResource.getPublicationYear() == null){
      logger.warn("Empty publication year provided for update. Using former value.");
      newResource.setPublicationYear(resource.getPublicationYear());
    }

    if(newResource.getPublisher() == null){
      logger.warn("Empty publisher provided for update. Using former value.");
      newResource.setPublisher(resource.getPublisher());
    }

    //AclEntry[] acls_before = updated.getAcls().toArray(new AclEntry[]{});
    DataResource result = getDao().save(newResource);
    // AclEntry[] acls_after = updated.getAcls().toArray(new AclEntry[]{});

    logger.trace("Sending UPDATE event.");
    messagingService.send(DataResourceMessage.factoryUpdateMessage(resource.getId(), AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
    return result;
  }

  /**
   * Get all identifiers of a resource that have to be unique, e.g. primary and
   * alternate identifiers.
   *
   * @param resource The resource.
   *
   * @return A list of identifiers.
   */
  private List<String> getUniqueIdentifiers(DataResource resource){
    List<String> identifiers = new ArrayList<>();
    if(resource.getIdentifier() != null){
      identifiers.add(resource.getIdentifier().getValue());
    }
    resource.getAlternateIdentifiers().forEach((alt) -> {
      identifiers.add(alt.getValue());
    });
    return identifiers;
  }

  /**
   * Check if there is a resource identified with one identifiers from list
   * 'after' not in 'before'. If at least one resource was found, a
   * ResourceAlreadyExistsException is throws avoiding any update of the
   * resource with the new list of identifiers.
   *
   * @param before The list of unique identifiers before an update.
   * @param after The list of unique identifiers after an update.
   */
  private void checkUniqueIdentifiers(List<String> before, List<String> after){
    logger.trace("Removing assigned identifiers {} from list of checked identifiers {}.", before, after);
    after.removeAll(before);
    logger.trace("Remaining new or updated resource identifiers: {}", after);

    if(after.isEmpty()){
      return;
    }
    String[] afterList = after.toArray(new String[]{});
    logger.trace("Checking for conflicting identifiers.");
    long cnt = getDao().count(PrimaryIdentifierSpec.toSpecification(afterList).or(AlternateIdentifierSpec.toSpecification(afterList).or(InternalIdentifierSpec.toSpecification(afterList))));
    logger.trace("Found {} resources conflicting with at least one identifier of: {}.", cnt, afterList);
    if(cnt > 0){
      logger.error("Number of conflicting identifiers with identifiers {} is neq 0. Throwing ResourceAlreadyExistException.", after);
      throw new ResourceAlreadyExistException("There is at least one resource with one of the following identifiers: " + after);
    }
    logger.trace("No identifier conflict detected. Persisting resource.");
  }

  @Override
  @Transactional(readOnly = false)
  public void delete(DataResource resource){
    logger.trace("Performing delete({}).", "DataResource#" + resource.getId());
    logger.debug("Setting resource state to {}.", DataResource.State.REVOKED);
    resource.setState(DataResource.State.REVOKED);
    logger.trace("Persisting revoked resource.");
    getDao().save(resource);
    logger.trace("Resource successfully persisted.");
  }

  protected IDataResourceDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    logger.trace("Obtaining health information.");
    return Health.up().withDetail("DataResources", getDao().count()).build();
  }

}
