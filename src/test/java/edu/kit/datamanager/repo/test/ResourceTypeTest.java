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

import edu.kit.datamanager.repo.domain.ResourceType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class ResourceTypeTest{

  @Test
  public void test(){
    ResourceType type = ResourceType.createResourceType("test", ResourceType.TYPE_GENERAL.EVENT);
    Assert.assertEquals("test", type.getValue());
    Assert.assertEquals(ResourceType.TYPE_GENERAL.EVENT, type.getTypeGeneral());
  }

  @Test
  public void testEqualsAndHashCode(){
    ResourceType type1 = ResourceType.createResourceType("test");
    type1.setId(1l);
    ResourceType type2 = ResourceType.createResourceType("test", ResourceType.TYPE_GENERAL.DATASET);
    type2.setId(1l);

    Assert.assertTrue(type1.equals(type2));
    Assert.assertFalse(type1.equals(null));
    Assert.assertFalse(type1.equals("A String"));

    type1.setValue("different");
    Assert.assertFalse(type1.equals(type2));

    type1.setValue("test");
    type1.setTypeGeneral(ResourceType.TYPE_GENERAL.EVENT);
    Assert.assertFalse(type1.equals(type2));

    type1.setTypeGeneral(ResourceType.TYPE_GENERAL.DATASET);
    type1.setId(2l);
    Assert.assertFalse(type1.equals(type2));
  }

}
