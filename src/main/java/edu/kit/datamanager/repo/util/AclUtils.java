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

import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.List;

/**
 *
 * @author jejkal
 */
public class AclUtils{

  /**
   * Determine the maximum permission for the resource using the list of
   * principalIds. This method will go through all permissions and principals to
   * determine the maximum permission.
   *
   * @param principalIds The list of principal IDs to check. Typically, this
   * list contains the username and the name of the group the user is currently
   * logged into.
   * @param resource The resource for which the permission should be determined.
   *
   * @return The maximum permission. PERMISSION.NONE is returned if no
   * permission was found.
   */
  public static AclEntry.PERMISSION getPrincipalPermission(List<String> principalIds, DataResource resource){
    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      //quick check for admin permission
      return AclEntry.PERMISSION.ADMINISTRATE;
    }

    AclEntry.PERMISSION maxPermission = AclEntry.PERMISSION.NONE;
    for(AclEntry entry : resource.getAcls()){
      if(principalIds.contains(entry.getSid())){
        if(entry.getPermission().ordinal() > maxPermission.ordinal()){
          maxPermission = entry.getPermission();
        }
      }
    }
    return maxPermission;
  }

  /**
   * Check the provided permission using the list of principal ids and the
   * resource. This method will stop after a principal was found posessing the
   * provided permission.
   *
   * @param principalIds The list of principal IDs to check. Typically, this
   * list contains the username and the name of the group the user is currently
   * logged into.
   * @param resource The resource for which the permission should be determined.
   * @param permission The permission to check for.
   *
   * @return TRUE if any sid has the requested permission or if the caller has
   * administrator permissions.
   */
  public static boolean hasPermission(List<String> principalIds, DataResource resource, AclEntry.PERMISSION permission){
    if(AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
      //quick check for admin permission
      return true;
    }

    boolean hasPermission = false;

    for(AclEntry entry : resource.getAcls()){
      if(principalIds.contains(entry.getSid())){
        hasPermission = entry.getPermission().atLeast(permission);
        if(hasPermission){
          //if have permission, break...else continue to try
          break;
        }
      }
    }
    return hasPermission;
  }
}
