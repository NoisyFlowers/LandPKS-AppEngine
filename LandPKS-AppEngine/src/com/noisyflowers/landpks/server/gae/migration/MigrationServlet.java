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
 * com.noisyflowers.landpks.server.gae.migration
 * MigrationServlet.java
 */

package com.noisyflowers.landpks.server.gae.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
//import com.google.appengine.repackaged.org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
//TODO: We're not supposed to reference classes in repackaged.  I tried to use the original classes from a separate jar.  
//This worked on my local machine but not when deployed.  Some library conflict I guess.  Still need to work this out.  
//In the meantime, fully specifying path at declaration prevents compile error.
//import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.model.Segment;
import com.noisyflowers.landpks.server.gae.model.StickSegment;
import com.noisyflowers.landpks.server.gae.model.Transect;
import com.noisyflowers.landpks.server.gae.reports.ReportServlet;

public class MigrationServlet extends HttpServlet {
	private static final String TAG = ReportServlet.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);

	private static final String PLOT_URL = "http://localhost:8080/lpks/rest/plot";
	private static final String TRANSECT_URL = "http://localhost:8080/lpks/rest/transect";
	private static final String CHARSET = "UTF-8";		

	//TODO: All these maps could be loaded from a props file or through the REST API
	public static Map<String, Integer> soilRockFragmentRangeCategories = new HashMap<String, Integer>();
	static {
		soilRockFragmentRangeCategories.put("0-15%", 1);
		soilRockFragmentRangeCategories.put("15-35%", 2);
		soilRockFragmentRangeCategories.put("35-60%", 3);
		soilRockFragmentRangeCategories.put(">60%", 4);
	}

	public static Map<String, Integer> soilTextureCategories = new HashMap<String, Integer>();
	static {
		soilTextureCategories.put("SAND", 1);
		soilTextureCategories.put("LOAMY SAND", 2);
		soilTextureCategories.put("SANDY LOAM", 3);
		soilTextureCategories.put("SILT LOAM", 4);
		soilTextureCategories.put("LOAM", 5);
		soilTextureCategories.put("SANDY CLAY LOAM", 6);
		soilTextureCategories.put("SILTY CLAY LOAM", 7);
		soilTextureCategories.put("CLAY LOAM", 8);
		soilTextureCategories.put("SANDY CLAY", 9);
		soilTextureCategories.put("SILTY CLAY", 10);
		soilTextureCategories.put("CLAY", 11);
	}

	public static Map<String, Integer> horizonNameCategories = new LinkedHashMap<String, Integer>();
	static {
		horizonNameCategories.put("0-1cm", 1);
		horizonNameCategories.put("1-10cm", 2);
		horizonNameCategories.put("10-20cm", 3);
		horizonNameCategories.put("20-50cm", 4);
		horizonNameCategories.put("50-70cm", 5);
		horizonNameCategories.put("70-100cm", 6);
		horizonNameCategories.put("100-120cm", 7);
	}
	
	public static Map<String, Integer> landCoverCategories = new HashMap<String, Integer>();
	static {
		landCoverCategories.put("tree cover, >25% canopy", 1);
		landCoverCategories.put("shrub cover, >50% cover", 2);
		landCoverCategories.put("grassland, >50% grass", 3);
		landCoverCategories.put("savanna, 10-20% tree cover", 4);
		landCoverCategories.put("garden/mixed", 5);
		landCoverCategories.put("cropland",6);
		landCoverCategories.put("developed", 7);
		landCoverCategories.put("barren, <5% veg cover", 8);
		landCoverCategories.put("water", 9);
	}
	
	public static Map<String, Integer> slopeShapeCategories = new HashMap<String, Integer>();
	static {
		slopeShapeCategories.put("CONCAVE,CONCAVE", 1);
		slopeShapeCategories.put("CONCAVE,CONVEX", 2);
		slopeShapeCategories.put("CONCAVE,LINEAR", 3);
		slopeShapeCategories.put("CONVEX,CONCAVE", 4);
		slopeShapeCategories.put("CONVEX,CONVEX", 5);
		slopeShapeCategories.put("CONVEX,LINEAR", 6);
		slopeShapeCategories.put("LINEAR,CONCAVE", 7);
		slopeShapeCategories.put("LINEAR,CONVEX", 8);
		slopeShapeCategories.put("LINEAR,LINEAR", 9);
	}

	public static Map<String, Integer> slopeCategories = new HashMap<String, Integer>();
	static {
		slopeCategories.put("flat (0-2%)", 1);
		slopeCategories.put("gentle (3-5%)", 2);
		slopeCategories.put("moderate (6-10%)", 3);
		slopeCategories.put("rolling (11-15%)", 4);
		slopeCategories.put("hilly (16-30%)", 5);
		slopeCategories.put("steep (31-60%)", 6);
		slopeCategories.put("very steep (>60%)", 7);
	}

	public static Map<String, Integer> directionCategories = new HashMap<String, Integer>();
	static {
		directionCategories.put("NORTH", 1);
		directionCategories.put("EAST", 2);
		directionCategories.put("SOUTH", 3);
		directionCategories.put("WEST", 4);
	}

	public static Map<String, Integer> canopyHeightCategories = new HashMap<String, Integer>();
	static {
		canopyHeightCategories.put("<10cm", 1);
		canopyHeightCategories.put("10-50cm", 2);
		canopyHeightCategories.put("50cm-1m", 3);
		canopyHeightCategories.put("1-2m", 4);
		canopyHeightCategories.put("2-3m", 5);
		canopyHeightCategories.put(">3m", 6);
	}

	public static Map<String, Integer> transectSegmentRangeCategories = new HashMap<String, Integer>();
	static {
		transectSegmentRangeCategories.put("5m", 1);
		transectSegmentRangeCategories.put("10m", 2);
		transectSegmentRangeCategories.put("15m", 3);
		transectSegmentRangeCategories.put("20m", 4);
		transectSegmentRangeCategories.put("25m", 5);
	}

	public static Map<String, Integer> stickSegmentRangeCategories = new HashMap<String, Integer>();
	static {
		stickSegmentRangeCategories.put("10cm", 1);
		stickSegmentRangeCategories.put("30cm", 2);
		stickSegmentRangeCategories.put("50cm", 3);
		stickSegmentRangeCategories.put("70cm", 4);
		stickSegmentRangeCategories.put("90cm", 5);
	}

	private void insert(String url, String content) throws ServletException, IOException{
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		connection.setConnectTimeout(30000);
		connection.setRequestProperty("Accept-Charset", CHARSET);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		out.write(content);
		out.close();
		InputStream response = connection.getInputStream();			
		
		if (((HttpURLConnection)connection).getResponseCode() != HttpURLConnection.HTTP_CREATED) {
			throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode());
		}	
	
	}
	
	
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		log.info("doGet, enter");

		EntityManager mgr = null;
		Cursor cursor = null;
		List<Plot> plots = null;
		List<Transect> transects = null;
        PrintWriter writer = resp.getWriter();
		
		try {
			mgr = getEntityManager();
			Query query = mgr.createQuery("select from Plot as plot");
			plots = (List<Plot>) query.getResultList();
		} finally {
			mgr.close();
		}
 
		com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper mapper = new com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper();
		mapper.setSerializationInclusion(com.google.appengine.repackaged.org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL);
		mapper.configure(com.google.appengine.repackaged.org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);			
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
		for (Plot plot: plots) {
			writer.println("Processing plot " + plot.getID() + "...");
			com.noisyflowers.lpks.persistence.model.Plot dtoPlot = loadDTOPlot(plot);
			Writer strWriter = new StringWriter();
			mapper.writeValue(strWriter, dtoPlot);
			String plotJSON = strWriter.toString();
			
			/***
			HttpURLConnection connection = (HttpURLConnection) new URL(PLOT_URL).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setConnectTimeout(30000);
			connection.setRequestProperty("Accept-Charset", CHARSET);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(plotJSON);
			out.close();
			InputStream response = connection.getInputStream();	
						
			if (((HttpURLConnection)connection).getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode() + " from LPKS Rest API for plot: " + plot.getID());
			}	
			***/
			
			try {
				insert(PLOT_URL, plotJSON);
			} catch (Exception eX) {
				log.log(Level.SEVERE, "Problem processing plot " + plot.getID(), eX);
			}
		}

		
		try {
			mgr = getEntityManager();
			Query query = mgr.createQuery("select from Transect as transect");
			transects = (List<Transect>) query.getResultList();
		} finally {
			mgr.close();
		}

		for (Transect transect: transects) {
			writer.println("Processing transect " + transect.getID() + "...");
			com.noisyflowers.lpks.persistence.model.Transect dtoTransect = loadDTOTransect(transect);
			Writer strWriter = new StringWriter();
			mapper.writeValue(strWriter, dtoTransect);
			String transectJSON = strWriter.toString();
			
			/***
			HttpURLConnection connection = (HttpURLConnection) new URL(TRANSECT_URL).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setConnectTimeout(30000);
			connection.setRequestProperty("Accept-Charset", CHARSET);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(transectJSON);
			out.close();
			InputStream response = connection.getInputStream();			
			
			if (((HttpURLConnection)connection).getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode() + " from LPKS Rest API for transect: " + transect.getID());
			}	
			***/
			
			try {
				insert(TRANSECT_URL, transectJSON);
			} catch (Exception eX) {
				log.log(Level.SEVERE, "Problem processing transect " + transect.getID(), eX);
			}
		}
		
		writer.println("Done.");

    }
    
    private com.noisyflowers.lpks.persistence.model.Plot loadDTOPlot(Plot plot) {
    	com.noisyflowers.lpks.persistence.model.Plot dtoPlot = new com.noisyflowers.lpks.persistence.model.Plot();
    	
    	com.noisyflowers.lpks.persistence.model.LandcoverCategory lC = new com.noisyflowers.lpks.persistence.model.LandcoverCategory();
    	com.noisyflowers.lpks.persistence.model.SlopeShapeCategory sSC = new com.noisyflowers.lpks.persistence.model.SlopeShapeCategory();
    	com.noisyflowers.lpks.persistence.model.SlopeCategory sC = new com.noisyflowers.lpks.persistence.model.SlopeCategory();
    	
    	
    	dtoPlot.setCity(plot.getCity());
    	dtoPlot.setCropErosion(plot.getCropErosion());
    	dtoPlot.setCropProductivity(plot.getCropProductivity());
    	dtoPlot.setFlooding(plot.isFlooding());
    	dtoPlot.setGrassErosion(plot.getGrassErosion());
    	dtoPlot.setGrassProductivity(plot.getGrassProductivity());
    	dtoPlot.setGrazed(plot.isGrazed());
    	dtoPlot.setId(plot.getID());
    	//if (plot.getLandCover() != null) {
    	//	lC.setId(LandCover.nameLookup.get(plot.getLandCover()).ordinal() + 1);
        //	dtoPlot.setLandcoverCategory(lC);
    	//}
    	if (plot.getLandCover() != null) {
    		lC.setId(landCoverCategories.get(plot.getLandCover()));
        	dtoPlot.setLandcoverCategory(lC);
    	}
    	
    	dtoPlot.setLandscapeEastPhotoUrl(plot.getLandscapeEastPhotoURL());
    	dtoPlot.setLandscapeNorthPhotoUrl(plot.getLandscapeNorthPhotoURL());
    	dtoPlot.setLandscapeSouthPhotoUrl(plot.getLandscapeSouthPhotoURL());
    	dtoPlot.setLandscapeWestPhotoUrl(plot.getLandscapeWestPhotoURL());
    	dtoPlot.setLatitude(plot.getLatitude());
    	dtoPlot.setLongitude(plot.getLongitude());
    	dtoPlot.setModifiedDate(plot.getModifiedDate());
    	dtoPlot.setName(plot.getName());
    	dtoPlot.setOrganization(plot.getOrganization());
    	dtoPlot.setRecorderName(plot.getRecorderName());
    	
    	/***
    	if (plot.getSlope() != null) {
	    	Slope s = Slope.nameLookup.get(plot.getSlope());
	    	if (s != null) {
	    		sC.setId(s.ordinal() + 1);
	    		dtoPlot.setSlopeCategory(sC);
	    	} else {
	    		dtoPlot.setSlopeMeasured(Double.parseDouble(plot.getSlope()));
	    	}
    	}
    	***/
    	if (plot.getSlope() != null) {
	    	Integer slopeCategorieIndex = slopeCategories.get(plot.getSlope());
	    	if (slopeCategorieIndex != null) {
	    		sC.setId(slopeCategorieIndex);
	    		dtoPlot.setSlopeCategory(sC);
	    	} else {
	    		dtoPlot.setSlopeMeasured(Double.parseDouble(plot.getSlope()));
	    	}
    	}
    	
    	/***
    	if (plot.getSlopeShape() != null) {
	    	sSC.setId(SlopeShape.nameLookup.get(plot.getSlopeShape()).ordinal() + 1);
	    	dtoPlot.setSlopeShapeCategory(sSC);
    	}
    	***/
    	if (plot.getSlopeShape() != null) {
	    	sSC.setId(slopeShapeCategories.get(plot.getSlopeShape()));
	    	dtoPlot.setSlopeShapeCategory(sSC);
    	}

    	dtoPlot.setSoilPitPhotoUrl(plot.getSoilPitPhotoURL());
    	dtoPlot.setSoilSamplesPhotoUrl(plot.getSoilSamplesPhotoURL());
    	dtoPlot.setSurfaceCracking(plot.isSurfaceCracking());
    	dtoPlot.setSurfaceSalt(plot.isSurfaceSalt());
    	
    	List<com.noisyflowers.lpks.persistence.model.SoilHorizon> sHList = new ArrayList<com.noisyflowers.lpks.persistence.model.SoilHorizon>();
    	/***
    	for(HorizonName h : HorizonName.values()) {
        	java.lang.reflect.Method method;
        	String rockMethodName = "getRockFragmentForSoilHorizon" + (h.ordinal() + 1);
        	String colorMethodName = "getColorForSoilHorizon" + (h.ordinal() + 1);
        	String textureMethodName = "getTextureForSoilHorizon" + (h.ordinal() + 1);
        	String rockStr = null;
        	Integer colorInt = null;
        	String textureStr = null;
        	try {
        	  method = plot.getClass().getMethod(rockMethodName);
        	  rockStr = (String) method.invoke(plot);
        	  method = plot.getClass().getMethod(colorMethodName);
        	  colorInt = (Integer) method.invoke(plot);
        	  method = plot.getClass().getMethod(textureMethodName);
        	  textureStr = (String) method.invoke(plot);
        	} catch (Exception e) {
        		//log
        	}
        	com.noisyflowers.lpks.persistence.model.SoilHorizon sH = new com.noisyflowers.lpks.persistence.model.SoilHorizon();
        	com.noisyflowers.lpks.persistence.model.SoilHorizonDepth sHD = new com.noisyflowers.lpks.persistence.model.SoilHorizonDepth();
        	com.noisyflowers.lpks.persistence.model.SoilRockFragmentCategory sRFD = new com.noisyflowers.lpks.persistence.model.SoilRockFragmentCategory();
        	com.noisyflowers.lpks.persistence.model.SoilTextureCategory sTC = new com.noisyflowers.lpks.persistence.model.SoilTextureCategory();
        	sHD.setId(h.ordinal() + 1); 
        	sH.setSoilHorizonDepth(sHD);
        	if (colorInt != null) {
        		sH.setColor(colorInt);
        	}
        	if (rockStr != null) {
	        	sRFD.setId(SoilRockFragmentRange.nameLookup.get(rockStr).ordinal() + 1);
	        	sH.setSoilRockFragmentCategory(sRFD);
        	}
        	if (textureStr != null) {
        		sTC.setId(SoilTexture.nameLookup.get(textureStr).ordinal() + 1);
        		sH.setSoilTextureCategory(sTC);
        	}
        	//dtoPlot.addSoilHorizon(sH);
        	sHList.add(sH);
        }
        ***/
        for(Integer horizonIndex : horizonNameCategories.values()) {
        	java.lang.reflect.Method method;
        	String rockMethodName = "getRockFragmentForSoilHorizon" + (horizonIndex);
        	String colorMethodName = "getColorForSoilHorizon" + (horizonIndex);
        	String textureMethodName = "getTextureForSoilHorizon" + (horizonIndex);
        	String rockStr = null;
        	Integer colorInt = null;
        	String textureStr = null;
        	try {
        	  method = plot.getClass().getMethod(rockMethodName);
        	  rockStr = (String) method.invoke(plot);
        	  method = plot.getClass().getMethod(colorMethodName);
        	  colorInt = (Integer) method.invoke(plot);
        	  method = plot.getClass().getMethod(textureMethodName);
        	  textureStr = (String) method.invoke(plot);
        	} catch (Exception e) {
        		//log
        	}
        	com.noisyflowers.lpks.persistence.model.SoilHorizon sH = new com.noisyflowers.lpks.persistence.model.SoilHorizon();
        	com.noisyflowers.lpks.persistence.model.SoilHorizonDepth sHD = new com.noisyflowers.lpks.persistence.model.SoilHorizonDepth();
        	com.noisyflowers.lpks.persistence.model.SoilRockFragmentCategory sRFD = new com.noisyflowers.lpks.persistence.model.SoilRockFragmentCategory();
        	com.noisyflowers.lpks.persistence.model.SoilTextureCategory sTC = new com.noisyflowers.lpks.persistence.model.SoilTextureCategory();
        	sHD.setId(horizonIndex); 
        	sH.setSoilHorizonDepth(sHD);
        	if (colorInt != null) {
        		sH.setColor(colorInt);
        	}
        	if (rockStr != null) {
	        	sRFD.setId(soilRockFragmentRangeCategories.get(rockStr));
	        	sH.setSoilRockFragmentCategory(sRFD);
        	}
        	if (textureStr != null) {
        		sTC.setId(soilTextureCategories.get(textureStr));
        		sH.setSoilTextureCategory(sTC);
        	}
        	//dtoPlot.addSoilHorizon(sH);
        	sHList.add(sH);
        }
        
        dtoPlot.setSoilHorizons(sHList);
        
    	return dtoPlot;
    }
    
    private com.noisyflowers.lpks.persistence.model.Transect loadDTOTransect(Transect transect) {
    	com.noisyflowers.lpks.persistence.model.Transect dtoTransect = new com.noisyflowers.lpks.persistence.model.Transect();
    
    	com.noisyflowers.lpks.persistence.model.Direction d = new com.noisyflowers.lpks.persistence.model.Direction();
    	com.noisyflowers.lpks.persistence.model.Plot p = new com.noisyflowers.lpks.persistence.model.Plot();

    	if (transect.getDirection() != null) {
    		d.setId(directionCategories.get(transect.getDirection()));
    		dtoTransect.setDirection(d);
    	}
    	dtoTransect.setId(transect.getID());
    	dtoTransect.setModifiedDate(transect.getModifiedDate());
    	p.setId(transect.getSiteID());
    	dtoTransect.setPlot(p);
    	
    	List<com.noisyflowers.lpks.persistence.model.TransectSegment> transectSegmentList = new ArrayList<com.noisyflowers.lpks.persistence.model.TransectSegment>();
    	for (Segment segment : transect.getSegments()) {
        	com.noisyflowers.lpks.persistence.model.TransectSegment tS = new com.noisyflowers.lpks.persistence.model.TransectSegment();
    		tS.setBasalGap(segment.getBasalGap());
    		tS.setCanopyGap(segment.getCanopyGap());
        	com.noisyflowers.lpks.persistence.model.CanopyHeightCategory cHC = new com.noisyflowers.lpks.persistence.model.CanopyHeightCategory();
        	if (segment.getCanopyHeight() != null) {
        		cHC.setId(canopyHeightCategories.get(segment.getCanopyHeight()));
        		tS.setCanopyHeightCategory(cHC);
        	}
        	try {tS.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(segment.getDate()));} catch (Exception e){}  //TODO: this better
        	List<com.noisyflowers.lpks.persistence.model.ObservedSpecy> speciesList = new ArrayList<com.noisyflowers.lpks.persistence.model.ObservedSpecy>();
        	for (String species : segment.getSpeciesList()) {
            	com.noisyflowers.lpks.persistence.model.ObservedSpecy oS = new com.noisyflowers.lpks.persistence.model.ObservedSpecy();
        		oS.setSpecies(species);
        		speciesList.add(oS);
        	}
        	tS.setObservedSpecies(speciesList); 
        	tS.setSpeciesDensity1(segment.getSpecies1Density());
        	tS.setSpeciesDensity2(segment.getSpecies2Density());
        	List<com.noisyflowers.lpks.persistence.model.StickSegment> stickSegmentList = new ArrayList<com.noisyflowers.lpks.persistence.model.StickSegment>();
        	for (StickSegment stickSegment : segment.getStickSegments()) {
            	com.noisyflowers.lpks.persistence.model.StickSegment sS = new com.noisyflowers.lpks.persistence.model.StickSegment();
		    	for (StickSegment.Cover cover : StickSegment.Cover.values()) {
		    		//String setMethod = "set" + cover.name.replaceAll("[^a-zA-Z0-9]", "");	    		
		    		//String setMethod = "set" + cover.name.replaceAll("[^a-zA-Z0-9]([a-z])", "$1".toUpperCase());	
		    		//setMethod = setMethod.replaceAll("[^a-zA-Z0-9]", "");	    		
		    		String setMethod = "set" + cover.name;	
		    		Pattern pA = Pattern.compile("[^a-zA-Z0-9]([a-z])");
		    		Matcher m = pA.matcher(setMethod);
		    		StringBuffer sb = new StringBuffer();
		    		while (m.find()) {
		    		    m.appendReplacement(sb, m.group(1).toUpperCase());
		    		}
		    		m.appendTail(sb);
		    		setMethod = sb.toString();
		        	java.lang.reflect.Method method;
	            	try {
	              	  method = sS.getClass().getMethod(setMethod, Boolean.TYPE);
	              	  method.invoke(sS, stickSegment.covers[cover.ordinal()]);
	              	} catch (Exception e) {
	              		//log
	              	}
		    	}
            	com.noisyflowers.lpks.persistence.model.StickSegmentRange sSR = new com.noisyflowers.lpks.persistence.model.StickSegmentRange();
            	//if (stickSegment.g() != null) {
            		sSR.setId(stickSegment.getSegmentIndex() + 1); //TODO: Hmm, not sure why I did this differently than other range indices
            		sS.setStickSegmentRange(sSR);
            	//}

		    	stickSegmentList.add(sS);
        	}
        	tS.setStickSegments(stickSegmentList); //TODO: loop through
        	
        	com.noisyflowers.lpks.persistence.model.Transect t = new com.noisyflowers.lpks.persistence.model.Transect();
        	t.setId(dtoTransect.getId());
        	tS.setTransect(t); 
        	com.noisyflowers.lpks.persistence.model.TransectSegmentRange tSR = new com.noisyflowers.lpks.persistence.model.TransectSegmentRange();
        	if (segment.getRange() != null) {
        		tSR.setId(transectSegmentRangeCategories.get(segment.getRange()));
        		tS.setTransectSegmentRange(tSR);
        	}
        	
        	transectSegmentList.add(tS);
    	}
    	
    	dtoTransect.setTransectSegments(transectSegmentList);
    	
    	return dtoTransect;
    }
    
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}

}
