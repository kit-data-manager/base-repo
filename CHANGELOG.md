# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
Added

Changed

Fixed

Security

## [1.5.8] - 2025-01-27
Changed
* ACL information no longer removed for Non-Administrator Users by @github-actions in https://github.com/kit-data-manager/base-repo/pull/381
* Resources with state GONE are now visible to owners and administrators (introduced in repo-core v1.2.5)

Fixed

Security
* Update dependency org.springframework.data:spring-data-elasticsearch to v5.4.2 by @renovate in https://github.com/kit-data-manager/base-repo/pull/372
* Update plugin org.owasp.dependencycheck to v12.0.1 by @renovate in https://github.com/kit-data-manager/base-repo/pull/374
* Update docker/build-push-action digest to ca877d9 by @renovate in https://github.com/kit-data-manager/base-repo/pull/378
* Update plugin io.freefair.maven-publish-java to v8.12 by @renovate in https://github.com/kit-data-manager/base-repo/pull/377
* Update plugin io.freefair.lombok to v8.12 by @renovate in https://github.com/kit-data-manager/base-repo/pull/376
* Update dependency gradle to v8.12.1 by @renovate in https://github.com/kit-data-manager/base-repo/pull/379
* Update plugin org.springframework.boot to v3.4.2 by @renovate in https://github.com/kit-data-manager/base-repo/pull/375
* Update springDocVersion to v2.8.4 by @renovate in https://github.com/kit-data-manager/base-repo/pull/382
* Update repo-core to v1.2.5


## [1.5.7] - 2025-01-17
Fixed
* Fixed ignored Elastic configuration in https://github.com/kit-data-manager/base-repo/pull/367

Security
* Update plugin io.freefair.lombok to v8.11 by @renovate in https://github.com/kit-data-manager/base-repo/pull/332
* Update codecov/codecov-action action to v5 by @renovate in https://github.com/kit-data-manager/base-repo/pull/326
* Update plugin io.freefair.maven-publish-java to v8.11 by @renovate in https://github.com/kit-data-manager/base-repo/pull/331
* Update dependency org.javers:javers-core to v7.7.0 by @renovate in https://github.com/kit-data-manager/base-repo/pull/335
* Update dependency org.springframework.restdocs:spring-restdocs-mockmvc to v3.0.3 by @renovate in https://github.com/kit-data-manager/base-repo/pull/338
* Update plugin io.spring.dependency-management to v1.1.7 by @renovate in https://github.com/kit-data-manager/base-repo/pull/359
* Update plugin org.asciidoctor.jvm.convert to v4.0.4 by @renovate in https://github.com/kit-data-manager/base-repo/pull/362
* Update dependency gradle to v8.12 by @renovate in https://github.com/kit-data-manager/base-repo/pull/361
* Update plugin net.researchgate.release to v3.1.0 by @renovate in https://github.com/kit-data-manager/base-repo/pull/364
* Update docker/build-push-action digest to 67a2d40 by @renovate in https://github.com/kit-data-manager/base-repo/pull/366
* Update dependency com.nimbusds:nimbus-jose-jwt to v10 by @renovate in https://github.com/kit-data-manager/base-repo/pull/365
* Update dependency org.springframework:spring-messaging to v6.2.2 by @renovate in https://github.com/kit-data-manager/base-repo/pull/357
* Update dependency org.springframework.cloud:spring-cloud-starter-netflix-eureka-client to v4.2.0 by @renovate in https://github.com/kit-data-manager/base-repo/pull/354
* Update docker/metadata-action digest to 8e1d546 by @renovate in https://github.com/kit-data-manager/base-repo/pull/369
* Update docker/login-action digest to 327cd5a by @renovate in https://github.com/kit-data-manager/base-repo/pull/368
* Update dependency de.codecentric:spring-boot-admin-starter-client to v3.4.1 by @renovate in https://github.com/kit-data-manager/base-repo/pull/347
* Update dependency org.springframework.cloud:spring-cloud-starter-config to v4.2.0 by @renovate in https://github.com/kit-data-manager/base-repo/pull/353
* Update dependency org.springframework.cloud:spring-cloud-gateway-mvc to v4.2.0 by @renovate in https://github.com/kit-data-manager/base-repo/pull/352
* Update springDocVersion to v2.8.3 by @renovate in https://github.com/kit-data-manager/base-repo/pull/344
* Update plugin org.springframework.boot to v3.4.1 by @renovate in https://github.com/kit-data-manager/base-repo/pull/343
* Update dependency org.springframework.data:spring-data-elasticsearch to v5.4.1 by @renovate in https://github.com/kit-data-manager/base-repo/pull/336
* Update dependency org.postgresql:postgresql to v42.7.5 by @renovate in https://github.com/kit-data-manager/base-repo/pull/370
* Update plugin org.owasp.dependencycheck to v12 by @renovate in https://github.com/kit-data-manager/base-repo/pull/371
* Update dependency edu.kit.datamanager:service-base to v1.3.3
* Update dependency edu.kit.datamanager:repo-core to v1.2.4

