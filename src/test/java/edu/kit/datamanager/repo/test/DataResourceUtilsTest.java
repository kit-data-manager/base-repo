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
package edu.kit.datamanager.repo.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoServiceRole;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.security.filter.JwtAuthenticationToken;
import edu.kit.datamanager.security.filter.ScopedPermission;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 *
 * @author jejkal
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthenticationHelper.class)
public class DataResourceUtilsTest{

  @Before
  public void setup(){
    PowerMockito.mockStatic(AuthenticationHelper.class);

    mockJwtUserAuthentication(RepoUserRole.USER);
  }

  @Test
  public void testGetInternalIdentifier(){
    DataResource res = DataResource.factoryNewDataResource();
    res.getAlternateIdentifiers().add(Identifier.factoryIdentifier("other", Identifier.IDENTIFIER_TYPE.OTHER));
    Assert.assertNotNull(DataResourceUtils.getInternalIdentifier(res));
    res = DataResource.factoryNewDataResource("internal_identifier");
    Assert.assertNotNull(DataResourceUtils.getInternalIdentifier(res));
    Assert.assertEquals("internal_identifier", DataResourceUtils.getInternalIdentifier(res));
    res = DataResource.factoryNewDataResource("internal_identifier");
    //clear alternate identifiers (should never happen)
    res.getAlternateIdentifiers().clear();
    Assert.assertNull(DataResourceUtils.getInternalIdentifier(res));
  }

  @Test
  public void testGetAccessPermission(){
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    //check normal permission from ACL entry
    Assert.assertEquals(PERMISSION.WRITE, DataResourceUtils.getAccessPermission(res));

    res.getAcls().clear();
    //check NONE permission if no ACL entry exists
    Assert.assertEquals(PERMISSION.NONE, DataResourceUtils.getAccessPermission(res));

    //add NONE permission for user and test for NONE access via ACL
    res.getAcls().add(new AclEntry("tester", PERMISSION.NONE));
    Assert.assertEquals(PERMISSION.NONE, DataResourceUtils.getAccessPermission(res));

    res.getAcls().clear();

    //check as ADMIN without ACL entry
    //first, assign ADMINISTRATOR role by authorization
    mockJwtUserAuthentication(RepoUserRole.ADMINISTRATOR);
    //permission are now ADMINISTRATE, ACL entries are ignored
    Assert.assertEquals(PERMISSION.ADMINISTRATE, DataResourceUtils.getAccessPermission(res));
  }

