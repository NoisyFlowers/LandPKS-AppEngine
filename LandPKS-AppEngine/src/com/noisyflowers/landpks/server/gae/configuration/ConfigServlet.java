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
 * com.noisyflowers.landpks.server.gae.configuration
 * ConfigServlet.java
 */

package com.noisyflowers.landpks.server.gae.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
//import com.google.appengine.api.datastore.Query;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.model.AWHCMapping;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.model.ProductivityAndErosionMapping;
import com.noisyflowers.landpks.server.gae.model.RHMModifiedDate;
import com.noisyflowers.landpks.server.gae.model.Transect;
import com.noisyflowers.landpks.server.gae.util.Constants.RockFragmentRange;
import com.noisyflowers.landpks.server.gae.util.Constants.SoilTexture;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;

public class ConfigServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Build a task using the TaskOptions Builder pattern from ** above
        Queue queue = QueueFactory.getDefaultQueue();
        queue.add(TaskOptions.Builder.withUrl("/config").method(TaskOptions.Method.POST)); 
        
        resp.setContentType("text/plain");
        resp.getWriter().println("Config task submitted");
        
        //initTransectModifiedDates();
        
    }
    
    // Executed by TaskQueue
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        initTransectModifiedDates();
    }

    private void initTransectModifiedDates() {
		EntityManager mgr = null;
		Cursor cursor = null;
		List<Transect> transectList = null;
		
		try {
			mgr = getEntityManager();
			Query query = mgr.createQuery("select from Transect as Transect");

			transectList = (List<Transect>) query.getResultList();
			cursor = JPACursorHelper.getCursor(transectList);

			/**
			Map<String, Date> recorderMap = new HashMap<String, Date>();
			**/
			
			for (Transect transect : transectList) {
				if (transect.getModifiedDate() == null) {
					transect.setModifiedDate(new Date(1));
					
					/***
					RHMModifiedDate modDate = new RHMModifiedDate();
					String recorderName = transect.getID().split("-")[0]; //TODO: risky
					if (recorderName != null) {
						if (!recorderMap.containsKey(recorderName) ||
							(recorderMap.containsKey(recorderName) && recorderMap.get(recorderName).before(transect.getModifiedDate()))) {
							recorderMap.put(recorderName, transect.getModifiedDate());								
						}				
					}
					***/

					
					mgr.getTransaction().begin();
					try {
						mgr.getTransaction().commit();
					} finally {
					    if (mgr.getTransaction().isActive()) {
					    	mgr.getTransaction().rollback();
					    }
					}
				}
			}
		} finally {
			mgr.close();
		}

    }
	
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}

    
}
