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
 * com.noisyflowers.landpks.server.gae.upload
 * PhotoUploadServlet.java
 */

package com.noisyflowers.landpks.server.gae.upload;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.tools.development.AppContext;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.dal.PlotEndpoint;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.processing.SoilDataCruncher;

public class PhotoUploadServlet extends HttpServlet {

	private static final String TAG = PlotEndpoint.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);

	private static final String PHOTO_FILE_PARAMETER_NAME = "photo";
	private static final String PLOT_ID_PARAMETER_NAME = "plotID";
	private static final String PHOTO_SUBJECT_PARAMETER_NAME = "photoSubject";
	private enum PhotoSubject {
		LANDSCAPE_NORTH ("landscapeNorth"),
		LANDSCAPE_EAST ("landscapeEast"),
		LANDSCAPE_SOUTH ("landscapeSouth"),
		LANDSCAPE_WEST ("landscapeWest"),
		SOIL_PIT ("soilPit"),
		SOIL_SAMPLES ("soilSamples");
		
		private final String subjectName;
		
		public static final Map<String, PhotoSubject> subjectNameLookup = new HashMap<String, PhotoSubject>();
		static {
			for (PhotoSubject s : PhotoSubject.values()) {
				subjectNameLookup.put(s.subjectName, s);
			}
		}

		PhotoSubject(String subjectName) {
			this.subjectName = subjectName;
		}
		
		public String getSubjectName() {
			return subjectName;
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		log.info("handling doGet");
		res.setContentType("text/plain");
		res.getWriter().println("Hello from PhotoUploadServlet");	
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		log.info("handling doPost");
		//TODO: This may not work properly when live. Must verify!
		/**
		try {
			OAuthService oauth = OAuthServiceFactory.getOAuthService();
			User user = oauth.getCurrentUser();
		} catch (OAuthRequestException oE) {
	        res.sendError(401, "User not authorized to upload photos.");
	        return;
		}
		**/
				
	    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
	    List<BlobKey> keys = blobs.get(PHOTO_FILE_PARAMETER_NAME);  
	    String servingURL = null;
	    boolean succeeded = false;
		//log.info("before if keys");
	    if (keys != null && keys.size() > 0) {
	    	BlobKey blobKey = keys.get(0);	    	
	    	String plotID = req.getParameter(PLOT_ID_PARAMETER_NAME);
	    	String photoSubject = req.getParameter(PHOTO_SUBJECT_PARAMETER_NAME);
			log.info("plotID = " + plotID + "; photoSubject = " + photoSubject);
	    	if (plotID != null && photoSubject != null) {
	    		Plot plot = getPlot(plotID);
	    		if (plot == null) {
	    			log.severe("No plot found with plotID = " + plotID);
	    		} else {
					log.info("plot name = " + plot.getName());
		    		
			    	ImagesService imageService = ImagesServiceFactory.getImagesService();
			    	ServingUrlOptions servingOptions = ServingUrlOptions.Builder.withBlobKey(blobKey);
			    	servingURL = imageService.getServingUrl(servingOptions);
					log.info("servingURL = " + servingURL);
	
		    		if (PhotoSubject.LANDSCAPE_NORTH.getSubjectName().equals(photoSubject)) {
		    			plot.setLandscapeNorthPhotoURL(servingURL);
		    		} else if (PhotoSubject.LANDSCAPE_EAST.getSubjectName().equals(photoSubject)) {
		    			plot.setLandscapeEastPhotoURL(servingURL);
		    		} else if (PhotoSubject.LANDSCAPE_SOUTH.getSubjectName().equals(photoSubject)) {
		    			plot.setLandscapeSouthPhotoURL(servingURL);
		    		} else if (PhotoSubject.LANDSCAPE_WEST.getSubjectName().equals(photoSubject)) {
		    			plot.setLandscapeWestPhotoURL(servingURL);
		    		} else if (PhotoSubject.SOIL_PIT.getSubjectName().equals(photoSubject)) {
		    			log.info("setting pit photo url");
		    			plot.setSoilPitPhotoURL(servingURL);
		    		} else if (PhotoSubject.SOIL_SAMPLES.getSubjectName().equals(photoSubject)) {
		    			plot.setSoilSamplesPhotoURL(servingURL);
		    		}
		    	    plot = updatePlot(plot);
		    		//log.info("after plot update");
		    	    succeeded = true;
	    		}
	    	}
	    }
	    
	    if (succeeded) {
			log.info("succeeded");
	        res.setStatus(HttpServletResponse.SC_OK);
	        //JSONObject json = new JSONObject();
	        //try {json.put("servingURL", servingURL);} catch (JSONException jEX){}
	        //res.setContentType("application/json");
	        //res.getWriter().print(json.toString());
	        res.setContentType("text/plain");
	        res.getWriter().println(servingURL);
	    } else {
	        res.sendError(400, "Unable to upload photo at this time.");
	    }
	}
	
	//TODO: should probably create plot manager used by both this servlet and PlotEndpoint 
	private Plot getPlot(String id) {
		EntityManager mgr = getEntityManager();
		Plot plot = null;
		try {
			plot = mgr.find(Plot.class, id);
		} finally {
			mgr.close();
		}
		return plot;
	}

	private Plot updatePlot(Plot plot) {
		EntityManager mgr = getEntityManager();
		try {
			if (!containsPlot(plot)) {
				throw new EntityNotFoundException("Object does not exist");
			}
			plot.setModifiedDate(new Date());
			mgr.merge(plot);
		} finally {
			mgr.close();
		}
		return plot;	
	}

	private boolean containsPlot(Plot plot) {
		EntityManager mgr = getEntityManager();
		boolean contains = true;
		if (plot.getID() == null) {
			contains = false;
			//log.severe("Error inserting plot: ID is null.");
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
	
}
