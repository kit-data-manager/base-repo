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

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
      @Override
      public Collection<? extends GrantedAuthority> getAuthorities(){
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RepoUserRole.USER.toString()));
        return authorities;
      }

      @Override
      public Object getCredentials(){
        return "none";
      }

      @Override
      public Object getDetails(){
        return null;
      }

      @Override
      public Object getPrincipal(){
        return "tester";
      }

      @Override
      public boolean isAuthenticated(){
        return true;
      }

      @Override
      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
      }

      @Override
      public String getName(){
        return "tester";
      }
    });

    when(AuthenticationHelper.hasAuthority(any(String.class))).thenCallRealMethod();
    when(AuthenticationHelper.getUsername()).thenCallRealMethod();
    when(AuthenticationHelper.getPrincipalIdentifiers()).thenCallRealMethod();
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
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    //check normal permission from ACL entry
    Assert.assertEquals(AclEntry.PERMISSION.WRITE, DataResourceUtils.getAccessPermission(res));

    res.getAcls().clear();
    //check NONE permission if no ACL entry exists
    Assert.assertEquals(AclEntry.PERMISSION.NONE, DataResourceUtils.getAccessPermission(res));

    //add NONE permission for user and test for NONE access via ACL
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.NONE));
    Assert.assertEquals(AclEntry.PERMISSION.NONE, DataResourceUtils.getAccessPermission(res));

    res.getAcls().clear();

    //check as ADMIN without ACL entry
    //first, assign ADMINISTRATOR role by authorization
    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
      @Override
      public Collection<? extends GrantedAuthority> getAuthorities(){
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.toString()));
        return authorities;
      }

      @Override
      public Object getCredentials(){
        return "none";
      }

      @Override
      public Object getDetails(){
        return null;
      }

      @Override
      public Object getPrincipal(){
        return "tester";
      }

      @Override
      public boolean isAuthenticated(){
        return true;
      }

      @Override
      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
      }

      @Override
      public String getName(){
        return "tester";
      }
    });
    //permission are now ADMINISTRATE, ACL entries are ignored
    Assert.assertEquals(AclEntry.PERMISSION.ADMINISTRATE, DataResourceUtils.getAccessPermission(res));
  }

  @Test
  public void testHasPermission(){
    //build rtesource with acls
    //mock authorization helper
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    //check normal permission from ACL entry
    Assert.assertFalse(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.ADMINISTRATE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.NONE));

    res.getAcls().clear();
    //check NONE permission if no ACL entry exists
    Assert.assertFalse(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.ADMINISTRATE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.WRITE));
    Assert.assertFalse(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.NONE));

    //check as ADMIN without ACL entry
    //first, assign ADMINISTRATOR role by authorization
    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
      @Override
      public Collection<? extends GrantedAuthority> getAuthorities(){
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.toString()));
        return authorities;
      }

      @Override
      public Object getCredentials(){
        return "none";
      }

      @Override
      public Object getDetails(){
        return null;
      }

      @Override
      public Object getPrincipal(){
        return "tester";
      }

      @Override
      public boolean isAuthenticated(){
        return true;
      }

      @Override
      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
      }

      @Override
      public String getName(){
        return "tester";
      }
    });
    //permission are now ADMINISTRATE, ACL entries are ignored
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.ADMINISTRATE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.WRITE));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.READ));
    Assert.assertTrue(DataResourceUtils.hasPermission(res, AclEntry.PERMISSION.NONE));
  }

  @Test
  public void testPermissionCheckSuccessful(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.VOLATILE);
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.WRITE);
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.NONE);
  }

  @Test
  public void testPermissionCheckWithoutState(){
    DataResource res = DataResource.factoryNewDataResource();
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.WRITE);
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.NONE);
  }

  @Test(expected = AccessForbiddenException.class)
  public void testPermissionCheckFail(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.VOLATILE);
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.ADMINISTRATE);
  }

  @Test(expected = AccessForbiddenException.class)
  public void testPermissionCheckOnFixedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has WRITE permissions, resource is fixed, requesting WRITE permissions should fail
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.WRITE);
  }

  @Test
  public void testPermissionCheckReadOnFixedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has READ permissions, resource is fixed, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.READ));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnFixedResourceWithAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has ADMINISTRATE permissions, resource is fixed, requesting WRITE permissions should succeed
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.ADMINISTRATE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.WRITE);
  }

  @Test
  public void testPermissionCheckOnFixedResourceAsAdministrator(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.FIXED);
    //user has READ permissions but has also role ADMINISTRATOR, resource is fixed, requesting WRITE permissions should succeed
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.READ));
    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
      @Override
      public Collection<? extends GrantedAuthority> getAuthorities(){
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.toString()));
        return authorities;
      }

      @Override
      public Object getCredentials(){
        return "none";
      }

      @Override
      public Object getDetails(){
        return null;
      }

      @Override
      public Object getPrincipal(){
        return "tester";
      }

      @Override
      public boolean isAuthenticated(){
        return true;
      }

      @Override
      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
      }

      @Override
      public String getName(){
        return "tester";
      }
    });
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.WRITE);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testPermissionCheckOnRevokedResourceWithoutAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has WRITE permissions, resource is revoked, requesting READ permissions should fail, resource should not be found
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.WRITE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnRevokedResourceWithAdministratePermissions(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has ADMINISTRATE permissions, resource is revoked, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.ADMINISTRATE));
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
  }

  @Test
  public void testPermissionCheckOnRevokedResourceAsAdministrator(){
    DataResource res = DataResource.factoryNewDataResource();
    res.setState(DataResource.State.REVOKED);
    //user has READ permissions but has also role ADMINISTRATOR, resource is revoked, requesting READ permissions should succeed
    res.getAcls().add(new AclEntry("tester", AclEntry.PERMISSION.READ));
    when(AuthenticationHelper.getAuthentication()).thenReturn(new Authentication(){
      @Override
      public Collection<? extends GrantedAuthority> getAuthorities(){
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(RepoUserRole.ADMINISTRATOR.toString()));
        return authorities;
      }

      @Override
      public Object getCredentials(){
        return "none";
      }

      @Override
      public Object getDetails(){
        return null;
      }

      @Override
      public Object getPrincipal(){
        return "tester";
      }

      @Override
      public boolean isAuthenticated(){
        return true;
      }

      @Override
      public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException{
      }

      @Override
      public String getName(){
        return "tester";
      }
    });
    DataResourceUtils.performPermissionCheck(res, AclEntry.PERMISSION.READ);
  }
}
