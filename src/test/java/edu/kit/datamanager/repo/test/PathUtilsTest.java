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

import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.util.PathUtils;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class PathUtilsTest{

  @Test
  public void testGetDataUri() throws Exception{
    // get current year
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);

    DataResource resource = DataResource.factoryNewDataResource("test123");
    ApplicationProperties props = new ApplicationProperties();
    //test with trailing slash 
    props.setBasepath(new URL("file:///tmp/"));
    props.setPathPattern("@{year}");

    Assert.assertTrue(PathUtils.getDataUri(resource, "folder/file.txt", props).toString().startsWith("file:/tmp/" + currentYear + "/test123/folder/file.txt_"));
    //test w/o trailing slash
    props.setBasepath(new URL("file:///tmp"));
    Assert.assertTrue(PathUtils.getDataUri(resource, "folder/file.txt", props).toString().startsWith("file:/tmp/" + currentYear + "/test123/folder/file.txt_"));

    //test with URL-escaped chars
    props.setBasepath(new URL("file:///f%C3%B4ld%C4%99r/"));

    Assert.assertTrue(PathUtils.getDataUri(resource, "folder/file.txt", props).toString().startsWith("file:/" + URLEncoder.encode("fôldęr", "UTF-8") + "/" + currentYear + "/test123/folder/file.txt_"));
   
    //test without URL-escaped chars
    props.setBasepath(new URL("file:///fôldęr/"));
    Assert.assertTrue(PathUtils.getDataUri(resource, "folder/file.txt", props).toString().startsWith("file:/" + URLEncoder.encode("fôldęr", "UTF-8") + "/" + currentYear + "/test123/folder/file.txt_"));
  }

  @Test(expected = CustomInternalServerError.class)
  public void testInvalidBasePath() throws Exception{
    DataResource resource = DataResource.factoryNewDataResource("test123");
    ApplicationProperties props = new ApplicationProperties();
    props.setBasepath(new URL("file:///fold?<>:er/"));
    Assert.fail("Creating the following path should not work: " + PathUtils.getDataUri(resource, "folder/file.txt", props));
  }

  @Test(expected = CustomInternalServerError.class)
  public void testNoInternalIdentifier() throws Exception{
    DataResource resource = DataResource.factoryNewDataResource("test123");
    resource.getAlternateIdentifiers().clear();
    ApplicationProperties props = new ApplicationProperties();
    props.setBasepath(new URL("file:///folder/"));
    Assert.fail("Creating the following path should not work: " + PathUtils.getDataUri(resource, "folder/file.txt", props));
  }

}
