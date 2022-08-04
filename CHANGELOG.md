# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Security

### Added

### Changed

### Fixed

### Removed

## [1.2.0] - 2022-08-04
### Security
- Update to h2 2.1.214:
  - Please migrate your database if you want to update base-repo while using h2!
    See: https://h2database.com/html/migration-to-v2.html 

### Added
- File versioning support for content elements
- Added Keycloak support for authentication
- Creating new DataResources accepts now official DataCite documents by providing Content-Type header with value 'application/datacite+json' 

### Changed
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

### Fixed
- Fixed minor issues in queries of search endpoint if providing a search template resource, e.g., queries involving ResourceType or ACL information

### Removed
- Property 'repo.audit.enabled' is no longer needed. Instead, 'repo.plugin.versioning' should be either set to 'none' or 'simple' to disable/enable versioning of both, metadata and data.

## [1.1.0] - 2020-12-17
### Changed
- Truncating service-assigned times to milliseconds for compatibility reasons
- Change of messaging property names including documentation
- Update to service-base 0.2.0
- Update to generic-message-consumer 0.2.0

### Fixed
- Fixed wrong HATEOAS links for search endpoint

## [1.0] - 2020-08-18
### Changed
- Update dependency service-base to version 0.1.1

## [0.0.1] - 2020-08-11
### Added
- First public version

### Changed
- none

### Removed
- none

### Deprecated
- none

### Fixed
- none

### Security
- none

[Unreleased]: https://github.com/kit-data-manager/base-repo/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/kit-data-manager/base-repo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/kit-data-manager/base-repo/compare/v1.0...v1.1.0
[1.0]: https://github.com/kit-data-manager/base-repo/compare/v0.0.1...v1.0
[0.0.1]: https://github.com/kit-data-manager/base-repo/releases/tag/v0.0.1

