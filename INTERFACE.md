# Interfaces Overview for base-repo

This document aims to answer questions on how to configure external dependencies and which public interfaces are offered by base-repo in a comprehensive way. 
It is meant to be used for getting an overview and guidance in addition to the official documentation, which is available at the official [base-repo Web page](https://kit-data-manager.github.io/webpage/base-repo/).

> **Note:**
> This document applies to the base-repo version it is shipped with. If you have a specific version running, please refer to `INTERFACE.md` of this particular release.

## External Dependencies

External dependencies are third-party services that are required for base-repo to work properly or that can be added optionally to provide additional functionality. Typically, external dependencies require
additional software to be installed and configured, before they can be included in the base-repo configuration, which is typically done via the main configuration file `application.properties`.

### Relational Database (mandatory)
A relational database is required by base-repo to store administrative metadata for resources and content information. If not configured properly, base-repo will fail to start.

#### Configuration:
  - H2 In-Memory (driver included, used for testing, not recommended for production) [Example](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/src/test/resources/test-config/application-test.properties#L31-L34)
  - H2 File-Based (driver included, used for basic Docker setup, not recommended for production) [Example](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-docker.properties#L17C1-L24)
  - PostgreSQL (driver included, requires a running PostgreSQL server, used for production) [PostgreSQL](https://www.postgresql.org/), [Example](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-default.properties#L38-L45)
  
> **Note**:
> Other relational databases, e.g., MariaDB, SQLite, or Oracle, may also work but require additional actions. To allow base-repo to connect, the source code repository must be cloned, an appropriate JDBC driver has to be added to `build.gradle`
> and base-repo has be be compiled. Proper JDBC drivers are typically provided on the database's Web page. Afterwards, the database can be configured in `application.properties` similar to PostgreSQL but with database-specific property naming. Please refer
> to the driver documentation for details.

#### Local Filesystem (mandatory)
Access to the local filesystem is required by base-repo to store and manage uploaded data. base-repo only needs access to a single folder, which can be located on the local hard drive or mounted, e.g., via NFS. 

#### Configuration:
   - see `application.properties` [Documentation1](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-default.properties#L137-L139), [Documentation2](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-default.properties#L154-L165)
   
### Messaging (optional)
AMQP-based messaging is an optional feature of base-repo, which allows base-repo to emit messages about creation, modification, and deletion events related to resources and content information. These messages can be received by registered consumers and processed in an asynchronous way.

#### Configuration:
   - RabbitMQ (dependencies included, requires a running RabbitMQ server) [RabbitMQ](https://www.rabbitmq.com/), [Documentation](https://kit-data-manager.github.io/webpage/base-repo/documentation/messaging-configuration.html)

### Enhanced Search (optional)
By default, base-repo offers basic search via RESTful API by example document or certain query parameters. Optionally, enhanced search via a search index can be enabled and used for fine-grained and facetted search operations.

#### Configuration:
   - Elasticsearch (serves as seach index, requires a running Elasticsearch server) [Elasticsearch] (https://www.elastic.co/de/elasticsearch/), [Documentation](https://kit-data-manager.github.io/webpage/base-repo/documentation/search-configuration.html), [Example](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-default.properties#L104-L107)
    
### Access Control (optional)
By default, base-repo itself is open for all kinds of operations, i.e., read and write, where write access should be restricted on the user interface level, e.g., by a password-protected area for critical operations. Optionally, authentication and authorization via 
JSON Web Tokens (JWT) issued by a Keycloak instance, can be configured.

#### Configuration:
   - Keycloak (serves as identity provider, requires a running Keycloak server) [Keycloak](https://www.keycloak.org/), [Documentation (TODO)](), [Example](https://github.com/kit-data-manager/base-repo/blob/4e90c6aeaced4715d419482f3cb127cddc85bd37/config/application-default.properties#L192-L201)
   
## Public Interfaces

Public Interfaces are used to access base-repo in order to obtain its contents, typically this happens via HTTP/REST. Depending on the interface, special clients or protocols must be used to access a specific public interface.

### HTTP / REST
The default way to access base-repo is via RESTful interfaces. They allow to create, update, and delete resources and content information, as well as upload and download data.

#### Documentation
    - [OpenAPI](https://kit-data-manager.github.io/webpage/base-repo/documentation/api-docs.html)
    - [Usage with Examples](https://kit-data-manager.github.io/webpage/base-repo/documentation/index.html)
    
    
    - Services known to connect to this interface:
        - Scripts (not available, yet)
        - Frontends (tAkita, frontend-collection/repo-management.html)
        - Webcomponent for rendering single ressources (not available, yet)
        - ...
- Elasticsearch Tunnel
    - A REST entpoint which tunnels all requests to the connected Elasticsearch instance. Part of the HTTP / REST interface.
    - Documentation: (https://kit-data-manager.github.io/webpage/base-repo/documentation/search-configuration.html)
    - Services known to connect to this interface:
        - frontend-collection/elastic-search-base-repo.html
- OAI-PMH
    - A XML-based interface for metadata harvesting implemented the full standard.
    - Source Code: https://git.scc.kit.edu/kitdatamanager/2.0/oai-pmh-controller-plugin
    - Setup: https://github.com/kit-data-manager/base-repo#enhanced-startup
    - Disclaimer: not tested for a long time, might be outdated/broken
- DOIP
    - A DOIP over TCP/IP implementation available as plugin. DOIP is a protocol sometimes used in the context of FAIR Digital Objects.
    - Source Code: https://git.scc.kit.edu/kitdatamanager/2.0/doip-plugin
    - Setup: https://github.com/kit-data-manager/base-repo#enhanced-startup
    - Disclaimer: not tested for a long time, might be outdated/broken
