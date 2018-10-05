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
import com.monitorjbl.json.JsonResult;
import com.monitorjbl.json.JsonView;
import com.monitorjbl.json.Match;
import static com.monitorjbl.json.Match.match;
import edu.kit.datamanager.dao.ByExampleSpecification;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.EtagMismatchException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.PermissionSpecification;
import edu.kit.datamanager.repo.dao.StateSpecification;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.UnknownInformationConstants;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.PatchUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author jejkal
 */
public class DataResourceService implements IDataResourceService{

  private final JsonResult json = JsonResult.instance();

  @Autowired
  private IDataResourceDao dao;
  @Autowired
  private Logger logger;

  @PersistenceContext
  private EntityManager em;

  public DataResourceService(){
    super();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DataResource> findById(final Long id){
    return getDao().findById(id);
  }

//  @Override
//  @Transactional(readOnly = true)
//  public List<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, AclEntry.PERMISSION permission){
//    return getDao().findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(state, sids, permission);
//  }
//
//  @Override
//  @Transactional(readOnly = true)
//  public Page<DataResource> findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(State state, List<String> sids, AclEntry.PERMISSION permission, Pageable pgbl){
//    return getDao().findByStateNotAndAclsSidInAndAclsPermissionGreaterThanEqual(state, sids, permission, pgbl);
//  }
//
//  @Override
//  @Transactional(readOnly = true)
//  public Optional<DataResource> findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(Long id, List<String> sids, AclEntry.PERMISSION permission){
//    return getDao().findByIdAndAclsSidInAndAclsPermissionGreaterThanEqual(id, sids, permission);
//  }
  @Override
  public DataResource create(DataResource resource){
    if(resource.getIdentifier() == null){
      //set placeholder identifier
      resource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER));
      //check alternate identifiers for internal identifier
      boolean haveAlternateInternalIdentifier = false;
      for(Identifier alt : resource.getAlternateIdentifiers()){
        if(Identifier.IDENTIFIER_TYPE.INTERNAL.equals(alt.getIdentifierType())){
          if(alt.getValue() == null){
            throw new BadArgumentException("Provided internal identifier must not be null.");
          }
          resource.setResourceIdentifier(alt.getValue());
          haveAlternateInternalIdentifier = true;
          break;
        }
      }

      if(!haveAlternateInternalIdentifier){
        String altId = UUID.randomUUID().toString();
        logger.info("No primary identifier assigned to resource and no alternate identifier of type INTERNAL was found. Assigning alternate INTERNAL identifier {}.", altId);
        resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(altId));
        resource.setResourceIdentifier(altId);
      }
    } else{
      resource.setResourceIdentifier(resource.getIdentifier().getValue());
    }

    //check resource by identifier
    DataResource expl = new DataResource();
    expl.setResourceIdentifier(resource.getResourceIdentifier());

    Page<DataResource> res = findAll(expl, PageRequest.of(0, 1), true);

    if(res.hasContent()){
      throw new ResourceAlreadyExistException("There is already a resource with identifier " + resource.getResourceIdentifier());
    }

    //check mandatory datacite attributes
    if(resource.getCreators().isEmpty()){
      Agent creator = new Agent();
      creator.setGivenName(AuthenticationHelper.getFirstname());
      creator.setFamilyName(AuthenticationHelper.getLastname());
      resource.getCreators().add(creator);
    }

    if(resource.getTitles().isEmpty()){
      throw new BadArgumentException("No title assigned to provided document.");
    }

    if(resource.getResourceType() == null){
      throw new BadArgumentException("No resource type assigned to provided document.");
    }

    String caller = (String) AuthenticationHelper.getAuthentication().getPrincipal();

    //set auto-generateable fields
    if(resource.getPublisher() == null){
      resource.setPublisher(caller);
    }

    if(resource.getPublicationYear() == null){
      resource.setPublicationYear(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    }

    //check ACLs for caller
    AclEntry callerEntry = null;
    for(AclEntry entry : resource.getAcls()){
      if(caller.equals(entry.getSid())){
        callerEntry = entry;
        break;
      }
    }

    if(callerEntry == null){
      callerEntry = new AclEntry(caller, PERMISSION.ADMINISTRATE);
      resource.getAcls().add(callerEntry);
    } else{
      //make sure at least the caller has administrate permissions
      callerEntry.setPermission(PERMISSION.ADMINISTRATE);
    }

    boolean haveCreationDate = false;
    for(edu.kit.datamanager.repo.domain.Date d : resource.getDates()){
      if(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())){
        haveCreationDate = true;
        break;
      }
    }

    if(!haveCreationDate){
      logger.trace("Resource has no creation date. Setting current date.");
      edu.kit.datamanager.repo.domain.Date creationDate = new edu.kit.datamanager.repo.domain.Date();
      creationDate.setType(edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED);
      creationDate.setValue(Instant.now());
      resource.getDates().add(creationDate);
    }

    if(Objects.isNull(resource.getState())){
      logger.trace("Setting initial resource state to VOLATILE.");
      resource.setState(DataResource.State.VOLATILE);
    }

    return dao.save(resource);
  }

  @Override
  public DataResource getById(Long id, PERMISSION requestedPermission) throws ResourceNotFoundException, AccessForbiddenException{
    Optional<DataResource> result = findById(id);
    if(!result.isPresent()){
      logger.debug("No data resource found for identifier {}. Returning HTTP 404.", id);
      throw new ResourceNotFoundException("Data resource with id " + id + " was not found.");
    }
    DataResource resource = result.get();

    DataResourceUtils.performPermissionCheck(resource, requestedPermission);

    return resource;
  }

  @Override
  public List<DataResource> findByExample(DataResource example, PageRequest request, BiConsumer<Integer, Integer> linkEventTrigger){
    Page<DataResource> page;
    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      //do find all
      logger.debug("Administrator access detected. Calling findAll() with example {} and page request {}.", example, request);
      page = findAll(example, request, true);
    } else{
      //query based on membership
      logger.debug("Non-Administrator access detected. Calling findAll() with READ permissions, example {}, principal identifiers {} and page request {}.", example, AuthenticationHelper.getAuthorizationIdentities(), request);
      page = findAll(example, AuthenticationHelper.getAuthorizationIdentities(), PERMISSION.READ, request, false);
    }

    if(request.getPageNumber() > page.getTotalPages()){
      logger.debug("Requested page number {} is too large. Number of pages is: {}. Returning empty list.", request.getPageNumber(), page.getTotalPages());
    }

    linkEventTrigger.accept(page.getNumber(), page.getTotalPages());

    List<DataResource> modResources = page.getContent();
    if(modResources.isEmpty()){
      logger.debug("No data resource found for example {} and principal identifiers {}. Returning empty result.", example, AuthenticationHelper.getAuthorizationIdentities());
    }
    return modResources;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DataResource> findAll(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean includeRevoked){
    Specification<DataResource> spec;
    if(example != null){
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission)).and(new ByExampleSpecification(em).byExample(example));
    } else{
      spec = Specification.where(PermissionSpecification.toSpecification(sids, permission));
    }
    if(!includeRevoked){
      spec = spec.and(StateSpecification.toSpecification());
    }
    return getDao().findAll(spec, pgbl);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean includeRevoked){
    Specification<DataResource> spec = null;
    if(example != null){
      spec = Specification.where(new ByExampleSpecification(em).byExample(example));
    }

    if(!includeRevoked){
      if(spec == null){
        spec = StateSpecification.toSpecification();
      } else{
        spec = spec.and(StateSpecification.toSpecification());
      }
    }

    return getDao().findAll(spec, pgbl);
  }

  @Override
  public void patch(Long id, Predicate<String> etagChecker, JsonPatch patch){
    Optional<DataResource> result = findById(id);
    if(!result.isPresent()){
      logger.debug("No data resource found for identifier {}. Returning HTTP 404.", id);
      throw new ResourceNotFoundException("Data resource with id " + id + " was not found.");
    }

    DataResource resource = result.get();

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.WRITE);

    if(!etagChecker.test(Integer.toString(resource.hashCode()))){
      logger.debug("Provided etag is not matching resource etag {}. Returning HTTP 412.", Integer.toString(resource.hashCode()));
      throw new EtagMismatchException("ETag not matching, resource has changed.");
    }

    PERMISSION callerPermission = DataResourceUtils.getAccessPermission(resource);
    boolean callerIsAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());

    Collection<GrantedAuthority> userGrants = new ArrayList<>();
    userGrants.add(new SimpleGrantedAuthority(callerPermission.getValue()));

    if(callerIsAdmin){
      logger.debug("Administrator access detected. Adding ADMINISTRATOR role to granted authorities.");
      userGrants.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.getValue()));
    }
    DataResource updated = PatchUtil.applyPatch(resource, patch, DataResource.class, userGrants);

    logger.info("Persisting patched resource.");
    dao.save(updated);
    logger.info("Resource successfully persisted.");
  }

  @Override
  public void delete(Long id, Predicate<String> etagChecker){
    Optional<DataResource> result = findById(id);
    if(result.isPresent()){
      DataResource resource = result.get();
      if(DataResourceUtils.hasPermission(resource, PERMISSION.ADMINISTRATE) || AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
        if(!etagChecker.test(Integer.toString(resource.hashCode()))){
          logger.debug("Provided etag is not matching resource etag {}. Returning HTTP 412.", Integer.toString(resource.hashCode()));
          throw new EtagMismatchException("ETag not matching, resource has changed.");
        }
        logger.debug("Setting resource state to REVOKED.");
        resource.setState(DataResource.State.REVOKED);
        logger.debug("Persisting revoked resource.");
        dao.save(resource);
      } else{
        throw new UpdateForbiddenException("Insufficient role. ADMINISTRATE permission or ROLE_ADMINISTRATOR required.");
      }
    } else{
      logger.debug("No data resource found for identifier {}. Returning HTTP 204.", id);
    }
  }

  protected IDataResourceDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    return Health.up().withDetail("DataResources", dao.count()).build();
  }
}
