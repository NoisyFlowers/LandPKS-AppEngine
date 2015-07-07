/**
 * 
 * Copyright 2014 Noisy Flowers LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * com.noisyflowers.landpks.server.gae.processing
 * SoilDataCruncher.java
 */

package com.noisyflowers.landpks.server.gae.processing;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;

import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.labs.repackaged.org.json.JSONTokener;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.dal.PlotEndpoint;
import com.noisyflowers.landpks.server.gae.model.AWHCMapping;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.model.ProductivityAndErosionMapping;
import com.noisyflowers.landpks.server.gae.util.Constants.RockFragmentRange;
import com.noisyflowers.landpks.server.gae.util.Constants.SoilHorizonName;
import com.noisyflowers.landpks.server.gae.util.Constants.SoilTexture;

public class SoilDataCruncher {
	private static final String TAG = SoilDataCruncher.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);

	private HashMap<AWHCKey, Double> awhcMap = new HashMap<AWHCKey, Double>();
	
	public SoilDataCruncher() {
		//TODO: Maybe check existence of config records and run config if not present
	}
	
	public Double calculateTotalAWHC(Plot plot) {
		double horizon1AWHC = 0;
		double horizon2AWHC = 0;
		double horizon3AWHC = 0;
        EntityManager em = EMF.get().createEntityManager();
        Double awhcTotal = null;
        List<AWHCMapping> awhcList = null;
        AWHCMapping awhcMapping = null;
        try {
        	Query q = em.createQuery("select a from AWHCMapping a where a.texture = :t and a.rockFragment = :r");
        	q.setParameter("t", plot.getTextureForSoilHorizon1().toUpperCase());
        	q.setParameter("r", plot.getRockFragmentForSoilHorizon1().toUpperCase());
        	awhcMapping = (AWHCMapping)q.getSingleResult();
        	if (awhcMapping != null) 
        		horizon1AWHC = awhcMapping.getAwhcValue() * 10;
        	q.setParameter("t", plot.getTextureForSoilHorizon2().toUpperCase());
        	q.setParameter("r", plot.getRockFragmentForSoilHorizon2().toUpperCase());
        	awhcMapping = (AWHCMapping)q.getSingleResult();
        	if (awhcMapping != null) 
        		horizon2AWHC = awhcMapping.getAwhcValue() * 10;
        	q.setParameter("t", plot.getTextureForSoilHorizon3().toUpperCase());
        	q.setParameter("r", plot.getRockFragmentForSoilHorizon3().toUpperCase());
        	awhcMapping = (AWHCMapping)q.getSingleResult();
        	if (awhcMapping != null) 
        		horizon3AWHC = awhcMapping.getAwhcValue() * 20;
        } catch (Exception eX) {
        	return null;
        } finally {
            em.close();
        }
        
        return mapToNearestAWHC(horizon1AWHC + horizon2AWHC + horizon3AWHC, plot);
	}
	
	private Double mapToNearestAWHC(double inAWHC, Plot plot) {
		Double nearestAWHC = null;
		List<Double> aWHCList = new ArrayList<Double>();
        EntityManager em = EMF.get().createEntityManager();
        try {
        	Query q = em.createQuery("select pe.awhcTotal from ProductivityAndErosionMapping pe where pe.surfaceCracked = :sc and pe.topLayerSoilTexture = :ts");
    		q.setParameter("sc", plot.isSurfaceCracking() ? "Y" : "N");
        	q.setParameter("ts", plot.getTextureForSoilHorizon1().toUpperCase());
        	aWHCList = (List<Double>)q.getResultList();
        } catch (Exception eX) {
        	nearestAWHC = null; //TODO: log?
        } finally {
            em.close();
        }
        
        if (aWHCList != null && aWHCList.size() > 0) {
			nearestAWHC = aWHCList.get(0);
			for (double anAWHC : aWHCList) {
				if (Math.abs(inAWHC - anAWHC) < Math.abs(inAWHC - nearestAWHC)) {
					nearestAWHC = anAWHC;
				}
			}
        }
		
		return nearestAWHC;
	}
	
	public Plot calculateResults(Plot plot) throws ServletException{
		try {
			String url = "http://128.123.177.13/APEX/SiteAnalysis";
			String charset = "UTF-8";		
			
			String query = "?" +
					"name=" + URLEncoder.encode(plot.getID(), charset) + "&" +
					"test_plot=" + (plot.isTestPlot() == null ? "" : URLEncoder.encode(plot.isTestPlot().toString(), charset)) + "&" +
					"recorder_name=" + (plot.getRecorderName() == null ? "" : URLEncoder.encode(plot.getRecorderName(), charset)) + "&" +
					"organization=" + (plot.getOrganization() == null ? "" : URLEncoder.encode( plot.getOrganization(), charset)) + "&" +
					"latitude=" + plot.getLatitude() + "&" +
					"longitude=" + plot.getLongitude() + "&" +
					"city=" + (plot.getCity() == null ? "" : URLEncoder.encode(plot.getCity(), charset)) + "&" +
					"modified_date=" + (plot.getModifiedDate() == null ? "" : URLEncoder.encode(plot.getModifiedDate().toString(), charset)) + "&" +
					"land_cover=" + (plot.getLandCover() == null ? "" : URLEncoder.encode(plot.getLandCover(), charset)) + "&" +
					"grazed=" + plot.isGrazed() + "&" +
					"flooding=" + plot.isFlooding() + "&" +
					"slope=" + (plot.getSlope() == null ? "" : URLEncoder.encode(plot.getSlope(), charset)) + "&" +
					"slope_shape=" + (plot.getSlopeShape() == null ? "" : URLEncoder.encode(plot.getSlopeShape(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_1=" + (plot.getRockFragmentForSoilHorizon1() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon1(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_2=" + (plot.getRockFragmentForSoilHorizon2() ==  null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon2(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_3=" + (plot.getRockFragmentForSoilHorizon3() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon3(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_4=" + (plot.getRockFragmentForSoilHorizon4() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon4(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_5=" + (plot.getRockFragmentForSoilHorizon5() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon5(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_6=" + (plot.getRockFragmentForSoilHorizon6() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon6(), charset)) + "&" +
					"rock_fragment_for_soil_horizon_7=" + (plot.getRockFragmentForSoilHorizon7() == null ? "" : URLEncoder.encode(plot.getRockFragmentForSoilHorizon7(), charset)) + "&";
					
			Integer color = plot.getColorForSoilHorizon1();
			String colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_1=" + colorStr + "&";
			color = plot.getColorForSoilHorizon2();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_2=" + colorStr + "&";
			color = plot.getColorForSoilHorizon3();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_3=" + colorStr + "&";
			color = plot.getColorForSoilHorizon4();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_4=" + colorStr + "&";
			color = plot.getColorForSoilHorizon5();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_5=" + colorStr + "&";
			color = plot.getColorForSoilHorizon6();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_6=" + colorStr + "&";
			color = plot.getColorForSoilHorizon7();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			query += "color_for_soil_horizon_7=" + colorStr + "&";
			
			query += "texture_for_soil_horizon_1=" +(plot.getTextureForSoilHorizon1() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon1(), charset)) + "&" +
					"texture_for_soil_horizon_2=" + (plot.getTextureForSoilHorizon2() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon2(), charset)) + "&" +
					"texture_for_soil_horizon_3=" + (plot.getTextureForSoilHorizon3() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon3(), charset)) + "&" +
					"texture_for_soil_horizon_4=" + (plot.getTextureForSoilHorizon4() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon4(), charset)) + "&" +
					"texture_for_soil_horizon_5=" + (plot.getTextureForSoilHorizon5() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon5(), charset)) + "&" +
					"texture_for_soil_horizon_6=" + (plot.getTextureForSoilHorizon6() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon6(), charset)) + "&" +
					"texture_for_soil_horizon_7=" + (plot.getTextureForSoilHorizon7() == null ? "" : URLEncoder.encode(plot.getTextureForSoilHorizon7(), charset)) + "&" +
					"surface_cracking=" + plot.isSurfaceCracking() + "&" +
					"surface_salt=" + plot.isSurfaceSalt() + "&" +
					"landscape_north_photo_url=" + (plot.getLandscapeNorthPhotoURL() == null ? "" : URLEncoder.encode(plot.getLandscapeNorthPhotoURL(), charset)) + "&" +
					"landscape_east_photo_url=" + (plot.getLandscapeEastPhotoURL() == null ? "" : URLEncoder.encode(plot.getLandscapeEastPhotoURL(), charset)) + "&" +
					"landscape_south_photo_url=" + (plot.getLandscapeSouthPhotoURL() == null ? "" : URLEncoder.encode(plot.getLandscapeSouthPhotoURL(), charset)) + "&" +
					"landscape_west_photo_url=" + (plot.getLandscapeWestPhotoURL() == null ? "" : URLEncoder.encode(plot.getLandscapeWestPhotoURL(), charset)) + "&" +
					"soil_pit_photo_url=" + (plot.getSoilPitPhotoURL() == null ? "" : URLEncoder.encode(plot.getSoilPitPhotoURL(), charset)) + "&" +
					"soil_samples_photo_url=" + (plot.getSoilSamplesPhotoURL() == null ? "" : URLEncoder.encode(plot.getSoilSamplesPhotoURL(), charset));
				
			log.info("Calling analytic service using: " + url + query);
			URLConnection connection = new URL(url + query).openConnection();
			connection.setConnectTimeout(30000);
			connection.setRequestProperty("Accept-Charset", charset);
			InputStream response = connection.getInputStream();			
			
			if (((HttpURLConnection)connection).getResponseCode() != HttpURLConnection.HTTP_OK) {
				String reason = "";
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
					String json = reader.readLine();
					JSONObject jObj = new JSONObject(json);
					reason = jObj.getString("reason");
				} catch (Exception eX) {}
				throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode() + "(reason: " + reason + ") from analytic web service for query: " + url + query);
			}
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
			String json = reader.readLine();
			JSONObject jObj = new JSONObject(json);
			try { plot.setGrassProductivity(jObj.getDouble("grass_productivity"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGrassErosion(jObj.getDouble("grass_erosion"));} catch (Exception eX) {log.info(eX.toString());}
			//try { plot.setMaizeProductivity(jObj.getDouble("crop_productivity"));} catch (Exception eX) {log.info(eX.toString());}
			//try { plot.setMaizeErosion(jObj.getDouble("crop_erosion"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setCropProductivity(jObj.getDouble("crop_productivity"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setCropErosion(jObj.getDouble("crop_erosion"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGdalElevation(jObj.getDouble("gdal_elevation"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGdalAridityIndex(jObj.getDouble("gdal_aridity_index"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGdalFaoLgp(jObj.getString("gdal_fao_lgp"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setAwcSoilProfile(jObj.getDouble("awc_soil_profile_awc"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setAverageAnnualPrecipitation(jObj.getDouble("climate_precip_average_annual"));} catch (Exception eX) {log.info(eX.toString());}
			
			try { 
				List<Double> monthlyPrecip = new ArrayList<Double>();
				monthlyPrecip.add(jObj.getDouble("climate_precip_january"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_february"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_march"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_april"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_may"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_june"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_july"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_august"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_september"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_october"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_november"));
				monthlyPrecip.add(jObj.getDouble("climate_precip_december"));
				plot.setMonthlyPrecipitation(monthlyPrecip);
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				List<Double> monthlyAvgTemperature = new ArrayList<Double>();
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_january"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_february"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_march"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_april"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_may"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_june"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_july"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_august"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_september"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_october"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_november"));
				monthlyAvgTemperature.add(jObj.getDouble("climate_avg_temp_december"));
				plot.setMonthlyAvgTemperature(monthlyAvgTemperature);
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				List<Double> monthlyMaxTemperature = new ArrayList<Double>();
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_january"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_february"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_march"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_april"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_may"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_june"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_july"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_august"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_september"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_october"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_november"));
				monthlyMaxTemperature.add(jObj.getDouble("climate_max_temp_december"));
				plot.setMonthlyMaxTemperature(monthlyMaxTemperature);
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				List<Double> monthlyMinTemperature = new ArrayList<Double>();
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_january"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_february"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_march"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_april"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_may"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_june"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_july"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_august"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_september"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_october"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_november"));
				monthlyMinTemperature.add(jObj.getDouble("climate_min_temp_december"));
				plot.setMonthlyMinTemperature(monthlyMinTemperature);
			} catch (Exception eX) {log.info(eX.toString());}

			
		} catch (Exception eX) {
			log.severe(eX.toString());
			throw new ServletException("Unable to process analytics for " + plot.getID());
		}
		
        return plot;	
	}
	
	private List<Double> loadListByMonth(JSONObject jObj) throws JSONException{
		List<Double> retList = new ArrayList<Double>();
		retList.add(jObj.getDouble("january"));
		retList.add(jObj.getDouble("february"));
		retList.add(jObj.getDouble("march"));
		retList.add(jObj.getDouble("april"));
		retList.add(jObj.getDouble("may"));
		retList.add(jObj.getDouble("june"));
		retList.add(jObj.getDouble("july"));
		retList.add(jObj.getDouble("august"));
		retList.add(jObj.getDouble("september"));
		retList.add(jObj.getDouble("october"));
		retList.add(jObj.getDouble("november"));
		retList.add(jObj.getDouble("december"));
		
		return retList;
	}
	
	public Plot getQuickClimate(Plot plot) throws ServletException{
		try {
			String url = "http://128.123.177.13:8081/quick_climate";
			String charset = "UTF-8";		
			
			String query = "?latitude=" + plot.getLatitude() + "&longitude=" + plot.getLongitude();

			log.info("Calling quick climate service using: " + url + query);
			URLConnection connection = new URL(url + query).openConnection();
			connection.setConnectTimeout(30000);
			connection.setRequestProperty("Accept-Charset", charset);
			InputStream response = connection.getInputStream();			
			
			if (((HttpURLConnection)connection).getResponseCode() != HttpURLConnection.HTTP_OK) {
				String reason = "";
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
					String json = reader.readLine();
					JSONObject jObj = new JSONObject(json);
					reason = jObj.getString("reason");
				} catch (Exception eX) {}
				throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode() + "(reason: " + reason + ") from analytic web service for query: " + url + query);
			}
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
			//String json = reader.readLine();
			StringBuilder sB = new StringBuilder();
			boolean done = false;
			while (!done) {
				String line = reader.readLine();
				if (line == null) {
					done = true;
				} else {
					sB.append(line);
				}
			}
			String json = sB.toString();
			JSONObject jObj = new JSONObject(json);
			
			JSONObject climate = jObj.getJSONObject("climate");
			
			try { 
				JSONObject precip = climate.getJSONObject("precipitation");
				plot.setAverageAnnualPrecipitation(precip.getDouble("annual"));
				plot.setMonthlyPrecipitation(loadListByMonth(precip));
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				plot.setMonthlyAvgTemperature(loadListByMonth(climate.getJSONObject("average_temperature")));
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				plot.setMonthlyMaxTemperature(loadListByMonth(climate.getJSONObject("max_temperature")));
			} catch (Exception eX) {log.info(eX.toString());}

			try { 
				plot.setMonthlyMinTemperature(loadListByMonth(climate.getJSONObject("min_temperature")));
			} catch (Exception eX) {log.info(eX.toString());}

			JSONObject gdal = jObj.getJSONObject("geospatial_data");
			try { plot.setGdalElevation(gdal.getDouble("gdal_elevation"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGdalAridityIndex(gdal.getDouble("gdal_aridity_index"));} catch (Exception eX) {log.info(eX.toString());}
			try { plot.setGdalFaoLgp(gdal.getString("gdal_fao_lgp"));} catch (Exception eX) {log.info(eX.toString());}

		} catch (Exception eX) {
			log.severe(eX.toString());
			throw new ServletException("Unable to get quick climate data for " + plot.getLatitude() + ", " + plot.getLongitude());
		}
		return plot;
	}
	
}
