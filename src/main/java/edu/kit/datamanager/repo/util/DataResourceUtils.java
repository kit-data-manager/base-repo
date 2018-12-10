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
package edu.kit.datamanager.repo.util;

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoServiceRole;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jejkal
 */
public class DataResourceUtils{

  private static final Logger LOGGER = LoggerFactory.getLogger(DataResourceUtils.class);

  private DataResourceUtils(){
  }

  public static String getInternalIdentifier(DataResource resource){
    for(Identifier alt : resource.getAlternateIdentifiers()){
      if(Identifier.IDENTIFIER_TYPE.INTERNAL.equals(alt.getIdentifierType())){
        return alt.getValue();
      }
    }
    return null;
  }

  /**
   * Check for sufficient permissions to access the provided resource with the
   * provided required permission. Permission evaluation is done in three steps:
   *
   * At first, access permissions are obtained via {@link #getAccessPermission(edu.kit.datamanager.repo.domain.DataResource)
   * }.
   *
   * The second step depends on the state of the resource. If the resource is
   * FIXED and WRITE permissions are requested, the caller permission must be
   * ADMINISTRATE, which is the case for the owner and administrators.
   * Otherwise, write access is forbidden. The same applies if the resource if
   * REVOKED. In that case, for all access types (READ, WRITE, ADMINISTRATE) the
   * caller must have ADMINISTRATE permissions.
   *
   * In a final step it is checked, if the caller permission is matching at
   * least the requested permission. If this is the case, this method will
   * return silently.
   *
   * In all other cases where requirements are not met, an
   * AccessForbiddenException or ResourceNotFoundException will be thrown.
   *
   *
   * @param resource The resource to check.
   * @param requiredPermission The required permission to access the resource.
   *
   * @throws AccessForbiddenException if the caller has not the required
   * permissions.
   * @throws ResourceNotFoundException if the resource has been revoked and the
   * caller has no ADMINISTRATE permissions.
   */
  public static void performPermissionCheck(DataResource resource, PERMISSION requiredPermission) throws AccessForbiddenException, ResourceNotFoundException{
    LOGGER.debug("Performing permission check for resource {} and permission {}.", "DataResource#" + resource.getId(), requiredPermission);
    PERMISSION callerPermission = getAccessPermission(resource);

    LOGGER.debug("Obtained caller permission {}. Checking resource state for special handling.", callerPermission);
    if(resource.getState() != null){
      switch(resource.getState()){
        case FIXED:
          LOGGER.debug("Performing special access check for FIXED resource and {} permission.", requiredPermission);
          //resource is fixed, only check if WRITE permissions are required
          if(requiredPermission.atLeast(PERMISSION.WRITE) && !callerPermission.atLeast(PERMISSION.ADMINISTRATE)){
            //no access, return 403 as resource has been revoked
            LOGGER.debug("{} permission to fixed resource NOT granted to principal with identifiers {}. ADMINISTRATE permissions required.", requiredPermission, AuthenticationHelper.getAuthorizationIdentities());
            throw new AccessForbiddenException("Resource has been fixed. Modifications to this resource are no longer permitted.");
          }
          break;
        case REVOKED:
          LOGGER.debug("Performing special access check for REVOKED resource and {} permission.", requiredPermission);
          //resource is revoked, check ADMINISTRATE or ADMINISTRATOR permissions
          if(!callerPermission.atLeast(PERMISSION.ADMINISTRATE)){
            //no access, return 404 as resource has been revoked
            LOGGER.debug("Access to revoked resource NOT granted to principal with identifiers {}. ADMINISTRATE permissions required.", requiredPermission, AuthenticationHelper.getAuthorizationIdentities());
            throw new ResourceNotFoundException("The resource never was or is not longer available.");
          }
          break;
        case VOLATILE:
          LOGGER.trace("Resource state is {}. No special access check necessary.", resource.getState());
          break;
        default:
          LOGGER.warn("Unhandled resource state {} detected. Not applying any special access checks.", resource.getState());
      }
    }

    LOGGER.debug("Checking if caller permission {} mets required permission {}.", callerPermission, requiredPermission);
    if(!callerPermission.atLeast(requiredPermission)){
      LOGGER.debug("Caller permission {} does not met required permission {}. Resource access NOT granted.", requiredPermission);
      throw new AccessForbiddenException("Resource access restricted by acl.");
    } else{
      LOGGER.debug("{} permission to resource granted to principal with identifiers {}.", requiredPermission, AuthenticationHelper.getAuthorizationIdentities());
    }

  }

