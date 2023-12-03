/*
 * Copyright 2022 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.configuration;

/**
 *
 * @author jejkal
 */
//@Configuration
//@ConditionalOnProperty(prefix = "repo.search", name="enabled", havingValue = "true", matchIfMissing = false)
public class ElasticConfiguration{// extends ElasticsearchConfiguration {


 /*   @Override
    public ClientConfiguration clientConfiguration() {
        //  HttpHeaders httpHeaders = new HttpHeaders();
        // httpHeaders.add("some-header", "on every request");
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200", "localhost:9291")
                //.usingSsl()
                //.withProxy("localhost:8888")
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(3))
                //.withDefaultHeaders(defaultHeaders)                                   
                //.withBasicAuth(username, password)                                    
                .withHeaders(() -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    return headers;
                })
                .withClientConfigurer(
                        ElasticsearchClients.ElasticsearchRestClientConfigurationCallback.from(clientBuilder -> {
                            return clientBuilder;
                        }))
                .build();
        return clientConfiguration;
    }*/
}
