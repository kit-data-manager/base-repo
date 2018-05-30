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
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

/**
 *
 * @author jejkal
 */
public class DataResourceUtils{

  private static final Logger LOGGER = LoggerFactory.getLogger(DataResourceUtils.class);

  public static String getInternalIdentifier(DataResource resource){
    for(Identifier alt : resource.getAlternateIdentifiers()){
      if(Identifier.IDENTIFIER_TYPE.INTERNAL.equals(alt.getIdentifierType())){
        return alt.getValue();
      }
    }
    return null;
  }

  public static void performPermissionCheck(DataResource resource, AclEntry.PERMISSION requiredPermission){
    LOGGER.debug("Performing permission check for {} permission to resource {}.", requiredPermission, resource);
    AclEntry.PERMISSION callerPermission = AclUtils.getPrincipalPermission(AuthenticationHelper.getPrincipalIdentifiers(), resource);

    switch(resource.getState()){
      case FIXED:
        LOGGER.debug("Performing special access check for FIXED resource and {} permission.", requiredPermission);
        //resource is fixed, only check if WRITE permissions are required
        if(requiredPermission.atLeast(AclEntry.PERMISSION.WRITE) && !callerPermission.atLeast(AclEntry.PERMISSION.ADMINISTRATE)){
          //no access, return 404 as resource has been revoked
          LOGGER.debug("{} permission to fixed resource NOT granted to principal with identifiers {}. ADMINISTRATE permissions missing.", requiredPermission, AuthenticationHelper.getPrincipalIdentifiers());
          throw new AccessForbiddenException("Resource has been fixed. Modifications to this resource are no longer permitted.");
        }
        break;
      case REVOKED:
        LOGGER.debug("Performing special access check for REVOKED resource.");
        //resource is revoked, check ADMINISTRATE or ADMINISTRATOR permissions
        if(!callerPermission.atLeast(AclEntry.PERMISSION.ADMINISTRATE)){
          //no access, return 404 as resource has been revoked
          LOGGER.debug("Access to revoked resource NOT granted to principal with identifiers {}. ADMINISTRATE permission missing.", requiredPermission, AuthenticationHelper.getPrincipalIdentifiers());
          throw new ResourceNotFoundException("The resource never was or is not longer available.");
        }
        break;
    }

    LOGGER.debug("Checking if caller permission {} are at least required permission {}.", callerPermission, requiredPermission);
    if(!callerPermission.atLeast(requiredPermission)){
      LOGGER.debug("Caller permission {} is not at least required permission {}. Resource access NOT granted.", requiredPermission);
      throw new AccessForbiddenException("Resource access restricted by acl.");
    } else{
      LOGGER.debug("{} permission to resource granted to principal with identifiers {}.", requiredPermission, AuthenticationHelper.getPrincipalIdentifiers());
    }

  }
}
