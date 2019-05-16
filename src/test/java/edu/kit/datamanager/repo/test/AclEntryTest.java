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

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class AclEntryTest{

  @Test
  public void testAclEntryCreation(){
    AclEntry entry = new AclEntry("test123", PERMISSION.READ);
    Assert.assertEquals("test123", entry.getSid());
    Assert.assertEquals(PERMISSION.READ, entry.getPermission());

    //test manual property setting
    entry = new AclEntry();
    entry.setSid("test123");
    entry.setPermission(PERMISSION.READ);
    Assert.assertEquals("test123", entry.getSid());
    Assert.assertEquals(PERMISSION.READ, entry.getPermission());
  }

  @Test
  public void testEqualsAndHashCode(){
    AclEntry entry1 = new AclEntry("test123", PERMISSION.READ);
    entry1.setId(1l);
    AclEntry entry2 = new AclEntry("test123", PERMISSION.READ);
    entry2.setId(1l);

    //check equal with different types
    Assert.assertTrue(entry1.equals(entry2));
    Assert.assertFalse(entry1.equals("A String"));

    //check equal with different id
    entry1.setId(2l);
    Assert.assertFalse(entry1.equals(entry2));

    //check equal with different permission
    entry1.setId(1l);
    entry1.setPermission(PERMISSION.WRITE);
    Assert.assertFalse(entry1.equals(entry2));

    //check equal with different sid
    entry1.setPermission(PERMISSION.READ);
    entry1.setSid("123test");
    Assert.assertFalse(entry1.equals(entry2));

  }

}
