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
 * com.noisyflowers.landpks.server.gae.reports
 * ReportServlet.java
 */

package com.noisyflowers.landpks.server.gae.reports;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.datanucleus.query.JPACursorHelper;
import com.noisyflowers.landpks.server.gae.EMF;
import com.noisyflowers.landpks.server.gae.dal.PlotEndpoint;
import com.noisyflowers.landpks.server.gae.model.Plot;
import com.noisyflowers.landpks.server.gae.model.Segment;
import com.noisyflowers.landpks.server.gae.model.StickSegment;
import com.noisyflowers.landpks.server.gae.model.Transect;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.MappingStrategy;
//import au.com.bytecode.opencsv.bean.BeanToCsv;

public class ReportServlet extends HttpServlet {
	private static final String TAG = ReportServlet.class.getName(); 
	private static final Logger log = Logger.getLogger(TAG);

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("doGet, enter");
    	
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();

    	resp.setContentType("text/plain");
    	
        PrintWriter writer = resp.getWriter();

        //if full RHM report, kick it to background task and return
        if ("RHM".equals(req.getParameter("reportType")) &&
    		req.getParameter("siteID") == null &&
    		req.getParameter("recorderName") == null) {
        	if (req.getParameter("taskTest") != null) {
	        	Queue queue = QueueFactory.getDefaultQueue();
	        	queue.add(TaskOptions.Builder.withUrl("/report")
	        			  .method(com.google.appengine.api.taskqueue.TaskOptions.Method.POST)
	        			  .param("email", user.getEmail())
	        			  .param("reportFormat", req.getParameter("reportFormat")));
	    		writer.println("Full RHM reports take a while to create.  The report will be emailed to you.");
        	} else {
        		writer.println("Full RHM reports are not supported.  You must run a full LPKS query, then an RHM query for each recorder name or site id found there.");
        	}
        	return;
    	}
       
		EntityManager mgr = null;
		Cursor cursor = null;
		List<Plot> plots = null;
 		
		try {
			mgr = getEntityManager();
			Query query = null;
			if (req.getParameter("siteID") != null) {
				query = mgr.createQuery("select p from Plot p where p.ID = :pID").setParameter("pID", req.getParameter("siteID"));
			} else if (req.getParameter("recorderName") != null) {
				query = mgr.createQuery("select p from Plot p where p.recorderName = :rN").setParameter("rN", req.getParameter("recorderName"));
			} else {
				query = mgr.createQuery("select from Plot as plot");
			}

			plots = (List<Plot>) query.getResultList();
		} finally {
			mgr.close();
		}
    	