## [1.5.6] - 2024-11-10
Added
* Added configuration support for CORS, enhancing security and request handling.

Fixed
* Addressed specific issues related to Spring Cloud Gateway configuration.
* Improved error handling and logging in the Gradle wrapper scripts.

Security
* Updated various GitHub Actions in workflows for improved performance and functionality.
* Enhanced Gradle build configurations with updated plugin versions for better compatibility.
* Updated Spring Boot and related dependencies for enhanced stability and performance.
* Updated Gradle distribution and wrapper properties for better performance.

## [1.5.5] - 2024-04-22
Added
* Added new search endpoint '/api/v1/{index}/_search' to match default Elastic endpoints

Fixed
* Fixed accessibility of Swagger-UI ([#229](https://github.com/kit-data-manager/base-repo/issues/229))
* Fixed issue with internal authentication using JWT ([#261](https://github.com/kit-data-manager/service-base/pull/261))

Security
* Bump org.springframework.boot to 3.2.4
* Bump org.owasp.dependencycheck to 9.1.0
* Bump postgresql to 42.7.3
* Bump spring-messaging to 6.1.6
* Bump spring-data-elasticsearch to 5.2.5
* Bump spring-boot-admin-starter-client to 3.2.3
* Bump spring-cloud-gateway-mvc to 4.1.2
* Bump spring-cloud-starter-config to 4.1.1
* Bump spring-cloud-starter-netflix-eureka-client to 4.1.1
* Bump service-base to 1.3.1

## [1.5.4] - 2024-04-03
Added
* Allow builds outside Git repository, e.g., when building from release packages

Changed
* Pagination now returns HTTP 416 (RangeNotSatisfiable) if page number exceeds total pages

Fixed
* Added fix in default application.properties to make Elastic proxy work again
* Fixed issue disallowing OPTIONS requests outside browser

Security
* Bump gradle to 8.5
* Bump jacoco to 0.8.11
* Bump org.owasp.dependencycheck to 9.0.10
* Bump io.freefair.maven-publish-java to 8.6
* Bump org.springframework.boot to 3.2.4
* Bump spring-security-web to 6.2.1
* Bump spring-security-config to 6.2.1
* Bump spring-messaging to 6.1.4
* Bump spring-cloud-gateway-mvc to 4.1.2
* Bump spring-boot-admin-starter-client to 3.2.3
* Bump spring-cloud-starter-netflix-eureka-client to 4.1.1
* Bump spring-cloud-starter-config to 4.1.1
* Bump spring-data-elasticsearch to 5.2.4
* Bump io.freefair.lombok to 8.6
* Bump org.asciidoctor.jvm.convert to 4.0.2 
* Bump postgresql to 42.7.3
* Bump service-base to 1.2.1
* Bump repo-core to 1.2.2

## [1.5.3] - 2023-12-17
Fixed
* Added data folder creation to Docker build as workaround

## [1.5.2] - 2023-12-15
Fixed
* Fixed Docker build

Security
* Bump spring-boot to 3.2.0
* Bump spring-cloud-starter-config to 4.1.0
* Bump spring-cloud-starter-netflix-eureka-client to 4.1.0
* Bump spring-cloud-gateway-mvc to 4.1.0

## [1.5.1] - 2023-12-11
Fixed
* Added ElasticConfiguration again to support indexing and search

Security
* Bump spring-cloud-gateway-mvc to 4.0.8
* Bump spring-restdocs-mockmvc to 3.0.1
* Bump nimbus-jose-jwt to 9.37.3
* Bump dependency-management to 1.1.4
* Bump spring-messaging to 6.1.1
* Bump postgresql to 42.7.1

## [1.5.0] - 2023-12-03
Changed
* Upgrade to Spring Boot 3
* Java 17 is now mandatory
* Indexing support now requires Elastic 8+

Fixed
* Correctly ignore user-provided primary keys (https://github.com/kit-data-manager/base-repo/pull/131)
* Fixed issues while providing escaped slashes in request URL

Security
* Bump spring-boot to 3.1.5
* Bump spring-cloud-starter-config to 4.0.4
* Bump spring-cloud-starter-netflix-eureka-client to 4.0.3
* Bump spring-cloud-gateway-mvc to 4.0.6
* Bump spring-data-elasticsearch to 5.2.0
* Bump spring-messaging to 6.0.13
* Bump spring-security-web to 6.2.0
* Bump spring-security-config to 6.2.0
* Bump spring-boot-admin-starter-client to 3.1.8
* Bump io.spring.dependency-management to 1.1.0
* Bump io.freefair.lombok to 8.4.0
* Bump io.freefair.maven-publish-java to 8.4.0
* Bump org.owasp.dependencycheck to 9.0.2
* Bump net.researchgate.release to 3.0.2
* Bump com.gorylenko.gradle-git-properties to 2.4.1
* Bump javers-core to 7.0.0
* Bump httpclient to 4.5.14
* Bump postgresql to 42.6.0
* Bump nimbus-jose-jwt to 9.37
* Bump h2 to 2.2.224
* Bump postgresql to 42.7.0
* Bump spring-restdocs-mockmvc to 3.0.0
* Bump mockito-inline to 5.2.0
* Bump json-utils to 0.1.8
* Bump repo-core to 1.2.1
* Bump service-base to 1.2.0

## [1.4.0] - 2023-03-17

Added
- Creating resources from Zenodo JSON metadata has been added and is triggered by providing Content-Type 'application/vnd.zenodo.org+json' at POST /api/v1/dataresources/.
- Added support for using PIDs (prefix/suffix) as resource id without escaping while accessing them, e.g., GET /api/v1/dataresources/<prefix>/<suffix> 

Changed

- ContentInformation metadata now returns own ETags different from the ETag of the parent resource.
- Creating resources from DataCite JSON metadata is now triggered by providing Content-Type 'application/vnd.datacite.org+json' at POST /api/v1/dataresources/.
- The allowed size of description content has been changed from 255 to 10240 characters (see 'Migration Remarks'). 

Fixed

- Creating resource from DataCite JSON metadata has been fixed.
- MediaType detection and providing mediaType by user now finally works for ContentInformation.

Security

* Bump service-base from 1.1.0 to 1.1.1
* Bump repo-core from 1.1.1 to 1.1.2

### Migration Remarks

For existing databases, a manual update is required to adjust the column size. 
The query may depend on the used database system, for PostgreSQL this would be: 

```
alter table description alter column description type character varying(10240);
``` 

## [1.3.0] - 2023-02-03

Security

* Bump spring-security-config from 5.5.2 to 5.7.3 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/58
* Bump postgresql from 42.4.1 to 42.5.0 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/68
* Bump spring-cloud-starter-netflix-eureka-client from 3.1.3 to 3.1.4 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/69
* Bump io.freefair.maven-publish-java from 6.5.0.3 to 6.5.1 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/70
* Bump io.freefair.lombok from 6.5.0.3 to 6.5.1 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/71
* Bump spring-messaging from 5.3.22 to 5.3.23 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/77
* Bump org.owasp.dependencycheck from 7.1.2 to 7.2.1 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/80
* Bump net.researchgate.release from 3.0.1 to 3.0.2 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/73
* Bump nimbus-jose-jwt from 9.24.3 to 9.25.6 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/82
* Bump io.spring.dependency-management from 1.0.13.RELEASE to 1.0.14.RELEASE by @dependabot in https://github.com/kit-data-manager/base-repo/pull/84
* Bump spring-security-config from 5.7.4 to 5.7.5 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/92
* Bump spring-security-web from 5.7.4 to 5.7.5 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/91
* Bump spring-cloud-starter-config from 3.1.4 to 3.1.5 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/93
* Bump mockito-inline from 4.8.1 to 4.9.0 by @dependabot in https://github.com/kit-data-manager/base-repo/pull/94
* Bump Gradle from 7.4.2 to 7.6 by @VolkerHartmann in https://github.com/kit-data-manager/base-repo/pull/110
* Bump org.springframework.boot from 2.7.3 to 2.7.5 
* Bump javers from 6.6.5 to 6.8.0 
* Bump service-base from 1.0.4 to 1.1.0
* Bump repo-core from 1.0.3 to 1.1.1

Added

- Added CSRF and CORS configuration support to application.properties
- Added support for indexing and searching for DataResources via Elastic
- Added new endpoint at GET /api/v1/dataresources for content type 'application/tabulator+json' for direct support of listing in [Tabulator](https://tabulator.info/)

Changed

- Attempts to write DataResources if repository is in readOnly mode now returns HTTP 403 (FORBIDDEN) instead of HTTP 503 (SERVICE_UNAVAILABLE)
- Attempts to write DataResources without permissions now returns HTTP 403 (FORBIDDEN) instead of HTTP 401 (UNAUTHORIZED)

Fixed

- MediaType detection and providing mediaType by user now properly works for ContentInformation

## [1.2.0] - 2022-08-04

Security

- Update to h2 2.1.214:
  - Please migrate your database if you want to update base-repo while using h2!
    See: <https://h2database.com/html/migration-to-v2.html>

Added

- File versioning support for content elements
- Added Keycloak support for authentication
- Creating new DataResources accepts now official DataCite documents by providing Content-Type header with value 'application/datacite+json'

 Changed

- Additional field in content information metadata for providing the used versioning (none, simple, ocfl)
- Moved endpoint for audit information to base path /api/v1/audit/
- More consistent content-type handling
- Externalized documentation to [https://kit-data-manager.github.io/webpage/base-repo.html](https://kit-data-manager.github.io/webpage/base-repo.html)
- Update to service-base 1.0.4
- Update to repo-core 1.0.3
- Update to io.freefair.lombok 6.5.0.3
- Update to org.owasp.dependencycheck 7.1.1
- Update to spring-boot 2.7.2
- Update to spring-doc 1.6.9
- Update to spring-cloud 3.1.3
- Update to spring-messaging 5.3.22
- Update to spring-restdocs-mockmvc 2.0.6.RELEASE
- Update to postgresql 42.4.0

Fixed

- Fixed minor issues in queries of search endpoint if providing a search template resource, e.g., queries involving ResourceType or ACL information

Removed

- Property 'repo.audit.enabled' is no longer needed. Instead, 'repo.plugin.versioning' should be either set to 'none' or 'simple' to disable/enable versioning of both, metadata and data.

## [1.1.0] - 2020-12-17

Changed

- Truncating service-assigned times to milliseconds for compatibility reasons
- Change of messaging property names including documentation
- Update to service-base 0.2.0
- Update to generic-message-consumer 0.2.0

Fixed

- Fixed wrong HATEOAS links for search endpoint

## [1.0] - 2020-08-18

Changed

- Update dependency service-base to version 0.1.1

## [0.0.1] - 2020-08-11

Added

- First public version

[Unreleased]: https://github.com/kit-data-manager/base-repo/compare/v1.5.8...HEAD
[1.5.8]: https://github.com/kit-data-manager/base-repo/compare/v1.5.7...v1.5.8
[1.5.7]: https://github.com/kit-data-manager/base-repo/compare/v1.5.6...v1.5.7
[1.5.6]: https://github.com/kit-data-manager/base-repo/compare/v1.5.5...v1.5.6
[1.5.5]: https://github.com/kit-data-manager/base-repo/compare/v1.5.4...v1.5.5
[1.5.4]: https://github.com/kit-data-manager/base-repo/compare/v1.5.3...v1.5.4
[1.5.3]: https://github.com/kit-data-manager/base-repo/compare/v1.5.2...v1.5.3
[1.5.2]: https://github.com/kit-data-manager/base-repo/compare/v1.5.1...v1.5.2
[1.5.1]: https://github.com/kit-data-manager/base-repo/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/kit-data-manager/base-repo/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/kit-data-manager/base-repo/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/kit-data-manager/base-repo/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/kit-data-manager/base-repo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/kit-data-manager/base-repo/compare/v1.0...v1.1.0
[1.0]: https://github.com/kit-data-manager/base-repo/compare/v0.0.1...v1.0
[0.0.1]: https://github.com/kit-data-manager/base-repo/releases/tag/v0.0.1
