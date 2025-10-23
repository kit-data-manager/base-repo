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
package edu.kit.datamanager.repo.configuration;

import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import edu.kit.datamanager.security.filter.NoAuthenticationFilter;
import edu.kit.datamanager.security.filter.PublicAuthenticationFilter;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 *
 * @author jejkal
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    private static final String[] AUTH_WHITELIST_SWAGGER_UI = {
        // -- Swagger UI v2
        "/v2/api-docs",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        // -- Swagger UI v3 (OpenAPI)
        "/v3/api-docs/**",
        "/swagger-ui/**"
    // other public endpoints of your API may be appended to this array
    };

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private Optional<KeycloakTokenFilter> keycloaktokenFilterBean;

    public WebSecurityConfig() {
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http.authorizeHttpRequests(
                authorize -> authorize.
                        requestMatchers(HttpMethod.OPTIONS).permitAll().
                        requestMatchers(EndpointRequest.to(
                                InfoEndpoint.class,
                                HealthEndpoint.class
                        )).permitAll().
                        requestMatchers(new AntPathRequestMatcher("/actuator/**")).hasAnyRole("ADMIN", "ACTUATOR").
                        requestMatchers(EndpointRequest.toAnyEndpoint()).hasAnyRole("ADMIN", "SERVICE_WRITE").
                        requestMatchers(new AntPathRequestMatcher("/oaipmh")).permitAll().
                        requestMatchers(new AntPathRequestMatcher("/static/**")).permitAll().
                        requestMatchers(new AntPathRequestMatcher("/api/v1/search")).permitAll().
                        requestMatchers(AUTH_WHITELIST_SWAGGER_UI).permitAll().
                        anyRequest().authenticated()
        ).
                httpBasic(Customizer.withDefaults()).
                cors(cors -> cors.configurationSource(corsConfigurationSource())).
                sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        logger.info("CSRF disabled!");
        httpSecurity = httpSecurity.csrf(csrf -> csrf.disable());
       
        logger.info("Adding 'NoAuthenticationFilter' to authentication chain.");
        if (keycloaktokenFilterBean.isPresent()) {
            logger.info("Add keycloak filter!");
            httpSecurity.addFilterAfter(keycloaktokenFilterBean.get(), BasicAuthenticationFilter.class);
            logger.info("Add public authentication filter!");
            httpSecurity = httpSecurity.addFilterAfter(new PublicAuthenticationFilter(applicationProperties.getJwtSecret()), BasicAuthenticationFilter.class);
        }
        if (!applicationProperties.isAuthEnabled()) {
            logger.info("Authentication is DISABLED. Adding 'NoAuthenticationFilter' to authentication chain.");
            AuthenticationManager defaultAuthenticationManager = http.getSharedObject(AuthenticationManager.class);
            httpSecurity = httpSecurity.addFilterAfter(new NoAuthenticationFilter(applicationProperties.getJwtSecret(), defaultAuthenticationManager), BasicAuthenticationFilter.class);
        } else {
            logger.info("Authentication is ENABLED.");
        }

        httpSecurity.headers(headers -> headers.cacheControl(cache -> cache.disable()));

        return httpSecurity.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    public CorsConfigurationSource  corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern(applicationProperties.getAllowedOriginPattern());
        config.setAllowedHeaders(Arrays.asList(applicationProperties.getAllowedHeaders()));
        config.setAllowedMethods(Arrays.asList(applicationProperties.getAllowedMethods()));
        config.setExposedHeaders(Arrays.asList(applicationProperties.getExposedHeaders()));

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
