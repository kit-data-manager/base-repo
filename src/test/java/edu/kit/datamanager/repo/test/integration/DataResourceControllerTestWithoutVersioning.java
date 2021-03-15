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
package edu.kit.datamanager.repo.test.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.Box;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.Contributor;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.Description;
import edu.kit.datamanager.repo.domain.FunderIdentifier;
import edu.kit.datamanager.repo.domain.FundingReference;
import edu.kit.datamanager.repo.domain.GeoLocation;
import edu.kit.datamanager.repo.domain.Point;
import edu.kit.datamanager.repo.domain.Polygon;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Scheme;
import edu.kit.datamanager.repo.domain.Subject;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.impl.DataResourceService;
import edu.kit.datamanager.service.IAuditService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalTo;
import org.javers.core.Javers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author jejkal
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@TestPropertySource(properties = {"repo.plugin.versioning=none"})
@ActiveProfiles("test")
public class DataResourceControllerTestWithoutVersioning {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  Javers javers = null;
  @Autowired
  private IDataResourceService dataResourceService;
  @Autowired
  private IContentInformationDao contentInformationDao;
  @Autowired
  private IAllIdentifiersDao allIdentifiersDao;

  private IAuditService<ContentInformation> contentInformationAuditService;

  @Autowired
  private RepoBaseConfiguration repositoryConfig;

  private String adminToken;
  private String userToken;
  private String otherUserToken;
  private String guestToken;

  private DataResource sampleResource;
  private DataResource otherResource;
  private DataResource revokedResource;
  private DataResource fixedResource;

