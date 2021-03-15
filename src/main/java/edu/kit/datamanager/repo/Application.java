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
package edu.kit.datamanager.repo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.configuration.DateBasedStorageProperties;
import edu.kit.datamanager.repo.configuration.IdBasedStorageProperties;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.ContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.RabbitMQMessagingService;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan({"edu.kit.datamanager"})
//@ComponentScan({"edu.kit.datamanager.repo", "edu.kit.datamanager.repo.configuration", "edu.kit.datamanager.service", "edu.kit.datamanager.configuration", "edu.kit.datamanager.repo.dao", "edu.kit.datamanager.repo.service", "edu.kit.datamanager.repo.service.impl", "edu.kit.datamanager.messaging.client"})
//@ComponentScan({"edu.kit.datamanager.repo", "edu.kit.datamanager.service", "edu.kit.datamanager.service.impl", "edu.kit.datamanager.configuration", "edu.kit.datamanager.repo.dao", "edu.kit.datamanager.repo.service", "edu.kit.datamanager.messaging.client"})
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  @Autowired
  private Javers javers;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IRepoVersioningService[] versioningServices;
  @Autowired
  private IRepoStorageService[] storageServices;

  @Autowired
  private IDataResourceDao dataResourceDao;

  @Autowired
  private IDataResourceService dataResourceService;
  @Autowired
  private IContentInformationService contentInformationService;

//  @Autowired
//  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;  
  @Bean
  @Scope("prototype")
  public Logger logger(InjectionPoint injectionPoint) {
    Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
    return LoggerFactory.getLogger(targetClass.getCanonicalName());
  }

  @Bean
  public IDataResourceService dataResourceService() {
    return new DataResourceService();
  }

  @Bean
  public IContentInformationService contentInformationService() {
    return new ContentInformationService();
  }

  @Bean(name = "OBJECT_MAPPER_BEAN")
  public ObjectMapper jsonObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
            .modules(new JavaTimeModule())
            .build();
  }

//  @Bean
//  public WebMvcConfigurer corsConfigurer(){
//    return new WebMvcConfigurer(){
//      @Override
//      public void addCorsMappings(CorsRegistry registry){
//        registry.addMapping("/**").allowedOrigins("http://localhost:8090").exposedHeaders("Content-Length").allowedHeaders("Accept");
//      }
//    };
//  }
//  @Bean
//  @Primary
//  public RequestMappingHandlerAdapter adapter(){
//    return requestMappingHandlerAdapter;
//  }
//  @Bean
//  public JsonViewSupportFactoryBean views(){
//    return new JsonViewSupportFactoryBean();
//  }
  @Bean
  @ConfigurationProperties("repo")
  public ApplicationProperties applicationProperties() {
    return new ApplicationProperties();
  }

  @Bean
  public IdBasedStorageProperties idBasedStorageProperties() {
    return new IdBasedStorageProperties();
  }

  @Bean
  public DateBasedStorageProperties dateBasedStorageProperties() {
    return new DateBasedStorageProperties();
  }

  @Bean
  public IMessagingService messagingService() {
    return new RabbitMQMessagingService();
  }

  @Bean
  public RepoBaseConfiguration repositoryConfig() {

    IAuditService<DataResource> auditServiceDataResource;
    IAuditService<ContentInformation> contentAuditService;
    RepoBaseConfiguration rbc = new RepoBaseConfiguration();
    rbc.setBasepath(this.applicationProperties.getBasepath());
    rbc.setReadOnly(this.applicationProperties.isReadOnly());
    rbc.setDataResourceService(dataResourceService);
    rbc.setContentInformationService(contentInformationService);
    rbc.setEventPublisher(eventPublisher);
    rbc.setJwtSecret(this.applicationProperties.getJwtSecret());
    rbc.setAuthEnabled(this.applicationProperties.isAuthEnabled());
    for (IRepoVersioningService versioningService : this.versioningServices) {
      if (applicationProperties.getDefaultVersioningService().equals(versioningService.getServiceName())) {
        LOG.info("Set versioning service: {}", versioningService.getServiceName());
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : this.storageServices) {
      if (applicationProperties.getDefaultStorageService().equals(storageService.getServiceName())) {
        LOG.info("Set storage service: {}", storageService.getServiceName());
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
    dataResourceService.configure(rbc);
    contentInformationService.configure(rbc);
    rbc.setAuditService(auditServiceDataResource);
     LOG.trace("Show Config: {}", rbc);
     LOG.trace("getBasepath {}", rbc.getBasepath());
     LOG.trace("getJwtSecret {}", rbc.getJwtSecret());
     LOG.trace("isAuditEnabled {}", rbc.isAuditEnabled());
     LOG.trace("isAuthEnabled {}", rbc.isAuthEnabled());
     LOG.trace("isReadOnly {}", rbc.isReadOnly());
     LOG.trace("getStorageService {}", rbc.getStorageService().getServiceName());
     LOG.trace("getVersioningService {}", rbc.getVersioningService().getServiceName());
   return rbc;
  }

  public static void main(String[] args) {
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    System.out.println("Spring is running!");
  }

}
