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
 * TransectEndpoint.java
 */

package com.noisyflowers.landpks.server.gae.dal;

import static com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID;

import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.model.RHMModifiedDate;
import com.noisyflowers.landpks.server.gae.model.Segment;
import com.noisyflowers.landpks.server.gae.model.Transect;
import com.noisyflowers.landpks.server.gae.util.Constants;
import com.noisyflowers.landpks.server.gae.util.Credentials;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.UserServicePb.GetOAuthUserResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;

@Api(name = "transectendpoint", 
	 namespace = @ApiNamespace(ownerDomain = "noisyflowers.com", ownerName = "noisyflowers.com", packagePath = "landpks.server.gae.model"),
	 clientIds = {Credentials.INSTALLED_CLIENT_ID, Credentials.WEB_CLIENT_ID, Credentials.ANDROID_CLIENT_ID_0, Credentials.ANDROID_CLIENT_ID_1, Credentials.ANDROID_CLIENT_ID_2, Credentials.ANDROID_CLIENT_ID_3, Credentials.ANDROID_CLIENT_ID_4, Credentials.ANDROID_CLIENT_ID_5, API_EXPLORER_CLIENT_ID },
	 audiences = {Credentials.INSTALLED_CLIENT_ID, Credentials.WEB_CLIENT_ID, Credentials.ANDROID_CLIENT_ID_0, Credentials.ANDROID_CLIENT_ID_1, Credentials.ANDROID_CLIENT_ID_2, Credentials.ANDROID_CLIENT_ID_3, Credentials.ANDROID_CLIENT_ID_4, Credentials.ANDROID_CLIENT_ID_5 },
	 scopes = {Credentials.EMAIL_SCOPE, Credentials.PROFILE_SCOPE}
	)
public class TransectEndpoint {
	private static final String TAG = PlotEndpoint.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@ApiMethod(name = "listTransect")
	public CollectionResponse<Transect> listTransect(
			@Nullable @Named("cursor") String cursorString,
			@Nullable @Named("limit") Integer limit, 
			@Nullable @Named("afterDate") Date afterDate, 
			@Nullable @Named("otherUser") String otherUser, 
			User user) throws OAuthRequestException, IOException, UnauthorizedException {

		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}

		if (otherUser != null) {
			ApiProxy.Environment environment = ApiProxy.getCurrentEnvironment();
			GetOAuthUserResponse oAuthResponse = (GetOAuthUserResponse) environment.getAttributes().get("com.google.appengine.api.oauth.OAuthService.get_oauth_user_response");
			if (!oAuthResponse.isIsAdmin()) {
				throw new UnauthorizedException("Fetching transects for another user requires admin access");				
			}
		}

		EntityManager mgr = null;
		Cursor cursor = null;
		List<Transect> transectList = null;

		afterDate = afterDate == null ? new Date(0) : afterDate;

		try {
			mgr = getEntityManager();
			//Query query = mgr.createQuery("select from Transect as Transect where modifiedDate > :modifiedDate")
			// 				.setParameter("modifiedDate", afterDate, TemporalType.DATE);
			/***
			Query query = mgr.createQuery("select from Transect as Transect where siteID >= :siteID1 and siteID < :siteID2 and modifiedDate > :modifiedDate")
						 .setParameter("siteID1", user.getEmail())
			 			 .setParameter("siteID2", user.getEmail() + "\ufffd")
			 			 .setParameter("modifiedDate", afterDate, TemporalType.DATE);
			 ***/
			//TODO: Might want to consider a separate modifiedDate table to cut down on db traffic
			String userID = (otherUser == null ? user.getEmail() : otherUser);
			Query query = mgr.createQuery("select from Transect as Transect where siteID >= :siteID1 and siteID < :siteID2")
					   		 .setParameter("siteID1", userID)
					   		 .setParameter("siteID2", userID + "\ufffd");
			if (cursorString != null && cursorString != "") {
				cursor = Cursor.fromWebSafeString(cursorString);
				query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
			}

			if (limit != null) {
				query.setFirstResult(0);
				query.setMaxResults(limit);
			}


			List<Transect> tmpList = (List<Transect>) query.getResultList();
			transectList = new ArrayList<Transect>();
	
			for (Transect transect : tmpList) {
				if (transect.getModifiedDate().after(afterDate)) {
					transectList.add(transect);
				}
			}
			
			cursor = JPACursorHelper.getCursor(transectList);
			if (cursor != null)
				cursorString = cursor.toWebSafeString();

		} finally {
			mgr.close();
		}

		return CollectionResponse.<Transect> builder().setItems(transectList)
				.setNextPageToken(cursorString).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(name = "getTransect")
	public Transect getTransect(@Named("id") String id, User user) throws OAuthRequestException, IOException {
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}

