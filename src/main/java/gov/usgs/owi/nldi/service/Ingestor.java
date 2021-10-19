package gov.usgs.owi.nldi.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.postgis.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import gov.usgs.owi.nldi.dao.FeatureDao;
import gov.usgs.owi.nldi.dao.IngestDao;
import gov.usgs.owi.nldi.domain.CrawlerSource;
import gov.usgs.owi.nldi.domain.Feature;

@Component
public class Ingestor {
	private static final Logger LOG = LoggerFactory.getLogger(Ingestor.class);
	private IngestDao ingestDao;
	private FeatureDao featureDao;
	private HttpUtils httpUtils;

	public static final String GEOJSON_FEATURES = "features";
	public static final String GEOJSON_PROPERTIES = "properties";
	public static final String GEOJSON_GEOMETRY = "geometry";
	public static final String GEOJSON_TYPE = "type";
	public static final String GEOJSON_COORDINATES = "coordinates";

	public static final String GEOJSON_TYPE_POINT = "Point";

	@Autowired
	public Ingestor(IngestDao ingestDao, FeatureDao featureDao, HttpUtils httpUtils) {
		this.ingestDao = ingestDao;
		this.featureDao = featureDao;
		this.httpUtils = httpUtils;
	}

	public void ingest(int sourceID) throws ClientProtocolException, IOException {
		CrawlerSource crawlerSource = CrawlerSource.getDao().getById(sourceID);

		clearTempTable(crawlerSource);

		LOG.info("***** about to call " + crawlerSource.getSourceUri() + " *****");
		File file = httpUtils.callSourceSystem(crawlerSource);
		LOG.info("***** back from call to " + crawlerSource.getSourceUri() + " *****");

		processSourceData(crawlerSource, file);

		linkCatchment(crawlerSource);
		LOG.info("***** done linking data *****");

		installSourceData(crawlerSource);
	}

	public void clearTempTable(CrawlerSource crawlerSource) {
		ingestDao.clearTempTable(crawlerSource);
	}

	public void processSourceData(CrawlerSource crawlerSource, File file) throws JsonIOException, JsonSyntaxException, IOException {
		//We are expecting geojson 
		//with source specific properties registered in the crawler_source table.
		Gson gson = new GsonBuilder().create();
		int cnt = 0;

		JsonReader reader = new JsonReader(new FileReader(file));
		
		reader.beginObject();
		//First, get to the "features" array.
		Boolean foundFeatures = false;
		while (!foundFeatures) {
			JsonToken token = reader.peek();
			switch (token) {
				case NAME:
					String propName = reader.nextName();
					if (propName.equals(GEOJSON_FEATURES)) {
						foundFeatures = true;
					}
					break;
				case BEGIN_ARRAY: 
				case BEGIN_OBJECT:
					reader.skipValue();
					break;
				case STRING:
				case NUMBER:
					reader.nextString();
					break;
				case BOOLEAN:
					reader.nextBoolean();
					break;
				case NULL:
					reader.nextNull();
					break;
			}		
		}
		
		//Then process it.
		reader.beginArray();
		while (reader.hasNext()) {
			JsonObject jsonFeature = gson.fromJson(reader, JsonObject.class);
			featureDao.addFeature(buildFeature(crawlerSource, jsonFeature));
			cnt = cnt + 1;
		}
		reader.endArray();

		reader.close();
		LOG.info("Done processing features:" + cnt);
	}

	public void linkCatchment(CrawlerSource crawlerSource) {
		switch (crawlerSource.getIngestType()) {
		case point:
			ingestDao.linkPoint(crawlerSource);
			break;
		case reach:
			ingestDao.linkReachMeasure(crawlerSource);
			break;
		default:
			break;
		}
	}

	public void installSourceData(CrawlerSource crawlerSource) {
		ingestDao.installData(crawlerSource);
	}

