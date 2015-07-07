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
 * com.noisyflowers.landpks.server.gae.dal
 * PlotEndpoint.java
 */

package com.noisyflowers.landpks.server.gae.dal;


import static com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.servlet.ServletException;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.blobstore.UploadOptions;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.UserServicePb.GetOAuthUserResponse;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.processing.SoilDataCruncher;
import com.noisyflowers.landpks.server.gae.util.Constants;
import com.noisyflowers.landpks.server.gae.util.Credentials;

@Api(name = "plotendpoint", 
	 namespace = @ApiNamespace(ownerDomain = "noisyflowers.com", ownerName = "noisyflowers.com", packagePath = "landpks.server.gae.model"),
	 clientIds = {Credentials.INSTALLED_CLIENT_ID, Credentials.WEB_CLIENT_ID, Credentials.ANDROID_CLIENT_ID_0, Credentials.ANDROID_CLIENT_ID_1, Credentials.ANDROID_CLIENT_ID_2, Credentials.ANDROID_CLIENT_ID_3, Credentials.ANDROID_CLIENT_ID_4, Credentials.ANDROID_CLIENT_ID_5, API_EXPLORER_CLIENT_ID },
	 audiences = {Credentials.INSTALLED_CLIENT_ID, Credentials.WEB_CLIENT_ID, Credentials.ANDROID_CLIENT_ID_0, Credentials.ANDROID_CLIENT_ID_1, Credentials.ANDROID_CLIENT_ID_2, Credentials.ANDROID_CLIENT_ID_3, Credentials.ANDROID_CLIENT_ID_4, Credentials.ANDROID_CLIENT_ID_5 },
	 scopes = {Credentials.EMAIL_SCOPE, Credentials.PROFILE_SCOPE}
	)
