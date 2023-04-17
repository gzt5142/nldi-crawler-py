#!/usr/bin/env python
# coding: utf-8
# pylint: disable=fixme
"""
Test source table functions.
"""
import pytest
import httpx
from sqlalchemy import URL


from nldi_crawler import src

def test_fake_repo():
    """test fake repo"""
    repo = src.FakeSrcRepo()
    srcs = repo.get_list()
    assert len(srcs) == 2

    _s = repo.get(12)
    assert _s.crawler_source_id == 12

def test_csv_repo():
    """test csv repo"""
    tsvsource=r'https://raw.githubusercontent.com/internetofwater/nldi-db/gt-097-source-table-fixes/liquibase/changeLogs/nldi/nldi_data/update_crawler_source/crawler_source.tsv'
    repo = src.CSVRepo(tsvsource)
    srcs = repo.get_list()
    assert len(srcs) >= 1
    _s = repo.get(12)
    assert _s.crawler_source_id == 12

def test_csv_repo_bad_url():
    with pytest.raises(ValueError):
        tsvsource="https://raw.githubusercontent.com/no/such/source.tsv"
        repo = src.CSVRepo(tsvsource)
    with pytest.raises(httpx.UnsupportedProtocol):
        tsvsource="file://no/such/source.json"
        repo = src.CSVRepo(tsvsource)

def test_json_repo():
    jsonsource="https://raw.githubusercontent.com/gzt5142/nldi-crawler-py/gt-src-repopattern/tests/sources.json"
    repo = src.JSONRepo(jsonsource)
    srcs = repo.get_list()
    assert len(srcs) >= 1
    _s = repo.get(12)
    assert _s.crawler_source_id == 12

def test_json_repo_bad_url():
    with pytest.raises(ValueError):
        jsonsource="https://raw.githubusercontent.com/no/such/source.json"
        repo = src.JSONRepo(jsonsource)
    with pytest.raises(httpx.UnsupportedProtocol):
        jsonsource="/no/such/source.json"
        repo = src.JSONRepo(jsonsource)

def test_sql_repo():
    url = URL.create(
            "postgresql+psycopg2",
            username="nldi_schema_owner",
            password="changeMe",
            host="172.18.0.1",
            port=5432,
            database="nldi",
        )
    repo = src.SQLRepo(url)
    srcs = repo.get_list()
    assert len(srcs) >= 1

