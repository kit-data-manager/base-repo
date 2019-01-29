
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.UnknownInformationConstants;
import org.junit.Assert;
import org.junit.Test;

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
/**
 *
 * @author jejkal
 */
public class PrimaryIdentifierTest{

  @Test
  public void testFactoryPrimaryIdentifierWithUnknownInformation(){
    PrimaryIdentifier pi = PrimaryIdentifier.factoryPrimaryIdentifier(UnknownInformationConstants.KWOWN_TO_BE_UNKNOWN);
    Assert.assertEquals("DOI", pi.getIdentifierType());
    Assert.assertEquals(UnknownInformationConstants.KWOWN_TO_BE_UNKNOWN.getValue(), pi.getValue());
    Assert.assertFalse(pi.hasDoi());
  }

  @Test
  public void testFactoryPrimaryIdentifierWithDOI(){
    PrimaryIdentifier pi = PrimaryIdentifier.factoryPrimaryIdentifier("123.456/abcd");
    Assert.assertEquals("DOI", pi.getIdentifierType());
    Assert.assertEquals("123.456/abcd", pi.getValue());
    Assert.assertTrue(pi.hasDoi());
  }
}
