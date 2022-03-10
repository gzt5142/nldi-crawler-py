# NLDI Crawler

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3c8a9f8eef79461e86d37036830a2b49)](https://app.codacy.com/app/usgs_wma_dev/nldi-crawler?utm_source=github.com&utm_medium=referral&utm_content=ACWI-SSWD/nldi-crawler&utm_campaign=Badge_Grade_Settings)
[![Build Status](https://travis-ci.org/ACWI-SSWD/nldi-crawler.svg?branch=master)](https://travis-ci.org/ACWI-SSWD/nldi-crawler)
[![codecov](https://codecov.io/gh/ACWI-SSWD/nldi-crawler/branch/master/graph/badge.svg)](https://codecov.io/gh/ACWI-SSWD/nldi-crawler)

The Crawler is used to ingest event data and link it to the network. The only requirement is that the source system is able to provide GeoJSON via a web request. A database table (nldi_data.crawler_source) contains metadata about the GeoJSON. We can link events to the network via latitude/longitude coordinates or NHDPlus reach and measure.

## Contributing:

Contributions can be made via [pull request to this file](https://github.com/ACWI-SSWD/nldi-db/blob/master/liquibase/changeLogs/nldi/nldi_data/update_crawler_source/crawler_source.tsv).

Current nldi_data.crawler_souce table fields:

crawler_source_id | source_name | source_suffix | source_uri | feature_id | feature_name | feature_uri | feature_reach | feature_measure | ingest_type
--- | --- | --- | --- | --- | --- | --- | --- | --- | ---
An integer used to identify the source when starting the crawler source. | A human-oriented name for the source. | The suffix to use in NLDI service urls to identify the source. | A uri the crawler can use to retrieve source data to be indexed by the crawling method. | The attribute in the returned data used to identify the feature for use in NLDI service urls. | A human readable name used to label the source feature. | A uri that can be used to access information about the feature. | **Conditionally Optional** The attribute in the source feature data where the crawler can find a reachcode. | **Conditionally Optional** The attribute in the source feature data where the crawler can find a measure to be used with the reachcode. (strings are parsed into numbers if measure is represented as a string) | Either `reach` or `point`. If `reach` then the feature_reach and feature_measure fields must be populated.

A command-line argument is used to initiate the ingest process for a data source:

```bash
java -jar target/nldi-crawler-0.4-SNAPSHOT.jar <crawler_source_id>
```

### Developer Environment

[nldi-db](https://travis-ci.org/ACWI-SSWD/nldi-db) contains everything you need to set up a development database environment. It includes data for the Yahara River in Wisconsin.

To run the project you will need to create the file application.yml in the project's root directory and add the following:
```
nldiDbHost: hostNameOfDatabase
nldiDbPort: portNumberForDatabase
nldiDbUsername: dbUserName
nldiDbPassword: dbPassword
```
To run:
```
% mvn spring-boot:run
```

This project has some integration testing against the database. The "package" goal of the maven command will stop the build before running them.
To set up the project for running the integration tests, add the following to your maven settings.xml file (the values below will work with the
nldi-db docker container running on the same machine as the tests):

```
<profiles>
  <profile>
    <id>default</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <nldi.url>jdbc:postgresql://127.0.0.1:5433/nldi</nldi.url>
        <nldi.dbUsername>nldi</nldi.dbUsername>
        <nldi.dbPassword>nldi</nldi.dbPassword>
        <nldi.dbUnitUsername>nldi</nldi.dbUnitUsername>
        <nldi.dbUnitPassword>nldi</nldi.dbUnitPassword>
      </properties>
  </profile>
</profiles>
```

If running integration tests without maven, you may specify the properties in the file,
application-it.yml. See the maven-failsafe-plugin configuration in the pom.xml
for the mapping of properties to varables.

### Running via Docker Compose

To run via Docker Compose, create a `secrets.env` file with the following format:

```
nldiDbHost: hostNameOfDatabase
nldiDbPort: portNumberForDatabase
nldiDbUsername: dbUserName
nldiDbPassword: dbPassword
```

... And run with:

```bash
docker-compose run -e CRAWLER_SOURCE_ID=<crawler_source_id> nldi-crawler
```

### Running via JAR File
[RUNNING.md](RUNNING.md)
