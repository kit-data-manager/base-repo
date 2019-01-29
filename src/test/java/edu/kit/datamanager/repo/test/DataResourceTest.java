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
package edu.kit.datamanager.repo.test;

import edu.kit.datamanager.repo.domain.DataResource;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class DataResourceTest{

  @Test
  public void testFactoryDataResourceWithDoi(){
    DataResource res = DataResource.factoryDataResourceWithDoi("12.3456/789ab");
    Assert.assertNotNull(res.getIdentifier());
    Assert.assertEquals("DOI", res.getIdentifier().getIdentifierType());
    Assert.assertEquals("12.3456/789ab", res.getIdentifier().getValue());
  }

  @Test(expected = NullPointerException.class)
  public void testFactoryDataResourceWithNullDoi(){
    DataResource res = DataResource.factoryDataResourceWithDoi(null);
    Assert.fail("Test should have already failed.");
  }

  @Test(expected = NullPointerException.class)
  public void testFactoryDataResourceWithNullIdentifier(){
    DataResource res = DataResource.factoryNewDataResource(null);
    Assert.fail("Test should have already failed.");
  }

}
