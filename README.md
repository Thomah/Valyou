# Les Projets Cagnottes - core
[![Release](https://github.com/les-projets-cagnottes/core/workflows/Release/badge.svg)](https://github.com/les-projets-cagnottes/core/actions?query=workflow%3ARelease)
[![Integration](https://github.com/les-projets-cagnottes/core/workflows/Integration/badge.svg)](https://github.com/les-projets-cagnottes/core/actions?query=workflow%3AIntegration)
[![Known Vulnerabilities](https://snyk.io/test/github/les-projets-cagnottes/core/badge.svg)](https://snyk.io/test/github/les-projets-cagnottes/core)

## Prerequisites

- Java 13 - [Download here](https://jdk.java.net/13/)
- PostgreSQL 12 - [Download here](https://www.postgresql.org/download/)
- A PostgreSQL Client - [Download DBeaver here](https://dbeaver.io/download/)
- Maven 3 - [Download here](https://maven.apache.org/download.cgi)

## Getting Started

1. Create a PostgreSQL database matching the configuration in `src/main/resources/application.properties`
2. Create the following environment variables :
```bash
LPC_SLACK_CLIENT_ID=<ask-the-team>
LPC_SLACK_CLIENT_SECRET=<ask-the-team>
LPC_WEB_URL=http://localhost:4200
HTTP_PROXY=http://<host>:<port>
```
3. Run the following command :
```bash
mvn clean install spring-boot:run
```