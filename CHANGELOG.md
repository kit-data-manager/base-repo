# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Security
- Update to h2 2.1.212:
  - Please migrate your database if you want to update base-repo while using h2!
    See: https://h2database.com/html/migration-to-v2.html 

### Added
- File versioning support for content elements
- Added Keycloak support for authentication

### Changed
- Additional field in content information metadata for providing the used versioning (none, simple, ocfl)
- Moved endpoint for audit information to base path /api/v1/audit/
- More consistent content-type handling
- Update to repo-core 1.0.2
- Update to service-base 1.0.1
- Externalized documentation to [https://kit-data-manager.github.io/webpage/base-repo.html](https://kit-data-manager.github.io/webpage/base-repo.html)

### Fixed
- Fixed minor issues in queries of search endpoint if providing a search template resource, e.g., queries involving ResourceType or ACL information

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

[Unreleased]: https://github.com/kit-data-manager/base-repo/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/kit-data-manager/base-repo/compare/v1.0...v1.1.0
[1.0.0]: https://github.com/kit-data-manager/base-repo/compare/v0.0.1...v1.0
[0.0.1]: https://github.com/kit-data-manager/base-repo/releases/tag/v0.0.1

