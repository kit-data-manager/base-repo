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
package edu.kit.datamanager.repo.test.documentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.repo.Application;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author jejkal
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("doc")
public class DataResourceControllerDocumentationTest{

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
//  @Autowired
//  private IDataResourceDao dataResourceDao;
//  @Autowired
//  private IDataResourceService dataResourceService;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  private DataResource sampleResource;

  @Before
  public void setUp() throws JsonProcessingException{
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(springSecurityFilterChain)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();

//    dataResourceDao.deleteAll();
//    sampleResource = DataResource.factoryNewDataResource("altIdentifier");
//    sampleResource.setState(DataResource.State.VOLATILE);
//    sampleResource.getDescriptions().add(Description.factoryDescription("This is a description", Description.TYPE.OTHER, "en"));
//    sampleResource.getTitles().add(Title.createTitle("Title", Title.TYPE.OTHER));
//    sampleResource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
//    sampleResource.getContributors().add(Contributor.factoryContributor(Agent.factoryAgent("Jane", "Doe", new String[]{"KIT"}), Contributor.TYPE.DATA_MANAGER));
//    sampleResource.getDates().add(Date.factoryDate(Instant.now(), Date.DATE_TYPE.CREATED));
//    sampleResource.setEmbargoDate(DateUtils.addDays(new java.util.Date(), 365));
//    sampleResource.setLanguage("en");
//    sampleResource.setPublisher("me");
//    sampleResource.setPublicationYear("2018");
//    sampleResource.getFormats().add("plain/text");
//    sampleResource.getSizes().add("100");
//    sampleResource.getFundingReferences().add(FundingReference.factoryFundingReference("BMBF", FunderIdentifier.factoryIdentifier("BMBF-01", FunderIdentifier.FUNDER_TYPE.ISNI), Scheme.factoryScheme("BMBF_AWARD", "https://www.bmbf.de/"), "https://www.bmbf.de/01", "Award 01"));
//    sampleResource.getAcls().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));
//    sampleResource.getAcls().add(new AclEntry("otheruser", PERMISSION.READ));
//    sampleResource.getAcls().add(new AclEntry("user", PERMISSION.WRITE));
//    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Point.factoryPoint(12.1f, 13.0f)));
//    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Box.factoryBox(12.0f, 13.0f, 14.0f, 15.0f)));
//    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Box.factoryBox(Point.factoryPoint(10.0f, 11.0f), Point.factoryPoint(42.0f, 45.1f))));
//    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Polygon.factoryPolygon(Point.factoryPoint(12.1f, 13.0f), Point.factoryPoint(14.1f, 12.0f), Point.factoryPoint(16.1f, 11.0f))));
//    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation("A place"));
//    sampleResource.getRelatedIdentifiers().add(RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_DOCUMENTED_BY, "document_location", Scheme.factoryScheme("id", "uri"), "metadata_scheme"));
//    sampleResource.getSubjects().add(Subject.factorySubject("testing", "uri", "en", Scheme.factoryScheme("id", "uri")));
//    sampleResource = dataResourceDao.save(sampleResource);
  }

  @Test
  public void documentBasicAccess() throws Exception{
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Most basic resource for testing", Title.TYPE.OTHER));
    resource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"Karlsruhe Institute of Technology"}));
    resource.setResourceType(ResourceType.createResourceType("testingSample", ResourceType.TYPE_GENERAL.DATASET));
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    //create resource and obtain location from response header
    String location = this.mockMvc.perform(post("/api/v1/dataresources/").contentType("application/json").content(mapper.writeValueAsString(resource))).
            andExpect(status().isCreated()).
            andDo(document("create-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).
            andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    //extract resourceId from response header and use it to issue a GET to obtain the current ETag
    String resourceId = location.substring(location.lastIndexOf("/") + 1);
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");

    //apply a simple patch to the resource
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"2017\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent()).andDo(document("patch-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //perform a GET for the patched resource...the publicationYear should be modified
    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-patched-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");

    //do some more complex patch adding a new alternate identifier
    patch = "[{\"op\": \"add\",\"path\": \"/alternateIdentifiers/1\",\"value\": {\"identifierType\":\"OTHER\", \"value\":\"resource-1-231118\"}}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent()).andDo(document("patch-resource-complex", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //get the resource again together with the current ETag
    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-patched-resource-complex", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    //perform PUT operation 
    etag = response.getHeader("ETag");
    String resourceString = response.getContentAsString();
    DataResource resourceToPut = mapper.readValue(resourceString, DataResource.class);
    resourceToPut.setPublisher("KIT Data Manager");
    this.mockMvc.perform(put("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json").content(mapper.writeValueAsString(resourceToPut))).andDo(print()).andExpect(status().isOk()).andDo(document("put-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-put-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");

    //try to GET the resource using the alternate identifier added a second ago
    this.mockMvc.perform(get("/api/v1/dataresources/" + "resource-1-231118")).andExpect(status().isSeeOther()).andDo(document("get-resource-by-alt-id", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //upload random data file
    Path temp = Files.createTempFile("randomFile", "test");

    try(FileWriter w = new FileWriter(temp.toFile())){
      w.write(RandomStringUtils.randomAlphanumeric(64));
      w.flush();
    }

    MockMultipartFile fstmp = new MockMultipartFile("file", "randomFile.txt", "multipart/form-data", Files.newInputStream(temp));
    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt").file(fstmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //upload random data file with metadata
    ContentInformation cinfo = new ContentInformation();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "ok");
    cinfo.setMetadata(metadata);
    fstmp = new MockMultipartFile("file", "randomFile2.txt", "multipart/form-data", Files.newInputStream(temp));
    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));
    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/randomFile2.txt").file(fstmp).file(secmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file-with-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //upload referenced file
    cinfo = new ContentInformation();
    cinfo.setContentUri("https://www.google.com");
    secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));
    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/referencedContent").file(secmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file-with-reference", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //obtain content metadata
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt").header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andExpect(status().isOk()).andDo(document("get-content-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //obtain content metadata as listing
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/").header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andExpect(status().isOk()).andDo(document("get-content-listing", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //download file
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt")).andExpect(status().isOk()).andDo(document("download-file", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

    //perform a DELETE
    this.mockMvc.perform(delete("/api/v1/dataresources/" + resourceId).header("If-Match", etag)).andExpect(status().isNoContent()).andDo(document("delete-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
    //perform a final GET to show that resources are still accessible by the owner/admin
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-deleted-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

  }

}
