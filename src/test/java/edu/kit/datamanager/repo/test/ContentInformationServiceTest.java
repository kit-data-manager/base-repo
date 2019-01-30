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

import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.io.ByteArrayInputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 *
 * @author jejkal
 */
@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*"})
@PrepareForTest(AuthenticationHelper.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  TransactionalTestExecutionListener.class
})
@ActiveProfiles("test")
public class ContentInformationServiceTest{

  @Autowired
  private IContentInformationService service;
  @Autowired
  private IContentInformationDao dao;
  @Autowired
  private IDataResourceService dataResourceService;
  @Autowired
  private IDataResourceDao dataResourceDao;

  private DataResource parentResource = null;

  @Before
  public void prepare(){
    DataResource resource = createResourceWithoutDoi("test123", "Test Title", "Test");
    parentResource = dataResourceService.create(resource, AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL);
  }

  @After
  public void cleanDb(){
    dao.deleteAll();
    dataResourceDao.deleteAll();
  }

  @Test
  public void testFindById(){
    ContentInformation info = createContentInformation("test123", "file.txt", "tag1");

    ByteArrayInputStream in = new ByteArrayInputStream("test123".getBytes());

    info = service.create(info, parentResource, "file.txt", in, false);
    ContentInformation found = service.findById(Long.toString(info.getId()));
    Assert.assertNotNull(found);
    Assert.assertEquals(info.getId(), found.getId());
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testFindByUnknownId(){
    ContentInformation found = service.findById("256712");
    Assert.fail("Test should have already failed.");
  }

  @Test(expected = CustomInternalServerError.class)
  public void testFindAllWithoutPerent(){
    ContentInformation info = createContentInformation("test123", "file.txt", "tag1");
    info.setParentResource(null);
    Page<ContentInformation> found = service.findAll(info, PageRequest.of(0, 10));
    Assert.fail("Test should have already failed.");
  }

  @Test(expected = BadArgumentException.class)
  public void testCreateWithFileUri(){
    ContentInformation info = createContentInformation("test123", "file2.txt", "tag1");
    info.setContentUri("file:///Users/data/dummy.txt");
    info = service.create(info, parentResource, "file.txt", null, false);
    Assert.fail("Test should have already failed.");
  }

  @Test(expected = BadArgumentException.class)
  public void testCreateWithoutFileAndContentUri(){
    ContentInformation info = createContentInformation("test123", "file2.txt", "tag1");
    info = service.create(info, parentResource, "file.txt", null, false);
    Assert.fail("Test should have already failed.");
  }

  private ContentInformation createContentInformation(String id, String path, String... tags){
    return ContentInformation.createContentInformation(id, path, tags);
  }

  private DataResource createResourceWithoutDoi(String iid, String title, String type){
    DataResource resource;

    resource = DataResource.factoryNewDataResource(iid);

    if(title != null){
      resource.getTitles().add(Title.factoryTitle(title, Title.TYPE.TRANSLATED_TITLE));
    }
    if(type != null){
      resource.setResourceType(ResourceType.createResourceType(type));
    }
    return resource;
  }
}
