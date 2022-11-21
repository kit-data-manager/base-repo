/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.kit.datamanager.repo.configuration;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.http.HttpHeaders;

/**
 *
 * @author jejkal
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "edu.kit.datamanager.repo")
@ComponentScan(basePackages = {"edu.kit.datamanager"})
public class ElasticConfiguration {

    @Bean
    public RestHighLevelClient client() {
        //required for compatibility to Elastic 8.X ... might not work and should be removed with spring-boot 3.X
        HttpHeaders compatibilityHeaders = new HttpHeaders();
        compatibilityHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
        compatibilityHeaders.add("Content-Type", "application/vnd.elasticsearch+json;compatible-with=7");

        ClientConfiguration clientConfiguration
                = ClientConfiguration.builder()
                        .connectedTo("localhost:9200")
                        .withDefaultHeaders(compatibilityHeaders)  
                        .build();

        return RestClients.create(clientConfiguration).rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }

}
