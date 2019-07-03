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

import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 *
 * @author jejkal
 */
public class ContentInformationTest{

  @Test
  public void testManualCreation(){
    DataResource parentResource = DataResource.factoryNewDataResource();
    ContentInformation info = new ContentInformation();
    info.setContentUri("file:///tmp/data/myfile.txt");
    info.setHash("md5:134abcd234fgh");
    info.setMediaType("text/plain");
    Map<String, String> metadata = new HashMap<>();
    metadata.put("type", "a file");
    info.setMetadata(metadata);
    info.setParentResource(parentResource);
    Set<String> tags = new HashSet<>();
    tags.add("file");
    tags.add("important");
    info.setTags(tags);
    info.setRelativePath("data/myfile.txt");

    Assert.assertEquals("file:///tmp/data/myfile.txt", info.getContentUri());
    Assert.assertEquals("md5:134abcd234fgh", info.getHash());
    Assert.assertEquals("text/plain", info.getMediaType());
    Assert.assertEquals(MediaType.TEXT_PLAIN, info.getMediaTypeAsObject());
    Assert.assertNotNull(info.getMetadata().size());
    Assert.assertEquals(1, info.getMetadata().size());
    Assert.assertEquals("a file", info.getMetadata().get("type"));
    Assert.assertNotNull(info.getParentResource());
    Assert.assertEquals(parentResource.getId(), info.getParentResource().getId());
    Assert.assertNotNull(info.getTags());
    Assert.assertEquals(2, info.getTags().size());
    Assert.assertEquals("data/myfile.txt", info.getRelativePath());
    Assert.assertEquals(2, info.getDepth());

  }

  @Test
  public void testCreateTemplate(){
    ContentInformation info = ContentInformation.createContentInformation("1", "folder/file.txt", "testing");
    Assert.assertNotNull(info.getParentResource());
    Assert.assertEquals("1", info.getParentResource().getId());
    Assert.assertEquals("folder/file.txt", info.getRelativePath());
    Assert.assertFalse(info.getTags().isEmpty());
    Assert.assertEquals("testing", info.getTags().toArray(new String[]{})[0]);

    info = ContentInformation.createContentInformation("1", "folder/file.txt", (String[]) null);
    Assert.assertTrue(info.getTags().isEmpty());

    info = ContentInformation.createContentInformation("1", "folder/file.txt", (String) null);
    Assert.assertTrue(info.getTags().isEmpty());
  }

  @Test(expected = NullPointerException.class)
  public void testCreateTemplateWithNullParentId(){
    ContentInformation info = ContentInformation.createContentInformation(null, "/");
    Assert.fail("Content information " + info + " should not have been created.");
  }

  @Test(expected = NullPointerException.class)
  public void testCreateTemplateWithNullPath(){
    ContentInformation info = ContentInformation.createContentInformation("1", null);
    Assert.fail("Content information " + info + " should not have been created.");
  }

  @Test
  public void testCreateContentInformation(){
    ContentInformation info = ContentInformation.createContentInformation("folder/file.txt");
    Assert.assertEquals("folder/file.txt", info.getRelativePath());
    Assert.assertEquals(2, info.getDepth());
  }

  @Test
  public void testCreateContentInformationWithNullArgument(){
    ContentInformation info = ContentInformation.createContentInformation(null);
    //works now as null check for relative path is no longer active
  }