  /**
   * Determine the maximum permission for the resource being accessed using the
   * internal authorization object. This method will go through all permissions
   * and principals to determine the maximum permission.
   *
   * @param resource The resource for which the permission should be determined.
   *
   * @return The maximum permission. PERMISSION.NONE is returned if no
   * permission was found.
   */
  public static PERMISSION getAccessPermission(DataResource resource){
    //quick check for admin permission
    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.getValue())){
      return PERMISSION.ADMINISTRATE;
    }

    //check for service roles
    PERMISSION servicePermission = PERMISSION.NONE;
    if(AuthenticationHelper.hasAuthority(RepoServiceRole.SERVICE_ADMINISTRATOR.getValue())){
      servicePermission = PERMISSION.ADMINISTRATE;
    } else if(AuthenticationHelper.hasAuthority(RepoServiceRole.SERVICE_WRITE.getValue())){
      servicePermission = PERMISSION.WRITE;
    } else if(AuthenticationHelper.hasAuthority(RepoServiceRole.SERVICE_READ.getValue())){
      servicePermission = PERMISSION.READ;
    }

    //quick check for temporary roles
    edu.kit.datamanager.entities.PERMISSION permission = AuthenticationHelper.getScopedPermission(DataResource.class.getSimpleName(), resource.getResourceIdentifier());
    if(permission.atLeast(edu.kit.datamanager.entities.PERMISSION.READ)){
      return permission;
    }

    List<String> principalIds = AuthenticationHelper.getAuthorizationIdentities();
    PERMISSION maxPermission = PERMISSION.NONE;
    for(AclEntry entry : resource.getAcls()){
      if(principalIds.contains(entry.getSid())){
        if(entry.getPermission().ordinal() > maxPermission.ordinal()){
          maxPermission = entry.getPermission();
        }
      }
    }

    //return service permission if higher, otherwise return maxPermission
    if(servicePermission.atLeast(maxPermission)){
      return servicePermission;
    }

    return maxPermission;
  }

  /**
   * Check if the internal authorization object has the provided permission.
   * This method will stop after a principal was found posessing the provided
   * permission.
   *
   * @param resource The resource for which the permission should be determined.
   * @param permission The permission to check for.
   *
   * @return TRUE if any sid has the requested permission or if the caller has
   * administrator permissions.
   */
  public static boolean hasPermission(DataResource resource, PERMISSION permission){
    return getAccessPermission(resource).atLeast(permission);
  }

  /**
   * Test if two acl lists are identical. This method returns FALSE if both
   * arrays have a different length or if at least one entry is different, e.g.
   * does not exist of has a different permission. Otherwise, TRUE is returned.
   * The implemented check is independent from the order of both arrays.
   *
   * @param first The first acl array, which is the reference.
   * @param second The second acl array.
   *
   * @return TRUE if all array elements of 'first' exist in 'second' with the
   * same PERMISSION, FALSE otherwise.
   */
  public static boolean areAclsEqual(@NonNull AclEntry[] first, @NonNull AclEntry[] second){

    if(first.length != second.length){
      //size differs, lists cannot be identical
      return false;
    }
    Map<String, PERMISSION> aclMapBefore = aclEntriesToMap(first);
    Map<String, PERMISSION> aclMapAfter = aclEntriesToMap(second);

    return aclMapBefore.entrySet().stream().noneMatch((entry) -> (!entry.getValue().equals(aclMapAfter.get(entry.getKey()))));
  }

  /**
   * Helper to create a map from an acl array.
   */
  private static Map<String, PERMISSION> aclEntriesToMap(AclEntry... entries){
    Map<String, PERMISSION> aclMap = new HashMap<>();
    for(AclEntry entry : entries){
      aclMap.put(entry.getSid(), entry.getPermission());
    }
    return aclMap;
  }

}