  @Test
  public void testHasPermission(){
    //build rtesource with acls
    //mock authorization helper
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    //check normal permission from ACL entry
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.NONE));

    res.getAcls().clear();
    //check NONE permission if no ACL entry exists
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.NONE));

    //check as ADMIN without ACL entry
    //first, assign ADMINISTRATOR role by authorization
    mockJwtUserAuthentication(RepoUserRole.ADMINISTRATOR);
    //permission are now ADMINISTRATE, ACL entries are ignored
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.NONE));
  }

  @Test
  public void testPermissionCheckSuccessful(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.VOLATILE);
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.WRITE);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.NONE);
  }

  @Test
  public void testPermissionCheckWithoutState(){
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.WRITE);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.NONE);
  }

  @Test(expected = AccessForbiddenException.class)
  public void testPermissionCheckFail(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.VOLATILE);
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.ADMINISTRATE);
  }

  @Test(expected = AccessForbiddenException.class)
  public void testPermissionCheckOnFixedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has WRITE permissions, resource is fixed, requesting WRITE permissions should fail
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.WRITE);
  }

  @Test
  public void testPermissionCheckReadOnFixedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has READ permissions, resource is fixed, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", PERMISSION.READ));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnFixedResourceWithAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has ADMINISTRATE permissions, resource is fixed, requesting WRITE permissions should succeed
    res.getAcls().add(new AclEntry("tester", PERMISSION.ADMINISTRATE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.WRITE);
  }

  @Test
  public void testPermissionCheckOnFixedResourceAsAdministrator(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has READ permissions but has also role ADMINISTRATOR, resource is fixed, requesting WRITE permissions should succeed
    res.getAcls().add(new AclEntry("tester", PERMISSION.READ));
    mockJwtUserAuthentication(RepoUserRole.ADMINISTRATOR);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.WRITE);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testPermissionCheckOnRevokedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has WRITE permissions, resource is revoked, requesting READ permissions should fail, resource should not be found
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnRevokedResourceWithAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has ADMINISTRATE permissions, resource is revoked, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", PERMISSION.ADMINISTRATE));
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnRevokedResourceAsAdministrator(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has READ permissions but has also role ADMINISTRATOR, resource is revoked, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", PERMISSION.READ));
    mockJwtUserAuthentication(RepoUserRole.ADMINISTRATOR);
    DataResourceUtils.performPermissionCheck(res, PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckAsServiceUser() throws JsonProcessingException{
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));

    mockJwtServiceAuthentication(RepoServiceRole.SERVICE_READ);
    Assert.assertEquals(PERMISSION.READ, DataResourceUtils.getAccessPermission(res));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));

    mockJwtServiceAuthentication(RepoServiceRole.SERVICE_WRITE);
    Assert.assertEquals(PERMISSION.WRITE, DataResourceUtils.getAccessPermission(res));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));

    mockJwtServiceAuthentication(RepoServiceRole.SERVICE_ADMINISTRATOR);
    Assert.assertEquals(PERMISSION.ADMINISTRATE, DataResourceUtils.getAccessPermission(res));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, PERMISSION.ADMINISTRATE));
  }

  @Test
  public void testPermissionCheckAsTemporaryUser() throws JsonProcessingException{
    DataResource res1 = DataResource.factoryNewDataResource("res1");
    res1.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResource res2 = DataResource.factoryNewDataResource("res2");
    res2.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    DataResource res3 = DataResource.factoryNewDataResource("res3");
    res3.getAcls().add(new AclEntry("tester", PERMISSION.WRITE));
    ScopedPermission[] perms = new ScopedPermission[]{
      ScopedPermission.factoryScopedPermission("DataResource", "res1", PERMISSION.READ),
      ScopedPermission.factoryScopedPermission("DataResource", "res2", PERMISSION.WRITE),
      ScopedPermission.factoryScopedPermission("DataResource", "res3", PERMISSION.ADMINISTRATE)};

    mockJwtTemporaryAuthentication(perms);
    Assert.assertEquals(PERMISSION.READ, DataResourceUtils.getAccessPermission(res1));
    Assert.assertTrue(DataResourceUtils.hasPermission(res1, PERMISSION.READ));
    Assert.assertFalse(DataResourceUtils.hasPermission(res1, PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res1, PERMISSION.ADMINISTRATE));

    Assert.assertEquals(PERMISSION.WRITE, DataResourceUtils.getAccessPermission(res2));
    Assert.assertTrue(DataResourceUtils.hasPermission(res2, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res2, PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res2, PERMISSION.ADMINISTRATE));

    Assert.assertEquals(PERMISSION.ADMINISTRATE, DataResourceUtils.getAccessPermission(res3));
    Assert.assertTrue(DataResourceUtils.hasPermission(res3, PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res3, PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res3, PERMISSION.ADMINISTRATE));
  }

  private void mockJwtUserAuthentication(RepoUserRole role){
    Map<String, Object> claimMap = new HashMap<>();
    claimMap.put("tokenType", JwtAuthenticationToken.TOKEN_TYPE.USER.toString());
    claimMap.put("username", "tester");
    claimMap.put("firstname", "test");
    claimMap.put("lastname", "user");
    claimMap.put("email", "test@mail.org");
    claimMap.put("groupid", "USERS");
    claimMap.put("roles", Arrays.asList(role.getValue()));
    JwtAuthenticationToken userToken = JwtAuthenticationToken.factoryToken("test123", claimMap);
    PowerMockito.mockStatic(AuthenticationHelper.class);
    when(AuthenticationHelper.getAuthentication()).thenReturn(userToken);
    when(AuthenticationHelper.hasAuthority(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.hasIdentity(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.getPrincipal()).thenCallRealMethod();
    when(AuthenticationHelper.getAuthorizationIdentities()).thenCallRealMethod();
    when(AuthenticationHelper.getScopedPermission(any(String.class), any(String.class))).thenCallRealMethod();
  }

  private void mockJwtServiceAuthentication(RepoServiceRole role) throws JsonProcessingException{
    Map<String, Object> claimMap = new HashMap<>();
    claimMap.put("tokenType", JwtAuthenticationToken.TOKEN_TYPE.SERVICE.toString());
    claimMap.put("servicename", "metadata_extractor");
    claimMap.put("roles", Arrays.asList(role.getValue()));
    claimMap.put("groupid", "USERS");
    JwtAuthenticationToken serviceToken = JwtAuthenticationToken.factoryToken("test123", claimMap);
    PowerMockito.mockStatic(AuthenticationHelper.class);
    when(AuthenticationHelper.getAuthentication()).thenReturn(serviceToken);
    when(AuthenticationHelper.hasAuthority(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.hasIdentity(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.getPrincipal()).thenCallRealMethod();
    when(AuthenticationHelper.getAuthorizationIdentities()).thenCallRealMethod();
    when(AuthenticationHelper.getScopedPermission(any(String.class), any(String.class))).thenCallRealMethod();
  }

  private void mockJwtTemporaryAuthentication(ScopedPermission[] perms) throws JsonProcessingException{
    Map<String, Object> claimMap = new HashMap<>();
    claimMap.put("tokenType", JwtAuthenticationToken.TOKEN_TYPE.TEMPORARY.toString());
    claimMap.put("principalname", "test@mail.org");
    claimMap.put("permissions", new ObjectMapper().writeValueAsString(perms));
    JwtAuthenticationToken temporaryToken = JwtAuthenticationToken.factoryToken("test123", claimMap);
    PowerMockito.mockStatic(AuthenticationHelper.class);
    when(AuthenticationHelper.getAuthentication()).thenReturn(temporaryToken);
    when(AuthenticationHelper.hasAuthority(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.hasIdentity(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.getPrincipal()).thenCallRealMethod();
    when(AuthenticationHelper.getAuthorizationIdentities()).thenCallRealMethod();
    when(AuthenticationHelper.getScopedPermission(any(String.class), any(String.class))).thenCallRealMethod();
  }

//  private void mockAuthentication(final RepoUserRole role){
//    
//    
//    
//    
//    
//    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
//      @Override
//      public Collection<? extends GrantedAuthority> getAuthorities(){
//        Collection<GrantedAuthority> authorities = new ArrayList<>();
//        authorities.add(new SimpleGrantedAuthority(role.toString()));
//        return authorities;
//      }
//
//      @Override
//      public Object getCredentials(){
//        return "none";
//      }
//
//      @Override
//      public Object getDetails(){
//        return null;
//      }
//
//      @Override
//      public Object getPrincipal(){
//        return "tester";
//      }
//
//      @Override
//      public boolean isAuthenticated(){
//        return true;
//      }
//
//      @Override
//      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
//      }
//
//      @Override
//      public String getName(){
//        return "tester";
//      }
//    });
//  }
}
