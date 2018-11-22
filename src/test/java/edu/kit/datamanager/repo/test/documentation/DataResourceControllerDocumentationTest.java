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
import edu.kit.datamanager.repo.Application;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            .alwaysDo(document("{method-name}",
                    preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
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
  public void testCreateResource() throws Exception{
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.createTitle("Most basic resource for testing", Title.TYPE.OTHER));
    resource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"Karlsruhe Institute of Technology"}));
    resource.setResourceType(ResourceType.createResourceType("testingSample", ResourceType.TYPE_GENERAL.DATASET));
    ObjectMapper mapper = new ObjectMapper();
    this.mockMvc.perform(post("/api/v1/dataresources/").contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated());
  }
}