		EntityManager mgr = getEntityManager();
		Transect transect = null;
		try {
			transect = mgr.find(Transect.class, id);
		} finally {
			mgr.close();
		}
		return transect;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param transect the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "insertTransect")
	public Transect insertTransect(Transect transect, User user) throws OAuthRequestException, 
																		IOException, 
																		EntityExistsException, 
																		EntityNotFoundException {
		
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}

		Date date = new Date();
		
		transect.setID(transect.getSiteID() + "-" + transect.getDirection());
		transect.setModifiedDate(date);
		
		//RHMModifiedDate modDate = new RHMModifiedDate(); //TODO: This is not fully implemented, but tabling until other backend changes are finalized
		//modDate.setRecorderName(user.getEmail());
		//modDate.setDate(date);
		
		EntityManager mgr = getEntityManager();
		try {
			if (containsTransect(transect)) {
				throw new EntityExistsException("Transect " + transect.getID() + " already exists");
			}
			
			if (!siteRecordExists(transect)) {
				throw new EntityNotFoundException("No site record found for transect " + transect.getID());
			}

			mgr.getTransaction().begin();
			try {
				mgr.persist(transect);
				//mgr.persist(modDate);
				mgr.getTransaction().commit();
			} finally {
			    if (mgr.getTransaction().isActive()) {
			    	mgr.getTransaction().rollback();
			    }
			}
		} finally {
			mgr.close();
		}
		return transect;
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param transect the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "updateTransect")
	public Transect updateTransect(Transect transect, User user) throws OAuthRequestException,
																		IOException, 
																		EntityExistsException, 
																		EntityNotFoundException {
		
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}

		Date date = new Date();

		transect.setID(transect.getSiteID() + "-" + transect.getDirection());
		transect.setModifiedDate(date);

		EntityManager mgr = getEntityManager();
		try {
			if (!containsTransect(transect)) {
				throw new EntityNotFoundException("Transect " + transect.getID() + " does not exist");
			}
			
			if (containsSegmentsForDate(transect)) {
				throw new EntityExistsException("Segments already uploaded for " + transect.getID() + " for this date");				
			}
						
			mgr.getTransaction().begin();
			try {
				Transect oldTransect = mgr.find(Transect.class, transect.getID());
				for (Segment s : transect.getSegments()) {
					s.setTransect(oldTransect);
				}
				oldTransect.getSegments().addAll(0, transect.getSegments());
				oldTransect.setModifiedDate(new Date());
				
				log.info("!!!!!updateTransect, in transaction!!!!!!");
				//RHMModifiedDate modDate = mgr.find(RHMModifiedDate.class, user.getEmail());
				//modDate.setDate(date);
				
				mgr.getTransaction().commit();
			} finally {
			    if (mgr.getTransaction().isActive()) {
			    	mgr.getTransaction().rollback();
			    }
			}
		} finally {
			mgr.close();
		}
		return transect;
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(name = "removeTransect")
	public void removeTransect(@Named("id") String id, User user) throws OAuthRequestException, IOException  {
		if (user == null) {
			throw new OAuthRequestException("Unknown user");
		}

		EntityManager mgr = getEntityManager();
		try {
			Transect transect = mgr.find(Transect.class, id);
			//mgr.remove(transect);
			mgr.getTransaction().begin();
			try {
				mgr.remove(transect);
				mgr.getTransaction().commit();
			} finally {
			    if (mgr.getTransaction().isActive()) {
			    	mgr.getTransaction().rollback();
			    }
			}
		} finally {
			mgr.close();
		}
	}

	private boolean containsTransect(Transect transect) {
		EntityManager mgr = getEntityManager();
		boolean contains = true;
		if (transect.getID() == null) {
			contains = false;
			log.severe("Error inserting plot: ID is null.");
		} else {
			try {
				Transect item = mgr.find(Transect.class, transect.getID());
				if (item == null) {
					contains = false;
				}
			} finally {
				mgr.close();
			}
		}
		return contains;
	}

	private boolean containsSegmentsForDate(Transect transect) {
		EntityManager mgr = getEntityManager();
		
		String newDate = transect.getSegments().get(0).getDate();
		
		boolean contains = false;
		try {
			Transect item = mgr.find(Transect.class, transect.getID());
			if (item != null) {
				for (Segment s : item.getSegments()) {
					if (newDate.equals(s.getDate())) {
						contains = true;
						break;
					}
				}
			}
		} finally {
			mgr.close();
		}
		
		return contains;	
	}
	
	private boolean siteRecordExists(Transect transect) {
		EntityManager mgr = getEntityManager();
		
		boolean contains = true;
		try {
			Plot item = mgr.find(Plot.class, transect.getSiteID());
			if (item == null) {
				contains = false;
			}
		} finally {
			mgr.close();
		}
		return contains;			
	}
	
	
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}

}