  @Before
  public void setUp() throws JsonProcessingException {
    contentInformationAuditService = repositoryConfig.getContentInformationAuditService();
    contentInformationDao.deleteAll();
    dataResourceDao.deleteAll();
    allIdentifiersDao.deleteAll();

    adminToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("admin", RepoUserRole.ADMINISTRATOR).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("groupid", "USERS").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(repositoryConfig.getJwtSecret());

    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("user", RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(repositoryConfig.getJwtSecret());

    otherUserToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("otheruser", RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(repositoryConfig.getJwtSecret());

    guestToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("guest", RepoUserRole.GUEST).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(repositoryConfig.getJwtSecret());

    sampleResource = DataResource.factoryNewDataResource("altIdentifier");
    sampleResource.setState(DataResource.State.VOLATILE);
    sampleResource.getDescriptions().add(Description.factoryDescription("This is a description", Description.TYPE.OTHER, "en"));
    sampleResource.getTitles().add(Title.factoryTitle("Title", Title.TYPE.OTHER));
    sampleResource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    sampleResource.getCreators().add(Agent.factoryAgent("Johanna", "Doe", new String[]{"FZJ"}));
    sampleResource.getContributors().add(Contributor.factoryContributor(Agent.factoryAgent("Jane", "Doe", new String[]{"KIT"}), Contributor.TYPE.DATA_MANAGER));
    sampleResource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.CREATED));
    sampleResource.setEmbargoDate(Instant.now().truncatedTo(ChronoUnit.MILLIS).plus(Duration.ofDays(365)));
    sampleResource.setResourceType(ResourceType.createResourceType("photo", ResourceType.TYPE_GENERAL.IMAGE));
    sampleResource.setLanguage("en");
    sampleResource.setPublisher("me");
    sampleResource.setPublicationYear("2018");
    sampleResource.getFormats().add("plain/text");
    sampleResource.getSizes().add("100");
    sampleResource.getFundingReferences().add(FundingReference.factoryFundingReference("BMBF", FunderIdentifier.factoryIdentifier("BMBF-01", FunderIdentifier.FUNDER_TYPE.ISNI), Scheme.factoryScheme("BMBF_AWARD", "https://www.bmbf.de/"), "https://www.bmbf.de/01", "Award 01"));
    sampleResource.getAcls().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));
    sampleResource.getAcls().add(new AclEntry("otheruser", PERMISSION.READ));
    sampleResource.getAcls().add(new AclEntry("user", PERMISSION.WRITE));
    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Point.factoryPoint(12.1f, 13.0f)));
    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Box.factoryBox(12.0f, 13.0f, 14.0f, 15.0f)));
    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Box.factoryBox(Point.factoryPoint(10.0f, 11.0f), Point.factoryPoint(42.0f, 45.1f))));
    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation(Polygon.factoryPolygon(Point.factoryPoint(12.1f, 13.0f), Point.factoryPoint(14.1f, 12.0f), Point.factoryPoint(16.1f, 11.0f))));
    sampleResource.getGeoLocations().add(GeoLocation.factoryGeoLocation("A place"));
    sampleResource.getRelatedIdentifiers().add(RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_DOCUMENTED_BY, "document_location", Scheme.factoryScheme("id", "uri"), "metadata_scheme"));
    sampleResource.getSubjects().add(Subject.factorySubject("testing", "uri", "en", Scheme.factoryScheme("id", "uri")));

    sampleResource = dataResourceDao.save(sampleResource);
    ((DataResourceService) dataResourceService).saveIdentifiers(sampleResource);

    otherResource = DataResource.factoryNewDataResource("otherResource");
    otherResource.getDescriptions().add(Description.factoryDescription("This is a description", Description.TYPE.OTHER, "en"));
    otherResource.getTitles().add(Title.factoryTitle("Title", Title.TYPE.OTHER));
    otherResource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    otherResource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.CREATED));
    otherResource.setPublisher("me");
    otherResource.setPublicationYear("2018");
    otherResource.getAcls().add(new AclEntry("admin", PERMISSION.WRITE));
    otherResource.getAcls().add(new AclEntry("otheruser", PERMISSION.ADMINISTRATE));
    otherResource.getAcls().add(new AclEntry("user", PERMISSION.READ));
    otherResource.setState(DataResource.State.REVOKED);

    otherResource = dataResourceDao.save(otherResource);
    ((DataResourceService) dataResourceService).saveIdentifiers(otherResource);

    revokedResource = DataResource.factoryNewDataResource("revokedResource");
    revokedResource.getDescriptions().add(Description.factoryDescription("This is a description", Description.TYPE.OTHER, "en"));
    revokedResource.getTitles().add(Title.factoryTitle("Title", Title.TYPE.OTHER));
    revokedResource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    revokedResource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.CREATED));
    revokedResource.setPublisher("me");
    revokedResource.setPublicationYear("2018");
    revokedResource.getAcls().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));
    revokedResource.getAcls().add(new AclEntry("user", PERMISSION.WRITE));
    revokedResource.setState(DataResource.State.REVOKED);

    revokedResource = dataResourceDao.save(revokedResource);
    ((DataResourceService) dataResourceService).saveIdentifiers(revokedResource);

    fixedResource = DataResource.factoryNewDataResource("fixedResource");
    fixedResource.getDescriptions().add(Description.factoryDescription("This is a description", Description.TYPE.OTHER, "en"));
    fixedResource.getTitles().add(Title.factoryTitle("Title", Title.TYPE.OTHER));
    fixedResource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    fixedResource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.CREATED));
    fixedResource.setPublisher("me");
    fixedResource.setPublicationYear("2018");
    fixedResource.getAcls().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));
    fixedResource.getAcls().add(new AclEntry("user", PERMISSION.WRITE));
    fixedResource.setState(DataResource.State.FIXED);

    fixedResource = dataResourceDao.save(fixedResource);
    ((DataResourceService) dataResourceService).saveIdentifiers(fixedResource);
  }

  /**
   * FIND TESTS*
   */
  @Test
  public void testGetDataResources() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/").param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(4))).andReturn();
  }

  @Test
  public void testGetDataResourcesWithInvalidPageSize() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/").param("page", "0").param("size", "1000").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(4)));
  }

  @Test
  public void testGetDataResourcesAsGuest() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/").param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + guestToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
  }

  @Test
  public void testFindDataResourcesByExampleAsUser() throws Exception {
    DataResource example = new DataResource();
    example.setState(null);
    example.setPublicationYear("2018");
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));
  }

  @Test
  public void testFindDataResourcesByExampleWithCreatorAsUser() throws Exception {
    DataResource example = new DataResource();
    example.setState(null);
    example.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    ObjectMapper mapper = createObjectMapper();

    //search for John Doe from KIT
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));

    //search for family name Doe
    example.getCreators().clear();
    example.getCreators().add(Agent.factoryAgent(null, "Doe"));
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));

    //search for family name Doe and affiliation FZJ
    example.getCreators().clear();
    example.getCreators().add(Agent.factoryAgent("Johanna", null, new String[]{"FZJ"}));
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));
  }

  @Test
  public void testFindUsingResourceType() throws Exception {
    DataResource example = new DataResource();
    example.setState(null);
    example.setResourceType(ResourceType.createResourceType(null, ResourceType.TYPE_GENERAL.IMAGE));
    ObjectMapper mapper = createObjectMapper();

    //search for resource type IMAGE
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //search for resource type value 'photo'
    example.setResourceType(ResourceType.createResourceType("photo", null));
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //search for resource type with no value
    example.setResourceType(ResourceType.createResourceType(null, null));
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));

  }

  @Test
  public void testFindUsingUnsupportedField() throws Exception {
    DataResource example = new DataResource();
    example.setState(null);
    example.getContributors().add(Contributor.factoryContributor(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}), Contributor.TYPE.OTHER));
    ObjectMapper mapper = createObjectMapper();

    //search for John Doe from KIT
    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNotImplemented());
  }

  @Test
  public void testFindDataResourcesByExampleWithInvalidPageNumber() throws Exception {
    DataResource example = new DataResource();
    example.setState(null);
    example.setPublicationYear("2018");
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "10").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
  }

  @Test
  public void testFindAllByExampleViaServiceAfterRevokation() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    DataResource example = new DataResource();
    example.setState(null);
    example.setLanguage("en");
    example.setPublicationYear("2018");

    int resourcesBeforeWithRevoked = dataResourceService.findAll(example, PageRequest.of(0, 10), true).getNumberOfElements();
    int resourcesBeforeWithoutRevoked = dataResourceService.findAll(example, PageRequest.of(0, 10), false).getNumberOfElements();

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isNoContent());

    int resourcesAfterWithRevoked = dataResourceService.findAll(example, PageRequest.of(0, 10), true).getNumberOfElements();
    int resourcesAfterWithoutRevoked = dataResourceService.findAll(example, PageRequest.of(0, 10), false).getNumberOfElements();

    Assert.assertEquals(resourcesBeforeWithRevoked, resourcesBeforeWithoutRevoked);
    Assert.assertNotEquals(resourcesAfterWithRevoked, resourcesAfterWithoutRevoked);
  }

  @Test
  public void testFindAllViaServiceAfterRevokation() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    int resourcesBeforeWithRevoked = dataResourceService.findAll(null, PageRequest.of(0, 10), true).getNumberOfElements();
    int resourcesBeforeWithoutRevoked = dataResourceService.findAll(null, PageRequest.of(0, 10), false).getNumberOfElements();

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isNoContent());

    int resourcesAfterWithRevoked = dataResourceService.findAll(null, PageRequest.of(0, 10), true).getNumberOfElements();
    int resourcesAfterWithoutRevoked = dataResourceService.findAll(null, PageRequest.of(0, 10), false).getNumberOfElements();

    Assert.assertEquals(4, resourcesBeforeWithRevoked);
    Assert.assertEquals(2, resourcesBeforeWithoutRevoked);

    Assert.assertEquals(4, resourcesAfterWithRevoked);
    Assert.assertEquals(1, resourcesAfterWithoutRevoked);
  }

  /**
   * GET TESTS*
   */
  @Test
  public void testGetDataResourceById() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.titles[0].value").value("Title")).andExpect(MockMvcResultMatchers.jsonPath("$.acls").exists());
  }

  @Test
  public void testGetDataResourceByIdAsGuest() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + guestToken)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testGetDataResourceByUnknownId() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testGetRevokedDataResourceByIdWithAdminRole() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + revokedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.titles[0].value").value("Title"));
  }

  @Test
  public void testGetRevokedDataResourceByIdWithAdministratePermissions() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.titles[0].value").value("Title"));
  }

  @Test
  public void testGetRevokedDataResourceByIdAsUser() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + revokedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testGetAclWithAdminRole() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.acls").exists());
  }

  @Test
  public void testGetAclWithAdministratePermissions() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.acls").exists());
  }

  /**
   * CREATE TESTS*
   */
  @Test
  public void testCreateResource() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    resource.setResourceType(ResourceType.createResourceType("autogenerated", ResourceType.TYPE_GENERAL.DATASET));
    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.titles[0].value").value("Created Resource"));
  }

  @Test
  public void testCreateResourceTwiceWithSameId() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.getCreators().add(Agent.factoryAgent("John", "Doe", new String[]{"KIT"}));
    resource.setResourceType(ResourceType.createResourceType("autogenerated", ResourceType.TYPE_GENERAL.DATASET));
    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.titles[0].value").value("Created Resource"));
    String contentAsString = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    DataResource result = mapper.readValue(contentAsString, DataResource.class);
    this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(result))).andExpect(status().isConflict());
  }

  @Test
  public void testCreateResourceWithAlternateIdentifier() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier("test123"));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.alternateIdentifiers[0].value").value("test123"));
  }

  @Test
  public void testCreateResourceWithInvalidAlternateIdentifier() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(null));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateResourceWithOtherAlternateIdentifier() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.getAlternateIdentifiers().add(Identifier.factoryIdentifier("someIdentifier", Identifier.IDENTIFIER_TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);
    //resource should have two identifiers: One of type OTHER and one INTERNAL
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.alternateIdentifiers", Matchers.hasSize(2)));
  }

  @Test
  public void testCreateResourceWithIdentifier() throws Exception {
    DataResource resource = new DataResource();
    resource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier("12.123/123"));
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.identifier.value").value("12.123/123"));
  }

  @Test
  public void testCreateResourceAnonymous() throws Exception {
    DataResource resource = new DataResource();
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/").contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreateResourceWithExistingIdentifier() throws Exception {
    DataResource resource = new DataResource();
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier("altIdentifier"));
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isConflict());

  }

  @Test
  public void testCreateResourceWithoutTitle() throws Exception {
    DataResource resource = new DataResource();
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateResourceWithoutResourceType() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    ObjectMapper mapper = createObjectMapper();

    this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateResourceWithPublisherAndPublicationYear() throws Exception {
    DataResource resource = new DataResource();
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(UUID.randomUUID().toString()));
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.setPublisher("me");
    resource.setPublicationYear("2018");

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.publisher").value("me")).andExpect(MockMvcResultMatchers.jsonPath("$.publicationYear").value("2018"));
  }

  @Test
  public void testCreateResourceWithCreationDate() throws Exception {
    DataResource resource = new DataResource();
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(UUID.randomUUID().toString()));
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.CREATED));
    resource.setPublisher("me");
    resource.setPublicationYear("2018");

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.dates", Matchers.hasSize(1)));
  }

  @Test
  public void testCreateResourceWithOtherDate() throws Exception {
    DataResource resource = new DataResource();
    resource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(UUID.randomUUID().toString()));
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.getDates().add(Date.factoryDate(Instant.now().truncatedTo(ChronoUnit.MILLIS), Date.DATE_TYPE.SUBMITTED));
    resource.setPublisher("me");
    resource.setPublicationYear("2018");

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.dates", Matchers.hasSize(2)));
  }

  @Test
  public void testCreateResourceWithCallerAcl() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.getAcls().add(new AclEntry("user", PERMISSION.ADMINISTRATE));

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.acls", Matchers.hasSize(1)));
  }

  @Test
  public void testCreateResourceWithCallerAclButWriteOnly() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.getAcls().add(new AclEntry("user", PERMISSION.WRITE));

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    //As 'user' is the caller, the final permission should be ADMINISTRATE and not WRITE as provided
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.acls", Matchers.hasSize(1))).andExpect(MockMvcResultMatchers.jsonPath("$.acls[0].permission").value(PERMISSION.ADMINISTRATE.name()));
  }

  @Test
  public void testCreateResourceWithNoCallerAcl() throws Exception {
    DataResource resource = new DataResource();
    resource.getTitles().add(Title.factoryTitle("Created Resource", Title.TYPE.OTHER));
    resource.setResourceType(ResourceType.createResourceType("autogenerated"));
    resource.getAcls().add(new AclEntry("admin", PERMISSION.WRITE));

    ObjectMapper mapper = createObjectMapper();

    String location = this.mockMvc.perform(post("/api/v1/dataresources/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    String resourceId = location.substring(location.lastIndexOf("/") + 1);

    //As 'user' is the caller, the final permission should be ADMINISTRATE and not WRITE as provided
    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.acls", Matchers.hasSize(2)));
  }

  /**
   * DELETE TESTS*
   */
  @Test
  public void testDeleteResourceAnonymousWithoutETag() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId())
            .contentType("application/json")).andExpect(status().isPreconditionRequired());
  }

  @Test
  public void testDeleteResourceAnonymous() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId())
            .header("If-Match", "anyEtag").contentType("application/json")).andExpect(status().isUnauthorized());
  }

  @Test
  public void testDeleteInvalidResourceWithoutETag() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json")).andExpect(status().isPreconditionRequired());
  }

  @Test
  public void testDeleteInvalidResource() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/0").header("If-Match", "anyEtag").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).contentType("application/json")).andExpect(status().isNoContent());
  }

  @Test
  public void testDeleteResourceWithoutPermission() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).contentType("application/json")).andExpect(status().isForbidden());
  }

  @Test
  public void testDeleteResourceAsAdmin() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isNoContent());

    //try a second time...this should work
    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isNoContent());
    //from now on, the resource should be in state GONE...HTTP GET should fail
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isNotFound());

  }

  @Test
  public void testDeleteResourceAsAdminWithWrongEtag() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", "0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isPreconditionFailed());
  }

  @Test
  public void testDeleteViaService() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json")).andExpect(status().isNoContent());

    dataResourceDao.delete(sampleResource);

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isNotFound());

    Assert.assertFalse(dataResourceDao.findById(sampleResource.getId()).isPresent());
  }

  /**
   * PATCH TESTS*
   */
  @Test
  public void testPatchResourceAnonymousWithoutEtag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId()).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isPreconditionRequired());
  }

  @Test
  public void testPatchResourceAnonymousWithEtag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isUnauthorized());
  }

  @Test
  public void testPatchUnknownResourceWithoutEtag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isPreconditionRequired());
  }

  @Test
  public void testPatchUnknownResourceWithEtag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    this.mockMvc.perform(patch("/api/v1/dataresources/0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testPatchResourceWithoutPermission() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testPatchRevokedResourceWithoutPermission() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + revokedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", "\"" + revokedResource.getEtag() + "\"").contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testPatchFixedResourceWithoutPermission() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + fixedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(patch("/api/v1/dataresources/" + fixedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testPatchResourceWithoutETag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + revokedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isPreconditionRequired());
  }

  @Test
  public void testPatchResourceWithInvalidETag() throws Exception {
    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"1900\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + revokedResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", "0").contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isPreconditionFailed());
  }

  @Test
  public void testPatchResourceAsAdmin() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"2017\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.publicationYear").value("2017"));

  }

  @Test
  public void testPatchAlternateIdentifier() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"add\",\"path\": \"/alternateIdentifiers/1\",\"value\": {\"identifierType\":\"OTHER\", \"value\":\"another-identifier\"}}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.alternateIdentifiers", Matchers.hasSize(2)));
  }

  @Test
  public void testPatchAlternateDuplicateIdentifier() throws Exception {
    //first, add identifier to otherResource...
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"add\",\"path\": \"/alternateIdentifiers/1\",\"value\": {\"identifierType\":\"OTHER\", \"value\":\"will-be-duplicated\"}}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andExpect(content().string(Matchers.containsString("will-be-duplicated")));

    //now change to sample resource and try to add identifier, too
    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isConflict());

  }

  @Test
  public void testPatchResourceWithAdminPermissions() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"2017\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.publicationYear").value("2017"));
  }

  @Test
  public void testPatchInvalidField() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/id\",\"value\": \"0\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testApplyInvalidPatch() throws Exception {
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/invalid\",\"value\": \"0\"}]";
    this.mockMvc.perform(patch("/api/v1/dataresources/" + otherResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isUnprocessableEntity());
  }

  /**
   * PUT TESTS*
   */
  @Test
  public void testPutResourceAsAdmin() throws Exception {
    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse();

    String etag = response.getHeader("ETag");
    String resourceString = response.getContentAsString();

    ObjectMapper mapper = createObjectMapper();
    DataResource resource = mapper.readValue(resourceString, DataResource.class);

    resource.setPublisher("OtherPub");
    this.mockMvc.perform(put("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", etag).contentType("application/json").content(mapper.writeValueAsString(resource))).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.publisher").value("OtherPub"));
  }

  @Test
  public void testPutResourceAsAnonymous() throws Exception {
    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse();

    String etag = response.getHeader("ETag");
    String resourceString = response.getContentAsString();

    ObjectMapper mapper = createObjectMapper();
    DataResource resource = mapper.readValue(resourceString, DataResource.class);

    resource.setPublisher("Anonymous");
    this.mockMvc.perform(put("/api/v1/dataresources/" + sampleResource.getId()).header("If-Match", etag).contentType("application/json").content(mapper.writeValueAsString(resource))).andDo(print()).andExpect(status().isUnauthorized());
  }

  @Test
  public void testPutResourceUnauthorized() throws Exception {
    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse();

    String etag = response.getHeader("ETag");
    String resourceString = response.getContentAsString();

    ObjectMapper mapper = createObjectMapper();
    DataResource resource = mapper.readValue(resourceString, DataResource.class);

    resource.setPublisher("Guest");
    this.mockMvc.perform(put("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + guestToken).header("If-Match", etag).contentType("application/json").content(mapper.writeValueAsString(resource))).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testPutResourceWithoutEtag() throws Exception {
    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse();

    String resourceString = response.getContentAsString();

    ObjectMapper mapper = createObjectMapper();
    DataResource resource = mapper.readValue(resourceString, DataResource.class);

    resource.setPublisher("OtherPub");
    this.mockMvc.perform(put("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).contentType("application/json").content(mapper.writeValueAsString(resource))).andDo(print()).andExpect(status().isPreconditionRequired());
  }

  /**
   * Upload tests
   */
  @Test
  public void testUploadFile() throws Exception {
    Path temp = Files.createTempFile("testUploadFile", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.relativePath").value("bibtex.txt"));
  }

  /**
   * Upload tests
   */
  @Test
  public void testUploadFileWithDataInPath() throws Exception {
    Path temp = Files.createTempFile("testUploadFile", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/test/data/bibtex.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/test/data/bibtex.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.relativePath").value("test/data/bibtex.txt"));
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testUploadFileWithoutPermissions() throws Exception {
    Path temp = Files.createTempFile("testUploadFileWithoutPermissions", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testUploadFileAnonymous() throws Exception {
    Path temp = Files.createTempFile("testUploadFileAnonymous", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex.txt", "multipart/form-data", Files.newInputStream(temp));
    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex.txt").file(fstmp)).andDo(print()).andExpect(status().isUnauthorized());
  }

  @Test
  public void testUploadFileForInvalidResource() throws Exception {
    Path temp = Files.createTempFile("testUploadFileForInvalidResource", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/0/data/bibtex.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testUploadExistingWithoutForce() throws Exception {
    Path temp = Files.createTempFile("testUploadExistingWithoutForce", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex1.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex1.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex1.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testUploadExistingWithForce() throws Exception {
    Path temp = Files.createTempFile("testUploadExistingWithForce", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex2.txt", "multipart/form-data", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex2.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex2.txt").file(fstmp).param("force", Boolean.TRUE.toString()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());
  }

  @Test
  public void testUploadExistingWithForceAndMetadataUpdate() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "ok");
    cinfo.setMetadata(metadata);
    ObjectMapper mapper = createObjectMapper();

    Path temp = Files.createTempFile("testUploadExistingWithForceAndMetadataUpdate", "test");

    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex3.txt", "application/json", Files.newInputStream(temp));
    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex3.txt").file(fstmp).file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex3.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.metadata['test']").value("ok"));

    metadata.put("test", "changed");
    secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex3.txt").file(fstmp).file(secmp).param("force", Boolean.TRUE.toString()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex3.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.metadata['test']").value("changed"));
  }

  @Test
  public void testUploadWithReference() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setContentUri("http://www.google.de");
    ObjectMapper mapper = createObjectMapper();

    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/google.de").file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/google.de").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.contentUri").value("http://www.google.de"));

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/google.de").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isSeeOther()).andExpect(header().string("Location", equalTo("http://www.google.de")));
  }

  @Test
  public void testUploadWithReferenceToCustomProtocol() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setContentUri("myProto://file.txt");
    ObjectMapper mapper = createObjectMapper();

    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/file.txt").file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/file.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.contentUri").value("myProto://file.txt"));

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/file.txt").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNoContent()).andExpect(header().string("Content-Location", equalTo("myProto://file.txt")));
  }

  /**
   * Content Information Query
   */
  @Test
  public void testQueryByTag() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);

    ObjectMapper mapper = createObjectMapper();
    Path temp = Files.createTempFile("testQueryByTag", "test");

    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex4.txt", "application/json", Files.newInputStream(temp));
    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex4.txt").file(fstmp).file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    cinfo = new ContentInformation();

    fstmp = new MockMultipartFile("file", "bibtex5.txt", "application/json", Files.newInputStream(temp));
    secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex5.txt").file(fstmp).file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    //get by tag ... should return one element
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").param("tag", "testing").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$").isArray()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$[0].tags[0]").value("testing"));

    //get by unknown tag...should return all elements (result set size should not be 1)
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").param("tag", "other").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$").isArray()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(Matchers.not(1))));
  }

  @Test
  public void testFindContentByExample() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "ok");
    cinfo.setMetadata(metadata);
    cinfo.setMediaType("application/json");

    ObjectMapper mapper = createObjectMapper();

    Path temp = Files.createTempFile("testUploadExistingWithForceAndMetadataUpdate", "test");
    Files.write(temp, "Test file".getBytes());

    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex3.txt", "text/plain", Files.newInputStream(temp));
    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex3.txt").file(fstmp).file(secmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    MvcResult res = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andReturn();

    //ContentInformation result = mapper.
    //res.getResponse().getContentAsString()
    //get all content with type text/plain
    ContentInformation example = new ContentInformation();
    example.setMediaType("text/plain");
    //expect one result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //get all content with type application/json
    example.setMediaType("application/json");
    //expect no result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));

    //reset media type and set wildcard selection of txt files
    example.setMediaType(null);
    example.setRelativePath("%.txt");
    //expect one result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //XXXXX
    //test search by content uri
    example = new ContentInformation();
    //  example.setContentUri("file:/tmp/2019/altIdentifier/bibtex3.txt%");

    //expect one result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //XXXXXX
    //test search by metadata only with key
    example.setContentUri(null);
    metadata.clear();
    metadata.put("test", null);
    example.setMetadata(metadata);
    //expect one result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //test search by metadata with key and value
    metadata.clear();
    metadata.put("test", "ok");
    example.setMetadata(metadata);
    //expect one result
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    //test search by metadata only with wrong key
    metadata.clear();
    metadata.put("wrong", "ok");
    example.setMetadata(metadata);
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));

    //test search by metadata only with wrong value
    metadata.clear();
    metadata.put("test", "fail");
    example.setMetadata(metadata);
    this.mockMvc.perform(post("/api/v1/dataresources/search/data").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(example)).param("page", "0").param("size", "10").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));
  }

  @Test
  public void testQueryForInvalidObject() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/0/data/").param("tag", "testing").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testQueryForInvalidContent() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/notExist").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testRemoveLeadingSlashFromPath() throws Exception {

    Path temp = Files.createTempFile("testRemoveLeadingSlashFromPath", "test");
    MockMultipartFile fstmp = new MockMultipartFile("file", "bibtex5.txt", "application/json", Files.newInputStream(temp));

    this.mockMvc.perform(multipart("/api/v1/dataresources/" + sampleResource.getId() + "/data/bibtex5.txt").file(fstmp).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isCreated());

    //CHECK THIS
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data//").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$").isArray()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$[0].relativePath").value("bibtex5.txt"));
  }

  /**
   * Download Tests
   */
  @Test
  public void testVariousContentDownload() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("missingFile");
    cinfo.setVersioningService("none");
    cinfo.setContentUri("file:///invalidlocation/missingFile");
    contentInformationDao.save(cinfo);

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("fileWithoutUriScheme");
    cinfo.setVersioningService("none");
    cinfo.setContentUri("/invalidlocation/missingFile");
    contentInformationDao.save(cinfo);

    Path temp = Files.createTempFile("testVariousContentDownload", "test");
    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("validFile");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("invalidRemoteUri");
    cinfo.setContentUri("http://somedomain.new/myFileWhichDoesNotExist");
    contentInformationDao.save(cinfo);

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("withMediaType");
    cinfo.setMediaType("text/plain");
    temp = Files.createTempFile("testVariousContentDownload2", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("withRedirect");
    cinfo.setContentUri("http://www.heise.de");
    contentInformationDao.save(cinfo);

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/missingFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/fileWithoutUriScheme").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNoContent()).andExpect(header().string("Content-Location", equalTo("/invalidlocation/missingFile")));

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/invalidRemoteUri").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isServiceUnavailable()).andExpect(header().string("Content-Location", equalTo("http://somedomain.new/myFileWhichDoesNotExist")));

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/withMediaType").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isOk()).andExpect(header().string("Content-Type", equalTo("text/plain")));

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/withRedirect").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(header().string("Location", equalTo("http://www.heise.de")));

//collection download ... first fails due to invalid element at /data/missingFile
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("Accept", "application/zip")).andDo(print()).andExpect(status().isInternalServerError());

    //rebuild valid content
    contentInformationDao.deleteAllInBatch();
    Path firstFile = Paths.get(System.getProperty("java.io.tmpdir"), "firstFile.txt");
    Path secondFile = Paths.get(System.getProperty("java.io.tmpdir"), "secondFile.txt");
    Files.write(firstFile, "This is ".getBytes());
    Files.write(secondFile, "a test! ".getBytes());

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("firstFile.txt");
    cinfo.setContentUri(firstFile.toUri().toString());
    contentInformationDao.save(cinfo);

    cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setVersioningService("none");
    cinfo.setRelativePath("secondFile.txt");
    cinfo.setContentUri(secondFile.toUri().toString());
    contentInformationDao.save(cinfo);

    //try again collection download
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("Accept", "application/zip")).andDo(print()).andExpect(status().isOk());

  }

  @Test
  public void testDownloadCollection() throws Exception {
    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken)).andDo(print()).andExpect(status().isNotFound());
  }

  /**
   * Patch tests
   */
  @Test
  public void testPatchContentInformation() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchContentInformation", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"add\",\"path\": \"/tags/0\",\"value\": \"success\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/").param("tag", "success").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$[0].relativePath").value("validFile"));
  }

  @Test
  public void testPatchInvalidContentInformationField() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchInvalidContentInformationField", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/depth\",\"value\": \"132\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testPatchWithoutPermissions() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchWithoutPermissions", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"add\",\"path\": \"/tags/0\",\"value\": \"success\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + otherUserToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isForbidden());
  }

  @Test
  public void testPatchWithUnknownResource() throws Exception {
    String patch = "[{\"op\": \"add\",\"path\": \"/tags/0\",\"value\": \"success\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/0/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", "\"123456\"").contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testPatchWithUnknownContent() throws Exception {
    String patch = "[{\"op\": \"add\",\"path\": \"/tags/0\",\"value\": \"success\"}]";
    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId()).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/notExist").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testPatchWithInvalidEtag() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchWithInvalidEtag", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String patch = "[{\"op\": \"add\",\"path\": \"/tags/0\",\"value\": \"success\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + userToken).header("If-Match", "\"0\"").contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isPreconditionFailed());
  }

  @Test
  public void testPatchWithAdminPermission() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchWithAdminPermission", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/size\",\"value\": \"4711\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.size").value("4711"));
  }

  @Test
  public void testPatchAnonymous() throws Exception {
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    Path temp = Files.createTempFile("testPatchAnonymous", "txt");
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    String patch = "[{\"op\": \"replace\",\"path\": \"/size\",\"value\": \"4711\"}]";

    this.mockMvc.perform(patch("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isUnauthorized());
  }

  @Test
  public void testDeleteContent() throws Exception {
    Path temp = Files.createTempFile("testDeleteContentAnonymous", "txt");
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    cinfo.setContentUri(temp.toUri().toString());
    cinfo = contentInformationDao.save(cinfo);
    contentInformationAuditService.captureAuditInformation(cinfo, "admin");

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk());

    //try with invalid etag
    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header("If-Match", "0").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isPreconditionFailed());

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header("If-Match", etag).header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken)).andDo(print()).andExpect(status().isNoContent());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testDeleteContentFromInvalidResource() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/0/data/notExist").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", "0000")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testDeleteInvalidContent() throws Exception {
    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId() + "/data/notExist").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header("If-Match", "\"" + sampleResource.getEtag() + "\"")).andDo(print()).andExpect(status().isNoContent());
  }

  @Test
  public void testDeleteContentAnonymous() throws Exception {
    Path temp = Files.createTempFile("testDeleteContentAnonymous", "txt");
    ContentInformation cinfo = new ContentInformation();
    cinfo.setParentResource(sampleResource);
    cinfo.setRelativePath("validFile");
    cinfo.setVersioningService("none");
    Set<String> tags = new HashSet<>();
    tags.add("testing");
    cinfo.setTags(tags);
    cinfo.setContentUri(temp.toUri().toString());
    contentInformationDao.save(cinfo);

    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk());

    this.mockMvc.perform(delete("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header("If-Match", etag)).andDo(print()).andExpect(status().isUnauthorized());

    this.mockMvc.perform(get("/api/v1/dataresources/" + sampleResource.getId() + "/data/validFile").header(HttpHeaders.AUTHORIZATION,
            "Bearer " + adminToken).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andDo(print()).andExpect(status().isOk());
  }

  private ObjectMapper createObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
            .modules(new JavaTimeModule())
            .build();
  }

  /**
   * VERSIONING TESTS**
   */
  // Skip versioning tests due to disabled versioning.
}
