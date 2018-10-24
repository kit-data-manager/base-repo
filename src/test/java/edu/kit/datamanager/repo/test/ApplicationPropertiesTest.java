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

import edu.kit.datamanager.configuration.GenericApplicationProperties;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class ApplicationPropertiesTest{

  @Test
  public void testApplicationProperties() throws Exception{
    ApplicationProperties props = new ApplicationProperties();
    props.setBasepath(URI.create("file:///tmp/").toURL());
    props.setJwtSecret("test123");
    Assert.assertEquals("file:/tmp/", props.getBasepath().toString());
    Assert.assertEquals("test123", props.getJwtSecret());
  }

  @Test
  public void testEqualsAndHashCode() throws Exception{
    ApplicationProperties props1 = new ApplicationProperties();
    props1.setBasepath(URI.create("file:///tmp/").toURL());
    props1.setJwtSecret("test123");
    ApplicationProperties props2 = new ApplicationProperties();
    props2.setBasepath(URI.create("file:///tmp/").toURL());
    props2.setJwtSecret("test123");
    Assert.assertTrue(props1.equals(props2));

    props1.setJwtSecret("different");
    Assert.assertFalse(props1.equals(props2));

    props1.setJwtSecret("test123");
    props1.setBasepath(URI.create("file:///otherFolder/").toURL());
    Assert.assertFalse(props1.equals(props2));
  }
}
