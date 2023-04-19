#!/usr/bin/env python
# coding: utf-8
# pylint: disable=fixme
"""
Test suite configuration file.
"""
import pytest
from sqlalchemy.engine import URL
from nldi_crawler.db import DataAccessLayer

from nldi_crawler import src


@pytest.fixture(scope="session")
def dal():
    """Data Access Layer fixture to ease connections"""
    url = URL.create(
        "postgresql+psycopg2",
        username="nldi_schema_owner",
        password="changeMe",
        host="172.18.0.1",
        port=5432,
        database="nldi",
    )
    _dal = DataAccessLayer(url)
    yield _dal
    _dal.disconnect()


@pytest.fixture(scope="session")
def db_uri():
    return URL.create(
        "postgresql+psycopg2",
        username="nldi_schema_owner",
        password="changeMe",
        host="172.18.0.1",
        port=5432,
        database="nldi",
    )


@pytest.fixture(scope="session")
def fake_source():
    """pretend source to use instead of connecting to a database"""
    _src = src.CrawlerSource(
        crawler_source_id=13,
        source_name="geoconnex contribution demo sites",
        source_suffix="geoconnex-demo",
        source_uri="https://geoconnex-demo-pages.internetofwater.dev/collections/demo-gpkg/items?f=json&limit=10000",
        feature_id="fid",
        feature_name="GNIS_NAME",
        feature_uri="uri",
        feature_reach="NHDPv2ReachCode",
        feature_measure="NHDPv2Measure",
        ingest_type="reach",
        feature_type="hydrolocation",
    )
    return _src


@pytest.fixture(scope="session")
def crawler_repo():
    """in-memory table of crawler sources, faking an external source"""
    m = src.FakeSrcRepo()
    return m