  @Test
  public void testSetRelativePath(){
    ContentInformation info = new ContentInformation();
    //test remove leading slash
    info.setRelativePath("/file.txt");
    Assert.assertEquals("file.txt", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test remove trailing slash
    info.setRelativePath("folder/");
    Assert.assertEquals("folder", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test remove leading and trailing slash
    info.setRelativePath("/folder/");
    Assert.assertEquals("folder", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test root
    info.setRelativePath("/");
    Assert.assertEquals("", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test multi slashes
    info.setRelativePath("//folder//");
    Assert.assertEquals("folder", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test multi slashes on root
    info.setRelativePath("////");
    Assert.assertEquals("", info.getRelativePath());
    Assert.assertEquals(1, info.getDepth());

    //test multi slashes in between
    info.setRelativePath("//file//test.txt");
    Assert.assertEquals("file/test.txt", info.getRelativePath());
    Assert.assertEquals(2, info.getDepth());
  }

  @Test
  public void testSetRelativePathWithNullArgument(){
    ContentInformation info = new ContentInformation();
    info.setRelativePath(null);
    //works now as null path is allowed
  }

  @Test
  public void testGetFilename(){
    ContentInformation info = new ContentInformation();
    info.setRelativePath("/data/file.txt");
    Assert.assertEquals("file.txt", info.getFilename());
    info.setRelativePath("file.txt");
    Assert.assertEquals("file.txt", info.getFilename());
    info.setRelativePath("/file.txt");
    Assert.assertEquals("file.txt", info.getFilename());
    info = new ContentInformation();
    Assert.assertNull(info.getFilename());
  }

  @Test
  public void testSetMediaTypeAsObject(){
    ContentInformation info = new ContentInformation();
    //set media type by object
    info.setMediaTypeAsObject(MediaType.TEXT_PLAIN);
    Assert.assertEquals(MediaType.TEXT_PLAIN.toString(), info.getMediaType());
    Assert.assertEquals(MediaType.TEXT_PLAIN, info.getMediaTypeAsObject());

    //setting mediatype should also work with string
    info.setMediaType(MediaType.TEXT_PLAIN.toString());
    Assert.assertEquals(MediaType.TEXT_PLAIN.toString(), info.getMediaType());
    Assert.assertEquals(MediaType.TEXT_PLAIN, info.getMediaTypeAsObject());

    //set invalid type...result should be media type octet stream
    info.setMediaType("invalid");
    Assert.assertEquals(MediaType.APPLICATION_OCTET_STREAM, info.getMediaTypeAsObject());

    //test if media type is null
    info = new ContentInformation();
    Assert.assertEquals(MediaType.APPLICATION_OCTET_STREAM, info.getMediaTypeAsObject());
  }

  @Test
  public void testEqualsAndHashCode(){
    DataResource parentResource = DataResource.factoryNewDataResource();
    ContentInformation info1 = new ContentInformation();
    info1.setContentUri("file:///tmp/data/myfile.txt");
    info1.setHash("md5:134abcd234fgh");
    info1.setMediaType("text/plain");
    Map<String, String> metadata = new HashMap<>();
    metadata.put("type", "a file");
    info1.setMetadata(metadata);
    info1.setParentResource(parentResource);
    Set<String> tags = new HashSet<>();
    tags.add("file");
    tags.add("important");
    info1.setTags(tags);
    info1.setRelativePath("data/myfile.txt");

    ContentInformation info2 = new ContentInformation();
    info2.setContentUri("file:///tmp/data/myfile.txt");
    info2.setHash("md5:134abcd234fgh");
    info2.setMediaType("text/plain");
    Map<String, String> metadata2 = new HashMap<>();
    metadata2.put("type", "a file");
    info2.setMetadata(metadata2);
    info2.setParentResource(parentResource);
    Set<String> tags2 = new HashSet<>();
    tags2.add("file");
    tags2.add("important");
    info2.setTags(tags2);
    info2.setRelativePath("data/myfile.txt");

    //check basic equals
    Assert.assertTrue(info1.equals(info2));
    Assert.assertFalse(info1.equals(null));
    Assert.assertFalse(info1.equals("A String"));
    Assert.assertEquals(info1.hashCode(), info2.hashCode());

    //check with different content URI
    info1.setContentUri(info2.getContentUri() + "2");
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
    info1.setContentUri(info2.getContentUri());

    //check with different hash
    info1.setHash(info2.getHash() + "2");
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
    info1.setHash(info2.getHash());

    //check with diferent media type
    info1.setMediaType(MediaType.APPLICATION_ATOM_XML_VALUE.toString());
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
    info1.setMediaType(info2.getMediaType());

    //check with different relative path
    info1.setRelativePath("test/other.txt");
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
    info1.setRelativePath(info2.getRelativePath());
    //check with different metadata
    info1.getMetadata().put("second", "value");
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
    //clear metadata to be equal again
    info1.getMetadata().clear();
    info2.getMetadata().clear();
    Assert.assertTrue(info1.equals(info2));
    Assert.assertEquals(info1.hashCode(), info2.hashCode());

    //check with different tags
    info1.getTags().add("another");
    Assert.assertFalse(info1.equals(info2));
    Assert.assertNotEquals(info1.hashCode(), info2.hashCode());
  }
}
