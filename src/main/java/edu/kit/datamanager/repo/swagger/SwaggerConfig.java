/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.swagger;


/**
 *
 * @author jejkal
 */
//@Configuration
//@EnableSwagger2WebMvc
public class SwaggerConfig{

 // @Bean
//  public Docket api(){
//    return new Docket(DocumentationType.SWAGGER_2)
//            .select()
//            .apis(RequestHandlerSelectors.basePackage("edu.kit.datamanager.repo.web"))
//            .paths(PathSelectors.any())
//            .build()
//            .ignoredParameterTypes(Pageable.class, WebRequest.class, HttpServletResponse.class, UriComponentsBuilder.class)
//            .apiInfo(apiInfo())
//            .securitySchemes(Arrays.asList(apiKey()))//, new BasicAuth("test")));
//            .securityContexts(Arrays.asList(securityContext()));
//  }
//
//  private ApiKey apiKey(){
//    return new ApiKey("Authorization", "Authorization", "header");
//  }
//
//  private ApiInfo apiInfo(){
//    return new ApiInfo(
//            "Repository Microservice - RESTful API",
//            "This webpage describes the RESTful interface of the KIT Data Manager Repository Microservice.",
//            "0.1",
//            null,
//            new Contact("KIT Data Manager Support", "datamanager.kit.edu", "support@datamanager.kit.edu"),
//            "Apache 2.0",
//            "http://www.apache.org/licenses/LICENSE-2.0.html",
//            Collections.emptyList());
//  }

//  private SecurityContext securityContext(){
//    return SecurityContext.builder()
//            .securityReferences(defaultAuth())
//            .forPaths(PathSelectors.regex("/api/v1.*"))
//            .build();
//  }

//  List<SecurityReference> defaultAuth(){
//    AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
//    AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
//    authorizationScopes[0] = authorizationScope;
//    return Lists.newArrayList(new SecurityReference("Authorization", authorizationScopes));
//  }
//  @Bean
//  SecurityContext securityContext(){
//    AuthorizationScope readScope = new AuthorizationScope("read:pets", "read your pets");
//    AuthorizationScope[] scopes = new AuthorizationScope[1];
//    scopes[0] = readScope;
//    SecurityReference securityReference = SecurityReference.builder()
//            .reference("petstore_auth")
//            .scopes(scopes)
//            .build();
//
//    return SecurityContext.builder()
//            .securityReferences(newArrayList(securityReference))
//            .forPaths(ant("/api/pet.*"))
//            .build();
//  }
//
//  @Bean
//  SecurityScheme oauth(){
//    return new OAuthBuilder()
//            .name("petstore_auth")
//            .grantTypes(grantTypes())
//            .scopes(scopes())
//            .build();
//  }
//
//  @Bean
//  SecurityScheme apiKey(){
//    return new ApiKey("api_key", "api_key", "header");
//  }
//
//  List<AuthorizationScope> scopes(){
//    return newArrayList(
//            new AuthorizationScope("write:pets", "modify pets in your account"),
//            new AuthorizationScope("read:pets", "read your pets"));
//  }
//
//  List<GrantType> grantTypes(){
//    GrantType grantType = new ImplicitGrantBuilder()
//            .loginEndpoint(new LoginEndpoint("http://petstore.swagger.io/api/oauth/dialog"))
//            .build();
//    return newArrayList(grantType);
//  }
//    @Override
//    public void init(ServletConfig config) throws ServletException {
//        super.init(config);
//
//        Info info = new Info()
//                .title("Auth Service - RESTful API")
//                .description("This webpage describes the RESTful interface of the KIT Data Manager Auth Service. "
//                        + "This interface is needed in order to authenticate users and to authorize access to resources.")
//                .termsOfService("http://datamanager.kit.edu")
//                .version("1.0")
//                .contact(new Contact()
//                        .email("thomas.jejkal@kit.edu"))
//                .license(new License()
//                        .name("Apache 2.0")
//                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));
//
//        // ServletContext context = config.getServletContext();
//        Swagger swagger = new Swagger().info(info);
//      //  swagger.basePath("/auth-service-1.0-SNAPSHOT/api/v1");
//        swagger.basePath("/swagger");
//        //swagger.externalDocs(new ExternalDocs("Find out more about Swagger", "http://swagger.io"));
//        swagger.securityDefinition("api_key", new ApiKeyAuthDefinition("Authorization", In.HEADER));
//        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
//    }
}