    	if ("RHM".equals(req.getParameter("reportType"))) { //RHM
    		log.info("doGet, RHM");
    		if ("csv".equals(req.getParameter("reportFormat"))) {
            	String fileName = "RHM-Snapshot_" + sdf.format(new Date()) + ".csv";
                resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);
    			generateRHMcsv(plots, writer, mgr);
    		} else if ("txt".equals(req.getParameter("reportFormat"))) {
    			generateRHMtxt(plots, writer, mgr);
    		} else {
    			generateUsage(writer);
    		}
    	}   else if ("LPKS".equals(req.getParameter("reportType"))) { //LPKS
    		log.info("doGet, LPKS");
    		if ("csv".equals(req.getParameter("reportFormat"))) {
    			String fileName = "LandPKS-Snapshot_" + sdf.format(new Date()) + ".csv";
    			resp.addHeader("Content-Disposition", "attachment; filename=" + fileName);
                generateLPKScsv(plots, writer);
    		} else if ("txt".equals(req.getParameter("reportFormat"))) {
    			generateLPKStxt(plots, writer);
    		} else {
    			generateUsage(writer);
    		}
    	} else {
    		generateUsage(writer);
    	}
    		
    		
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("doPost, enter");
		
		String email = req.getParameter("email");//TODO: verify
		
		EntityManager mgr = null;
		Cursor cursor = null;
		List<Plot> plots = null;
 		
		try {
			mgr = getEntityManager();
			Query query = mgr.createQuery("select from Plot as plot");
			plots = (List<Plot>) query.getResultList();
		} finally {
			mgr.close();
		}
		
		emailRHMreport(plots, mgr, email, req.getParameter("reportFormat"));

    }
    
    private void generateUsage(PrintWriter writer) {
		writer.println("Usage: (\"?\" before first parameter, \"&\" between subsequent parameters)");
		writer.println("    Required:");
		writer.println("        reportType = RHM | LPKS");
		writer.println("        reportFormat = csv | txt");
		writer.println("    Optional for LPKS/Required for RHM:");
		writer.println("        recorderName = [email address of user account]");
		writer.println("        <or>");
		writer.println("        siteID = [full site id (email-site name)]");
		writer.close();
    }
    
    private void generateLPKScsv(List<Plot> plots, PrintWriter writer) {        	                
		String line = "name,recorderName,organization,latitude,longitude,city,modifiedDate,landCover,grazed,flooding,slope,slopeShape,rockFragmentForSoilHorizon1,rockFragmentForSoilHorizon2,rockFragmentForSoilHorizon3,rockFragmentForSoilHorizon4,rockFragmentForSoilHorizon5,rockFragmentForSoilHorizon6,rockFragmentForSoilHorizon7,colorForSoilHorizon1,colorForSoilHorizon2,colorForSoilHorizon3,colorForSoilHorizon4,colorForSoilHorizon5,colorForSoilHorizon6,colorForSoilHorizon7,textureForSoilHorizon1,textureForSoilHorizon2,textureForSoilHorizon3,textureForSoilHorizon4,textureForSoilHorizon5,textureForSoilHorizon6,textureForSoilHorizon7,surfaceCracking,surfaceSalt,landscapeNorthPhotoURL,landscapeEastPhotoURL,landscapeSouthPhotoURL,landscapeWestPhotoURL,soilPitPhotoURL,soilSamplesPhotoURL,grassProductivity,grassErosion,maizeProductivity,maizeErosion";
		writer.println(line);
		for (Plot plot: plots) {
			//TODO: Because of color machinations below, would be better to use StringBuilder
			line = "\"" + plot.getName() + "\",\"" + plot.getRecorderName() + "\",\"" + plot.getOrganization() + "\"," + 
					plot.getLatitude() + "," + plot.getLongitude() + ",\"" +
					plot.getCity() + "\",\"" + plot.getModifiedDate() + "\",\"" + plot.getLandCover() + "\"," + 
					plot.isGrazed() + "," + plot.isFlooding() + ",\"" +
					plot.getSlope() + "\",\"" + plot.getSlopeShape() + "\"," +
					plot.getRockFragmentForSoilHorizon1() + "," + 
					plot.getRockFragmentForSoilHorizon2() + "," + 
					plot.getRockFragmentForSoilHorizon3() + "," + 
					plot.getRockFragmentForSoilHorizon4() + "," + 
					plot.getRockFragmentForSoilHorizon5() + "," + 
					plot.getRockFragmentForSoilHorizon6() + "," + 
					plot.getRockFragmentForSoilHorizon7() + ",";
					
			Integer color = plot.getColorForSoilHorizon1();
			String colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon2();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon3();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon4();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon5();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon6();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			color = plot.getColorForSoilHorizon7();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			line += colorStr + ",";
			
			line += plot.getTextureForSoilHorizon1() + "," + 
					plot.getTextureForSoilHorizon2() + "," + 
					plot.getTextureForSoilHorizon3() + "," + 
					plot.getTextureForSoilHorizon4() + "," + 
					plot.getTextureForSoilHorizon5() + "," + 
					plot.getTextureForSoilHorizon6() + "," + 
					plot.getTextureForSoilHorizon7() + "," + 
					plot.isSurfaceCracking() + "," + plot.isSurfaceSalt() + "," +
					plot.getLandscapeNorthPhotoURL() + "," +
					plot.getLandscapeEastPhotoURL() + "," +
					plot.getLandscapeSouthPhotoURL() + "," +
					plot.getLandscapeWestPhotoURL() + "," +
					plot.getSoilPitPhotoURL() + "," +
					plot.getSoilSamplesPhotoURL() + "," +
					//"\"" + plot.getRecommendation() + "\"," + 
					plot.getGrassProductivity() + "," + plot.getGrassErosion() + "," +
					//plot.getMaizeProductivity() + "," + plot.getMaizeErosion() + "\n";	
					plot.getCropProductivity() + "," + plot.getCropErosion();	
			writer.println(line);
		}
		writer.close();
    }

    private void generateLPKStxt(List<Plot> plots, PrintWriter writer) {        	                
		for (Plot plot: plots) {
			writer.println("Plot id: " + plot.getID());
			writer.println("    Name: " + plot.getName());
			writer.println("    Recorder name: " + plot.getRecorderName());
			writer.println("    Latitude: " + plot.getLatitude());
			writer.println("    Longitude: " + plot.getLongitude());
			writer.println("    City: " + plot.getCity());
			writer.println("    Upload date: " + plot.getModifiedDate());
			writer.println("    Land cover: " + plot.getLandCover());
			writer.println("    Grazed: " + plot.isGrazed());
			writer.println("    Flooded: " + plot.isFlooding());
			writer.println("    Slope: " + plot.getSlope());
			writer.println("    Slope shape: " + plot.getSlopeShape());
			
			writer.println("    Soil horizon 1: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon1());
			Integer color = plot.getColorForSoilHorizon1();
			String colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon1());
			
			writer.println("    Soil horizon 2: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon2());
			color = plot.getColorForSoilHorizon2();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon2());
			
			writer.println("    Soil horizon 3: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon3());
			color = plot.getColorForSoilHorizon3();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon3());
			
			writer.println("    Soil horizon 4: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon4());
			color = plot.getColorForSoilHorizon4();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon4());
			
			writer.println("    Soil horizon 5: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon5());
			color = plot.getColorForSoilHorizon5();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon5());
			
			writer.println("    Soil horizon 6: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon6());
			color = plot.getColorForSoilHorizon6();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon6());
			
			writer.println("    Soil horizon 7: ");
			writer.println("        Rock fragment: " + plot.getRockFragmentForSoilHorizon7());
			color = plot.getColorForSoilHorizon7();
			colorStr = color == null ? null : ((color >> 16) & 0xFF) + "/" + ((color >> 8) & 0xFF) + "/" + ((color) & 0xFF);
			writer.println("        Color: " + colorStr);
			writer.println("        Texture: " + plot.getTextureForSoilHorizon7());
			
			writer.println("    Surface cracked: " + plot.isSurfaceCracking());
			writer.println("    Surface salt: " + plot.isSurfaceSalt());
			writer.println("    Landscape north photo URL: " + plot.getLandscapeNorthPhotoURL());
			writer.println("    Landscape east photo URL: " + plot.getLandscapeEastPhotoURL());
			writer.println("    Landscape south photo URL: " + plot.getLandscapeSouthPhotoURL());
			writer.println("    Landscape west photo URL: " + plot.getLandscapeWestPhotoURL());
			writer.println("    Soil pit photo URL: " + plot.getSoilPitPhotoURL());
			writer.println("    Soil samples photo URL: " + plot.getSoilSamplesPhotoURL());
			writer.println("    Analytic results: ");
			writer.println("        Grass productivity: " + plot.getGrassProductivity());
			writer.println("        Grass erosion: " + plot.getGrassErosion());
			writer.println("        Crop productivity: " + plot.getCropProductivity());
			writer.println("        Crop erosion: " + plot.getCropErosion());
		}
		writer.close();
    }
  
    private String buildRHMcsvString(List<Plot> sites, EntityManager mgr) {
		log.info("buildRHMcsvString, enter");
    	StringBuilder sB = new StringBuilder();
		sB.append("Name,Recorder Name,Transect,Segment,Date,Canopy Height,Canopy Gap, Basal Gap, Species 1 Density, Species 2 Density, Species List, Stick Segment 0, Stick Segment 1, Stick Segment 2, Stick Segment 3, Stick Segment 4, Bare Total, Trees Total, Shrubs Total, Sub-shrubs Total, Perennial Grasses Total, Annuals Total, Herb Litter Total, Wood Litter Total, Rock Total\n");
        
		if (sites == null || sites.size() == 0) {
			sB.append("No sites found\n");
		} else {
			List<Transect> allTransects = null;
			
			try {
				log.info("buildRHMcsvString, getting transects");
				mgr = getEntityManager();
				Query query = mgr.createQuery("select t from Transect t");
				allTransects = (List<Transect>) query.getResultList();
				log.info("buildRHMcsvString, successfully queried transects. Count: " + allTransects.size() +  " First: " + allTransects.get(0).getID());
			} catch (Exception eX) {
				log.warning(eX.getMessage());
				log.log(Level.SEVERE, "Error during query", eX);
			} finally {
				mgr.close();
			}
			
			if (allTransects != null && allTransects.size() > 0) {
				Map<String, List<Transect>> transectMap = new HashMap<String, List<Transect>>();
				for (Transect transect : allTransects) {
					log.info("buildRHMcsvString, transect load loop: " + transect.getID());
					List<Transect> transectList = transectMap.get(transect.getSiteID());
					if (transectList == null) {
						transectList = new ArrayList<Transect>();
					}
					transectList.add(transect);
					transectMap.put(transect.getSiteID(), transectList);
				}
				for (Plot site : sites) {
					log.info("buildRHMcsvString, site loop: " + site.getID());
					List<Transect> transects = transectMap.get(site.getID());
					if (transects == null) {
						sB.append("No transect data found for this site: " + site.getID() + "\n");
					} else {	
						for (Transect transect : transects) {
							//log.info("generateRHMcsv, processing transect " + transect.getID());
							long startTime = System.currentTimeMillis();
							for (Segment segment : transect.getSegments()) {
								sB.append(site.getName() + "," + site.getRecorderName());
								sB.append("," + transect.getDirection());
								
								sB.append("," + segment.getRange());
								sB.append("," + segment.getDate());
								sB.append("," + segment.getCanopyHeight());
								sB.append("," + segment.getCanopyGap());
								sB.append("," + segment.getBasalGap());
								sB.append("," + segment.getSpeciesOfInterest1Count());
								sB.append("," + segment.getSpeciesOfInterest2Count());
								
								//put quotes around species list to contain inner commas
								sB.append(",\"");
								boolean first = true;
								for (String species : segment.getSpeciesList()) {
									if (!first) {
										sB.append(", ");
									} else {
										first = false;
									}
									sB.append(species);
								}
								sB.append("\"");

								//put quotes around cover list to contain inner commas
								int bareCount = 0, treeCount = 0, shrubCount = 0, subShrubCount = 0, grassCount = 0, annualCount = 0, herbLitterCount = 0, treeLitterCount = 0, rockCount = 0;
								for (StickSegment stickSegment : segment.getStickSegments()) {
									sB.append(",\"");
									first = true;
									if (stickSegment.covers[0]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Bare");
										bareCount++;
									}
									if (stickSegment.covers[1]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Trees");
										treeCount++;
									}
									if (stickSegment.covers[2]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Shrubs");
										shrubCount++;
									}
									if (stickSegment.covers[3]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Sub-Shrubs");
										subShrubCount++;
									}
									if (stickSegment.covers[4]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Grasses");
										grassCount++;
									}
									if (stickSegment.covers[5]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Annuals");
										annualCount++;
									}
									if (stickSegment.covers[6]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Herb Litter");
										herbLitterCount++;
									}
									if (stickSegment.covers[7]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Tree Litter");
										treeLitterCount++;
									}
									if (stickSegment.covers[8]) {
										if (!first) {
											sB.append(", ");
										} else {
											first = false;
										}
										sB.append("Rock");
										rockCount++;
									}
									sB.append("\"");							
								}
								sB.append("," + bareCount + "," + treeCount + "," + shrubCount + "," + subShrubCount + 
										  "," + grassCount + "," + annualCount + "," + herbLitterCount + 
										  "," + treeLitterCount + "," + rockCount);
								sB.append("\n");
							}
						}
					}
				}
			}
			
		}
    	
    	return sB.toString();
    }
    
    private void emailRHMreport(List<Plot> sites, EntityManager mgr, String email, String format) throws IOException {
		log.info("emailRHMreport, enter");
		
		if (!"csv".equals(format) && !"txt".equals(format))
		format = ("csv".equals(format) || "txt".equals(format)) ? format : "csv";
		String dateStr = sdf.format(new Date());
		
		FileService fileService = FileServiceFactory.getFileService();
		AppEngineFile file = fileService.createNewBlobFile("text/plain");
		boolean lock = false;
		FileWriteChannel writeChannel = fileService.openWriteChannel(file, lock);
		PrintWriter writer = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
		
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		//String msgBody = "csv".equals(format) ? buildRHMcsvString(sites, mgr) : buildRHMtxtString(sites, mgr);
		//log.info("emailRHMreport, msgBody: \n" + msgBody);
		
		if ("csv".equals(format)) {
			
		} else {
			buildRHMtxtFile(sites, mgr, writer);
			writer.close();
		}
		
		try {
		    Message msg = new MimeMessage(session);
		    msg.setFrom(new InternetAddress(email, ""));
		    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""));
		    msg.setSubject("RHM report for " + dateStr);
		    //msg.setText(msgBody);
		    
	        Multipart mp = new MimeMultipart();

	        BodyPart messageBody = new MimeBodyPart();
	        //messageBody.setText("Report is attached.");
	        messageBody.setContent("RHM report for " + dateStr + " is attached.", "text/plain");
	        mp.addBodyPart(messageBody);

	        MimeBodyPart attachment = new MimeBodyPart();
	        FileReadChannel readChannel = fileService.openReadChannel(file, false);
	        //BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, "UTF8"));
	        InputStream stream = Channels.newInputStream(readChannel);

	        //DataSource source = new ByteArrayDataSource(msgBody.getBytes(), "text/plain");
	        DataSource source = new ByteArrayDataSource(stream, "text/plain");
	        attachment.setDataHandler(new DataHandler(source));
	        attachment.setFileName("RHM-Snapshot_" + dateStr + "." + format);	        
	        attachment.setDisposition(Part.ATTACHMENT);
	        mp.addBodyPart(attachment);

	        msg.setContent(mp);
	        
	        msg.writeTo(System.out);
	        
		    Transport.send(msg);
		    readChannel.close();

		} catch (AddressException e) {
		    log.warning(e.getMessage());
		} catch (MessagingException e) {
		    log.warning(e.getMessage());
		}catch (Exception e) {
		    log.warning(e.getMessage());			
		}
		
		fileService.delete(file);
		log.info("emailRHMreport, email sent");
    }

    private void generateRHMcsv(List<Plot> sites, PrintWriter writer, EntityManager mgr) throws IOException {
		log.info("generateRHMcsv, enter");
		writer.println(buildRHMcsvString(sites, mgr));
		writer.close();		
    }

    private String buildRHMtxtString(List<Plot> sites, EntityManager mgr) {
		log.info("buildRHMtxtString, enter");
    	StringBuilder sB = new StringBuilder();

		if (sites == null || sites.size() == 0) {
			sB.append("No sites found\n");
		} else {
			for (Plot site : sites) {
				log.info("buildRHMtxtString, site loop: " + site.getID());
				List<Transect> transects = null;
				sB.append("Site: " + site.getID() + "\n");
				
				try {
					mgr = getEntityManager();
					//log.info("buildRHMtxtString, getting transects");
					Query query = mgr.createQuery("select t from Transect t where t.siteID = :sID").setParameter("sID", site.getID());
					transects = (List<Transect>) query.getResultList();
					//log.info("buildRHMtxtString, successfully queried transects");
					//cursor = JPACursorHelper.getCursor(transects);
					//if (cursor != null)	
						// Tight loop for fetching all entities from datastore and accomodate
						// for lazy fetch.
						//for (Transect obj : transects)
						//	;
				} catch (Exception eX) {
					log.warning(eX.getMessage());
					log.log(Level.SEVERE, "Error during query", eX);
				} finally {
					mgr.close();
				}
				
				if (transects == null || transects.size() == 0) {
					sB.append("    No transect data found for this site\n");
				} else {	
					for (Transect transect : transects) {
						sB.append("    Direction: " + transect.getDirection() + "\n");
						for (Segment segment : transect.getSegments()) {
							sB.append("        Range: " + segment.getRange() + "\n");
							sB.append("            Date: " + segment.getDate() + "\n");
							sB.append("            Canopy Height: " + segment.getCanopyHeight() + "\n");
							sB.append("            Canopy Gap: " + segment.getCanopyGap() + "\n");
							sB.append("            Basal Gap: " + segment.getBasalGap() + "\n");
							sB.append("            Species 1 Density: " + segment.getSpeciesOfInterest1Count() + "\n");
							sB.append("            Species 2 Density: " + segment.getSpeciesOfInterest2Count() + "\n");
							sB.append("            Species List: \n");
							for (String species : segment.getSpeciesList()) {
								sB.append("                " + species + "\n");
							}
							for (StickSegment stickSegment : segment.getStickSegments()) {
								sB.append("            Stick Segment: " + stickSegment.getSegmentIndex() + "\n");
								if (stickSegment.covers[0]) sB.append("                Bare\n");
								if (stickSegment.covers[1]) sB.append("                Trees\n");
								if (stickSegment.covers[2]) sB.append("                Shrubs\n");
								if (stickSegment.covers[3]) sB.append("                Sub-Shrubs\n");
								if (stickSegment.covers[4]) sB.append("                Grasses\n");
								if (stickSegment.covers[5]) sB.append("                Annuals\n");
								if (stickSegment.covers[6]) sB.append("                Herb Litter\n");
								if (stickSegment.covers[7]) sB.append("                Tree Litter\n");
								if (stickSegment.covers[8]) sB.append("                Rock\n");
							}
						}
					}
				}
				
				sB.append("\n--------------------------------------------------------------------------\n" +
							   "--------------------------------------------------------------------------\n\n\n");
			}
		}
		
		log.info("buildRHMtxtString, returning");		
		return sB.toString();
    }
    
    
    private void buildRHMtxtFile(List<Plot> sites, EntityManager mgr, PrintWriter writer) {
		log.info("buildRHMtxtFile, enter");

		if (sites == null || sites.size() == 0) {
			writer.println("No sites found");
		} else {
			for (Plot site : sites) {
				log.info("buildRHMtxtFile, site loop: " + site.getID());
				List<Transect> transects = null;
				writer.println("Site: " + site.getID());
				
				try {
					mgr = getEntityManager();
					log.info("buildRHMtxtFile, getting transects");
					Query query = mgr.createQuery("select t from Transect t where t.siteID = :sID").setParameter("sID", site.getID());
					transects = (List<Transect>) query.getResultList();
					log.info("buildRHMtxtFile, successfully queried transects");
					//cursor = JPACursorHelper.getCursor(transects);
					//if (cursor != null)	
						// Tight loop for fetching all entities from datastore and accomodate
						// for lazy fetch.
						//for (Transect obj : transects)
						//	;
				} catch (Exception eX) {
					log.warning(eX.getMessage());
					log.log(Level.SEVERE, "Error during query", eX);
				} finally {
					mgr.close();
				}
				
				if (transects == null || transects.size() == 0) {
					writer.println("    No transect data found for this site");
				} else {	
					for (Transect transect : transects) {
						writer.println("    Direction: " + transect.getDirection());
						for (Segment segment : transect.getSegments()) {
							writer.println("        Range: " + segment.getRange());
							writer.println("            Date: " + segment.getDate());
							writer.println("            Canopy Height: " + segment.getCanopyHeight());
							writer.println("            Canopy Gap: " + segment.getCanopyGap());
							writer.println("            Basal Gap: " + segment.getBasalGap());
							writer.println("            Species 1 Density: " + segment.getSpeciesOfInterest1Count());
							writer.println("            Species 2 Density: " + segment.getSpeciesOfInterest2Count());
							writer.println("            Species List: \n");
							for (String species : segment.getSpeciesList()) {
								writer.println("                " + species);
							}
							for (StickSegment stickSegment : segment.getStickSegments()) {
								writer.println("            Stick Segment: " + stickSegment.getSegmentIndex());
								if (stickSegment.covers[0]) writer.println("                Bare");
								if (stickSegment.covers[1]) writer.println("                Trees");
								if (stickSegment.covers[2]) writer.println("                Shrubs");
								if (stickSegment.covers[3]) writer.println("                Sub-Shrubs");
								if (stickSegment.covers[4]) writer.println("                Grasses");
								if (stickSegment.covers[5]) writer.println("                Annuals");
								if (stickSegment.covers[6]) writer.println("                Herb Litter");
								if (stickSegment.covers[7]) writer.println("                Tree Litter");
								if (stickSegment.covers[8]) writer.println("                Rock");
							}
						}
					}
				}
				
				writer.println("\n--------------------------------------------------------------------------\n" +
							   "--------------------------------------------------------------------------\n\n");
			}
		}
		
		log.info("buildRHMtxtFile, returning");		
		//return sB.toString();
    }

    
    private void generateRHMtxt(List<Plot> sites, PrintWriter writer, EntityManager mgr) {
		log.info("generateRHMtxt, enter");
    	writer.println(buildRHMtxtString(sites, mgr));
		writer.close();		
    }
    
	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}
 
}
