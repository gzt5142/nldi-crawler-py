# Workflow

The crawler CLI will bulk-download feature data from pre-defined sources. The sequence is a follows:

## Sequence Diagram

```mermaid
%%{init: {
    "theme": "default",
    "mid-width": 2500,
    "max-width": 5000,
    "sequence": {"showSequenceNumbers": true }
    }
}%%
sequenceDiagram
    Crawler->>+NLDI-DB: Get Source Information
    Note left of NLDI-DB: SELECT * FROM nldi_data.crawler_source
    NLDI-DB-->>-Crawler: Sources table

    Crawler->>+Feature_Source: Request Features
    Note left of Feature_Source: HTTP GET ${crawler_source.source_uri}
    Feature_Source-->>-Crawler: GeoJSON FeatureCollection
    loop foreach feature in Collection
        Crawler-->>+Crawler: ORM
        Note right of Crawler: Parses and maps feature to SQL
        Crawler->>-NLDI-DB: Add to feature table
        Note left of NLDI-DB: INSERT INTO nldi_data.features
    end
    Crawler->>+NLDI-DB: Relate Features

    %NLDI-DB-->>-Crawler: Success
```
## Annotations

1) Connect to NLDI master database, requesting the list of configured feature sources.
2) Returns a list of feature sources.  The crawler can either:
   * list all sources and exit
   * Proceed to 'crawl' one of the sources in the table
3) For the identified feature source, make a GET request via HTTP.  The URI is taken from the `crawler_sources` table.
4) The feature source returns GeoJSON.  Among the returned data is a list of 'features'.

For each feature:

    5) Use the ORM to map the feature data to the schema reflected from the `features` table
    6) Insert the new feature to the master NLDI database


7) "Relate" features -- build the relationships matching features to their adjacent features in the NLDI topology.