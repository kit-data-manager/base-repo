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
import edu.kit.datamanager.configuration.SearchConfiguration;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.configuration.ElasticConfiguration;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.configuration.StorageServiceProperties;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.ContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.security.filter.KeycloakJwtProperties;
import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import edu.kit.datamanager.security.filter.KeycloakTokenValidator;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.RabbitMQMessagingService;
import java.util.Optional;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan({"edu.kit.datamanager"})
@EnableElasticsearchRepositories(basePackages = "edu.kit.datamanager.repo.elastic")
@EntityScan("edu.kit.datamanager") // if you have it
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    @Autowired
    private Javers javers;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    //private ApplicationProperties applicationProperties;
    @Autowired
    private IRepoVersioningService[] versioningServices;
    @Autowired
    private IRepoStorageService[] storageServices;

//    @Autowired
//    private IDataResourceDao dataResourceDao;
//    @Autowired
//    private ApplicationProperties applicationProperties;

    /* @Autowired
    private IDataResourceService dataResourceService;
    @Autowired
    private IContentInformationService contentInformationService;*/
//  @Autowired
//  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;  
//    @Bean
//    @Scope("prototype")
//    public Logger logger(InjectionPoint injectionPoint) {
//        Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
//        return LoggerFactory.getLogger(targetClass.getCanonicalName());
//    }
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

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        LOG.info("Refresh event detected. Configuration reloaded.");
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
    @ConfigurationProperties("repo")
    @ConditionalOnProperty(prefix = "repo.search", name = "enabled", havingValue = "true")
    public SearchConfiguration searchConfiguration() {
        return new SearchConfiguration();
    }

    /* @Bean
    public IdBasedStorageProperties idBasedStorageProperties() {
        return new IdBasedStorageProperties();
    }

    @Bean
    public DateBasedStorageProperties dateBasedStorageProperties() {
        return new DateBasedStorageProperties();
    }*/
    @Bean
    public StorageServiceProperties storageServiceProperties() {
        return new StorageServiceProperties();
    }

    @Bean
    @ConfigurationProperties("repo")
    @ConditionalOnProperty(prefix = "repo.messaging", name = "enabled", havingValue = "true")
    public Optional<IMessagingService> messagingService() {
        return Optional.of(new RabbitMQMessagingService());
    }

    @Bean
    public KeycloakJwtProperties keycloakProperties() {
        return new KeycloakJwtProperties();
    }

    @Bean
    @ConditionalOnProperty(
            value = "repo.auth.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public KeycloakTokenFilter keycloaktokenFilterBean() throws Exception {
        return new KeycloakTokenFilter(KeycloakTokenValidator.builder()
                .readTimeout(keycloakProperties().getReadTimeoutms())
                .connectTimeout(keycloakProperties().getConnectTimeoutms())
                .sizeLimit(keycloakProperties().getSizeLimit())
                .jwtLocalSecret(applicationProperties().getJwtSecret())
                .build(keycloakProperties().getJwkUrl(), keycloakProperties().getResource(), keycloakProperties().getJwtClaim()));
    }

    @Bean
    @RefreshScope
    public RepoBaseConfiguration repositoryConfig() {
        LOG.info("Loading repository configuration.");
        IAuditService<DataResource> auditServiceDataResource;
        ContentInformationAuditService contentAuditService;
        RepoBaseConfiguration rbc = new RepoBaseConfiguration();
        rbc.setBasepath(applicationProperties().getBasepath());
        rbc.setReadOnly(applicationProperties().isReadOnly());
        rbc.setDataResourceService(dataResourceService());
        rbc.setContentInformationService(contentInformationService());
        rbc.setEventPublisher(eventPublisher);
        rbc.setJwtSecret(applicationProperties().getJwtSecret());
        rbc.setAuthEnabled(applicationProperties().isAuthEnabled());
        if (applicationProperties().getDefaultStorageService() != null) {
            for (IRepoStorageService storageService : this.storageServices) {
                if (applicationProperties().getDefaultStorageService().equals(storageService.getServiceName())) {
                    storageService.configure(storageServiceProperties());
                    LOG.info("Set storage service: {}", storageService.getServiceName());
                    rbc.setStorageService(storageService);
                    break;
                }
            }
        }

        if (applicationProperties().getDefaultVersioningService() != null) {
            for (IRepoVersioningService versioningService : this.versioningServices) {
                if (applicationProperties().getDefaultVersioningService().equals(versioningService.getServiceName())) {
                    versioningService.configure(rbc);
                    LOG.info("Set versioning service: {}", versioningService.getServiceName());
                    rbc.setVersioningService(versioningService);
                    break;
                }
            }
        }

        auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
        contentAuditService = new ContentInformationAuditService(this.javers, rbc);
        dataResourceService().configure(rbc);
        contentInformationService().configure(rbc);
        rbc.setAuditService(auditServiceDataResource);
        rbc.setContentInformationAuditService(contentAuditService);
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
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        System.out.println("Spring is running!");

//        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
//        SchemaGeneratorConfig config = configBuilder.build();
//        SchemaGenerator generator = new SchemaGenerator(config);
//        JsonNode jsonSchema = generator.generateSchema(DataResource.class);
//
//        System.out.println(jsonSchema.toString());
    }

}
