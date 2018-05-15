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
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.monitorjbl.json.JsonViewSupportFactoryBean;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.impl.ContentInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 *
 * @author jejkal
 */
@SpringBootApplication
@ComponentScan({"edu.kit.datamanager"})
public class Application{

  @Autowired
  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  @Bean
  @Scope("prototype")
  public Logger logger(InjectionPoint injectionPoint){
    Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
    return LoggerFactory.getLogger(targetClass.getCanonicalName());
  }

  @Bean
  public IDataResourceService dataResourceService(){
    return new DataResourceService();
  }

  @Bean
  public IContentInformationService contentInformationService(){
    return new ContentInformationService();
  }

 @Bean(name = "OBJECT_MAPPER_BEAN")
public ObjectMapper jsonObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_NULL) // Don’t include null values
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
            .modules(new JavaTimeModule())
            .build();
}

  @Bean
  @Primary
  public RequestMappingHandlerAdapter adapter(){
    return requestMappingHandlerAdapter;
  }

  @Bean
  public JsonViewSupportFactoryBean views(){
    return new JsonViewSupportFactoryBean();
  }

  @Bean
  @ConfigurationProperties("repo")
  public ApplicationProperties applicationProperties(){
    return new ApplicationProperties();
  }

//  @Bean
//  public ConnectionFactory connectionFactory(){
//    return new CachingConnectionFactory("localhost");
//  }
//
//  @Bean
//  public AmqpAdmin amqpAdmin(){
//    return new RabbitAdmin(connectionFactory());
//  }
//
//  @Bean
//  public RabbitTemplate rabbitTemplate(){
//    return new RabbitTemplate(connectionFactory());
//  }
//
//  @Bean
//  TopicExchange exchange(){
//    return new TopicExchange("topic_note");
//  }
//  @Bean
//  public Queue myQueue(){
//    return new Queue("myqueue");
//  }
//  @Bean
//  public Filter shallowETagHeaderFilter(){
//    return new ShallowEtagHeaderFilter();
//  }
  public static void main(String[] args){
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    ApplicationProperties bean = ctx.getBean(ApplicationProperties.class);
    System.out.println(bean);
    /*  String[] beanNames = ctx.getBeanDefinitionNames();
    Arrays.sort(beanNames);
    for(String beanName : beanNames){
      System.out.println(beanName);
    }
    System.out.println("Spring Boot started...");*/
  }

}