public class PlotEndpoint {
	private static final String TAG = PlotEndpoint.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);
	
	private static final String BUCKET_NAME = "silicon-bivouac-496.appspot.com";
		
	/**
	 * This method lists all the entities inserted in datastore for current user.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listPlot")
	public CollectionResponse<Plot> listPlot(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit, 
			@Nullable @Named("afterDate") Date afterDate, 
			@Nullable @Named("allUsers") Boolean allUsers, 
			User user) throws OAuthRequestException, UnauthorizedException,  IOException {

		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		} 
		
		if (allUsers != null) {
			ApiProxy.Environment environment = ApiProxy.getCurrentEnvironment();
			GetOAuthUserResponse oAuthResponse = (GetOAuthUserResponse) environment.getAttributes().get("com.google.appengine.api.oauth.OAuthService.get_oauth_user_response");
			if (!oAuthResponse.isIsAdmin()) {
				throw new UnauthorizedException("Fetching plots for all users requires admin access");				
			}
		} else {
			allUsers = false;
		}

		EntityManager mgr = null;
		Cursor cursor = null;
		List<Plot> execute = null;

		afterDate = afterDate == null ? new Date(0) : afterDate;
		//afterDate = new Date();
		
		try {
			mgr = getEntityManager();
			//Query query = mgr.createQuery("select from Plot as Plot where recorderName='" + user.getEmail() + "'");
			Query query;
			if (allUsers) {
				query = mgr.createQuery("select from Plot as Plot where modifiedDate >= :modifiedDate")
						 .setParameter("modifiedDate", afterDate, TemporalType.DATE);
			} else {
				query = mgr.createQuery("select from Plot as Plot where recorderName=:recorderName and modifiedDate >= :modifiedDate")
								 .setParameter("recorderName", user.getEmail())
								 .setParameter("modifiedDate", afterDate, TemporalType.DATE);
			}
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
			}

			if (limit != null) {
				query.setFirstResult(0);
				query.setMaxResults(limit);
			}

			execute = (List<Plot>) query.getResultList();
			cursor = JPACursorHelper.getCursor(execute);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

			// Tight loop for fetching all entities from datastore and accomodate
			// for lazy fetch.
			for (Plot obj : execute)
				;
		} finally {
			mgr.close();
		}

		return CollectionResponse.<Plot> builder().setItems(execute)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getPlot")
	//public Plot getPlot(@Named("id") Long id) {
	public Plot getPlot(@Named("id") String id) {
		EntityManager mgr = getEntityManager();
		Plot plot = null;
		try {
			plot = mgr.find(Plot.class, id);
		} finally {
			mgr.close();
		}
		return plot;
	}


	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param plot the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertPlot")
	public Plot insertPlot(Plot plot, User user)  throws OAuthRequestException, IOException, EntityExistsException, ServletException {
		log.info(TAG + ", insertPlot entering with " + plot.getRecorderName() + "-" + plot.getName());
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}
		
		plot.setRecorderName(user.getEmail());
		plot.setID(plot.getRecorderName() + "-" + plot.getName()); 
		
		EntityManager mgr = getEntityManager();
		try {
			if (containsPlot(plot)) {
				throw new EntityExistsException("Object already exists: " + plot.getID());
			}
			
			plot = new SoilDataCruncher().calculateResults(plot);
			plot.setRecommendation("No longer used");
			plot.setModifiedDate(new Date());
			mgr.persist(plot);
		} catch (Exception eX) {
			log.severe(TAG + ", " + eX.toString());
			plot.setID(null); 		
			if (eX instanceof EntityExistsException) throw (EntityExistsException)eX;
		} finally {
			mgr.close();
		}
		
		/***
		//TODO: This works. Use it when we switch to MySQL db 
		plot = new SoilDataCruncher().calculateResults(plot);
		plot.setModifiedDate(new Date());
		insert(PLOT_URL, toJSON(plot));
		***/

		log.info(TAG + ", successfully inserted " + plot.getID());
		return plot;
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param plot the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updatePlot")
	public Plot updatePlot(Plot plot, User user)  throws OAuthRequestException, IOException, EntityNotFoundException, ServletException {
		log.info(TAG + ", updatePlot entering with " + plot.getRecorderName() + "-" + plot.getName());
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}
		
		plot.setRecorderName(user.getEmail());
		plot.setID(plot.getRecorderName() + "-" + plot.getName());

		EntityManager mgr = getEntityManager();
		try {
			if (!containsPlot(plot)) {
				throw new EntityNotFoundException("Object does not exist: " + plot.getID());
			}
			plot = new SoilDataCruncher().calculateResults(plot);
			/***
			plot.setRecommendation("Current indices:\n    Grass productivity: " + 
					(plot.getGrassProductivity() == null ? "N/A" :  plot.getGrassProductivity()) + 
					"\n    Grass erosion: " + 
					(plot.getGrassErosion() == null ? "N/A" : plot.getGrassErosion()) + 
					"\n    Maize productivity: " + 
					(plot.getMaizeProductivity() == null ? "N/A" : plot.getMaizeProductivity()) + 
					"\n    Maize erosion: " + 
					(plot.getMaizeErosion() == null ? "N/A" : plot.getMaizeErosion()));
			***/
			plot.setRecommendation("No longer used");
			plot.setModifiedDate(new Date());
			mgr.persist(plot);
		} catch (Exception eX) {
			log.severe(TAG + ", " + eX.toString());
			plot.setID(null); 		
			if (eX instanceof EntityNotFoundException) throw (EntityNotFoundException)eX;
		} finally {
			mgr.close();
		}
		log.info(TAG + ", successfully updated " + plot.getID());
		return plot;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removePlot")
	//public void removePlot(@Named("id") Long id) {
	public void removePlot(@Named("id") String id) {
		EntityManager mgr = getEntityManager();
		try {
			Plot plot = mgr.find(Plot.class, id);
			mgr.remove(plot);
		} finally {
			mgr.close();
		}
	}
	
	
	@ApiMethod(name = "quickClimate")
	public Plot quickClimate(Plot plot, User user)  throws OAuthRequestException, IOException, EntityExistsException, ServletException {
		log.info(TAG + ", quickClimate entering");
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}
				
		plot = new SoilDataCruncher().getQuickClimate(plot);

		log.info(TAG + ", returning climate data");
		return plot;
	}
	
	
	public class StringReturn {
		private String theString;
		
		public StringReturn() {
		}
		public StringReturn(String s) {
			this.theString = s;
		}
		
		public void setTheString(String s) {
			this.theString = s;			
		}
		public String getTheString() {
			return theString;
		}
	}
	
	//TODO: Not sure I want this inside the endpoint.  Might move to upload handler servlet.
	@ApiMethod(name = "getPhotoUploadURL")
	//public String[] getUploadUrl() {
	public StringReturn getUploadUrl() {
		log.info("PlotEndpoint.getPhotoUploadURL, enter");
	//public String[] getUploadUrl() {
		//String bucket = configManager.getGoogleStorageBucket();
		//String bucket = "silicon-bivouac-496.appspot.com";  //TODO: configuration
	    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		log.info("PlotEndpoint.getPhotoUploadURL, got blodstoreService");
	    UploadOptions uploadOptions = UploadOptions.Builder.withGoogleStorageBucketName(BUCKET_NAME);
		log.info("PlotEndpoint.getPhotoUploadURL, got uploadOptions");
	    //return blobstoreService.createUploadUrl(configManager.getUploadHandlerUrl(), uploadOptions);
	    String retURL = blobstoreService.createUploadUrl("/photo/upload", uploadOptions);
		log.info("PlotEndpoint.getPhotoUploadURL, retURL = " + retURL);
	    //return new String [] {retURL};
	    return new StringReturn(retURL);
	}	
 
	private boolean containsPlot(Plot plot) {
		EntityManager mgr = getEntityManager();
		boolean contains = true;
		if (plot.getID() == null) {
			contains = false;
			log.severe("Error inserting plot: ID is null.");
		} else {
			try {
				Plot item = mgr.find(Plot.class, plot.getID());
				if (item == null) {
					contains = false;
					//log.severe("Error inserting plot: ID exists.");
				}
			} finally {
				mgr.close();
			}
		}
		return contains;
	}

	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}

	
	//******************************************************************************************
	//Everything below this line is for using MySQL via REST instead of the GAE datastore.  It works.
	//However, all this should be moved to a common class for use by the endpoints, the migration servlet, and the 
	//report servlet.
	
	private static final String CHARSET = "UTF-8";		
	private static final String PLOT_URL = "http://localhost:8080/lpks/rest/plot";
	private static final String TRANSECT_URL = "http://localhost:8080/lpks/rest/transect";
	private void insert(String url, String content) throws ServletException, IOException, EntityExistsException{
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
			if (((HttpURLConnection)connection).getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
				throw new EntityExistsException();
			} else {
				throw new ServletException("Response code " + ((HttpURLConnection)connection).getResponseCode());
			}
		}	
	}
	
    private String toJSON(Plot plot)  throws IOException{
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
    	
    	if (plot.getSlope() != null) {
	    	Integer slopeCategorieIndex = slopeCategories.get(plot.getSlope());
	    	if (slopeCategorieIndex != null) {
	    		sC.setId(slopeCategorieIndex);
	    		dtoPlot.setSlopeCategory(sC);
	    	} else {
	    		dtoPlot.setSlopeMeasured(Double.parseDouble(plot.getSlope()));
	    	}
    	}
    	
    	if (plot.getSlopeShape() != null) {
	    	sSC.setId(slopeShapeCategories.get(plot.getSlopeShape()));
	    	dtoPlot.setSlopeShapeCategory(sSC);
    	}

    	dtoPlot.setSoilPitPhotoUrl(plot.getSoilPitPhotoURL());
    	dtoPlot.setSoilSamplesPhotoUrl(plot.getSoilSamplesPhotoURL());
    	dtoPlot.setSurfaceCracking(plot.isSurfaceCracking());
    	dtoPlot.setSurfaceSalt(plot.isSurfaceSalt());
    	
    	List<com.noisyflowers.lpks.persistence.model.SoilHorizon> sHList = new ArrayList<com.noisyflowers.lpks.persistence.model.SoilHorizon>();
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
        
		Writer strWriter = new StringWriter();
		com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper mapper = new com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper();
		mapper.setSerializationInclusion(com.google.appengine.repackaged.org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL);
		mapper.configure(com.google.appengine.repackaged.org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);			
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
		mapper.writeValue(strWriter, dtoPlot);
		String plotJSON = strWriter.toString();

    	return plotJSON;
    }

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

}
