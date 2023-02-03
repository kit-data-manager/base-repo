# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Security

- Bump postgresql from 42.4.1 to 42.5.0 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/68>
- Bump spring-cloud-starter-netflix-eureka-client from 3.1.3 to 3.1.4 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/69>
- Bump io.freefair.maven-publish-java from 6.5.0.3 to 6.5.1 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/70>
- Bump mockito-core from 4.6.1 to 4.8.0 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/72>
- Bump io.freefair.lombok from 6.5.0.3 to 6.5.1 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/71>
- Bump spring-messaging from 5.3.22 to 5.3.23 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/77>
- Bump org.owasp.dependencycheck from 7.1.1 to 7.2.1 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/80>
- Bump net.researchgate.release from 3.0.0 to 3.0.2 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/73>
- Bump nimbus-jose-jwt from 9.23 to 9.25.6 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/82>
- Bump org.springframework.boot from 2.7.2 to 2.7.4 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/83>
- Bump io.spring.dependency-management from 1.0.11.RELEASE to 1.0.14.RELEASE by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/84>
- Bump spring-security-config from 5.5.2 to 5.7.5 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/92>
- Bump spring-security-web from 5.7.2 to 5.7.5 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/91>
- Bump spring-cloud-starter-config from 3.1.3 to 3.1.5 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/93>
- Bump mockito-inline from 4.8.1 to 4.9.0 by @dependabot in <https://github.com/kit-data-manager/base-repo/pull/94>

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

[Unreleased]: https://github.com/kit-data-manager/base-repo/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/kit-data-manager/base-repo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/kit-data-manager/base-repo/compare/v1.0...v1.1.0
[1.0]: https://github.com/kit-data-manager/base-repo/compare/v0.0.1...v1.0
[0.0.1]: https://github.com/kit-data-manager/base-repo/releases/tag/v0.0.1