	protected Iterator<JsonElement> getResponseIterator(Object obj) {
		Iterator<JsonElement> rtn = null;
		if (obj instanceof JsonObject) {
			JsonObject jsonObject = (JsonObject) obj;
			if (null != jsonObject
					&& jsonObject.has(GEOJSON_FEATURES) && jsonObject.get(GEOJSON_FEATURES).isJsonArray()) {
				rtn = jsonObject.getAsJsonArray(GEOJSON_FEATURES).iterator();
			}
		}
		return rtn;
	}

	protected Feature buildFeature(CrawlerSource crawlerSource, JsonObject jsonFeature) {
		Feature feature = new Feature();
		feature.setCrawlerSource(crawlerSource);
		feature.setPoint(getPoint(jsonFeature));

		if (null != crawlerSource) {
			JsonObject properties = getProperties(jsonFeature, crawlerSource.getFeatureId());
			feature.setIdentifier(getString(crawlerSource.getFeatureId(), properties));
			feature.setName(getString(crawlerSource.getFeatureName(), properties));
			feature.setUri(getString(crawlerSource.getFeatureUri(), properties));
			feature.setReachcode(getString(crawlerSource.getFeatureReach(), properties));
			feature.setMeasure(getBigDecimal(crawlerSource.getFeatureMeasure(), properties));
		}

		return feature;
	}

	protected JsonObject getProperties(JsonObject feature, String featureId) {
		JsonObject properties = null;

		if (null != feature
				&& feature.has(GEOJSON_PROPERTIES) && feature.get(GEOJSON_PROPERTIES).isJsonObject()) {
			properties = feature.getAsJsonObject(GEOJSON_PROPERTIES);

			if (null != featureId && !properties.has(featureId) && feature.has(featureId)) {
				properties.addProperty(featureId, feature.get(featureId).getAsString());
			}
		}

		return properties;
	}

	protected Point getPoint(JsonObject feature) {
		JsonObject geometry = null;
		JsonArray coordinates = null;
		Point point = null;

		if (null != feature
				&& feature.has(GEOJSON_GEOMETRY) && feature.get(GEOJSON_GEOMETRY).isJsonObject()) {
			geometry = feature.getAsJsonObject(GEOJSON_GEOMETRY);
		}

		if (null != geometry
				&& geometry.has(GEOJSON_TYPE) && GEOJSON_TYPE_POINT.equalsIgnoreCase(geometry.get(GEOJSON_TYPE).getAsString())
				&& geometry.has(GEOJSON_COORDINATES) && geometry.get(GEOJSON_COORDINATES).isJsonArray()) {
			coordinates = geometry.getAsJsonArray(GEOJSON_COORDINATES);
		}

		if (null != coordinates && 2 == coordinates.size()
				&& !coordinates.get(0).isJsonNull() && !coordinates.get(1).isJsonNull()) {
			try {
				point = new Point(coordinates.get(0).getAsDouble(), coordinates.get(1).getAsDouble());
				point.setSrid(Feature.DEFAULT_SRID);
			} catch (Throwable e) {
				LOG.info("Unable to determine point from coordinates:" + coordinates.get(0).toString() + "-" + coordinates.get(1).toString());
			}
		}
		return point;
	}

	protected String getString(String name, JsonObject jsonObject) {
		String value = null;

		if (null != jsonObject
				&& jsonObject.has(name) && jsonObject.get(name).isJsonPrimitive()) {
			value = jsonObject.get(name).getAsString();
		}

		return value;
	}

	protected BigDecimal getBigDecimal(String name, JsonObject jsonObject) {
		BigDecimal value = null;

		if (null != jsonObject
				&& jsonObject.has(name) && jsonObject.get(name).isJsonPrimitive()) {
			try {
				value = jsonObject.get(name).getAsBigDecimal();
			} catch (NumberFormatException e) {
				//Do nothing (return null) if not a valid BigDecimal value.
			}
		}

		return value;
	}

}
