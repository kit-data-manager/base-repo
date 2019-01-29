
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.DataResource;
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
public class AgentTest{
  
  @Test
  public void testFactoryAgent(){
    Agent ag = Agent.factoryAgent("John", "Doe");
    Assert.assertEquals("John", ag.getGivenName());
    Assert.assertEquals("Doe", ag.getFamilyName());
    Assert.assertNotNull(ag.getAffiliations());
    Assert.assertTrue(ag.getAffiliations().isEmpty());
  }
}
