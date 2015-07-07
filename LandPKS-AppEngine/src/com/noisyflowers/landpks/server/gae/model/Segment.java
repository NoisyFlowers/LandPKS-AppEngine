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
 * com.noisyflowers.landpks.server.gae.model
 * Segment.java
 */

package com.noisyflowers.landpks.server.gae.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

//TODO: We're not supposed to reference classes in repackaged.  I tried to use the original classes from a separate jar.  
//This worked on my local machine but not when deployed.  Some library conflict I guess.  Still need to work this out.  
//In the meantime, fully specifying path in the annotation prevents compile error.
//import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonBackReference;
//import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonManagedReference;
//import org.codehaus.jackson.annotate.JsonBackReference;
//import org.codehaus.jackson.annotate.JsonManagedReference;


@Entity
public class Segment {
	public static final int STICK_SEGMENT_COUNT = 6;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key ID;
	//private String ID;


    //private Long transectID;
	@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
    private Transect transect;

	private String range;
    
	//public StickSegment[] stickSegments = new StickSegment[STICK_SEGMENT_COUNT];
	@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonManagedReference
	@OneToMany(mappedBy = "segment", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<StickSegment> stickSegments = new ArrayList<StickSegment>();
    
    private String canopyHeight;
    private Boolean basalGap;
    private Boolean canopyGap;
    
    private Integer species1Density;
    private Integer species2Density;
    
    private List<String> speciesList;
    
    private String date;
	
    
	public Key getID() {
		return ID;
	}
	public void setID(Key iD) {
		this.ID = iD;
	}
	
	//@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonIgnore
	public Transect getTransect() {
		return transect;
	}
	public void setTransect(Transect transect) {
		this.transect = transect;
	}
	
	public String getRange() {
		return range;
	}
	public void setRange(String range) {
		this.range = range;
	}
	
	public List<StickSegment> getStickSegments() {
		return stickSegments;
	}
	public void setStickSegments(List<StickSegment> stickSegments) {
		this.stickSegments = stickSegments;
	}
	
	public String getCanopyHeight() {
		return canopyHeight;
	}
	public void setCanopyHeight(String canopyHeight) {
		this.canopyHeight = canopyHeight;
	}
	
	public Boolean getBasalGap() {
		return basalGap;
	}
	public void setBasalGap(Boolean basalGap) {
		this.basalGap = basalGap;
	}
	
	public Boolean getCanopyGap() {
		return canopyGap;
	}
	public void setCanopyGap(Boolean canopyGap) {
		this.canopyGap = canopyGap;
	}

	public Integer getSpecies1Density() {
		return species1Density;
	}
	public void setSpecies1Density(Integer species1Density) {
		this.species1Density = species1Density;
	}

	public Integer getSpecies2Density() {
		return species2Density;
	}
	public void setSpecies2Density(Integer species2Density) {
		this.species2Density = species2Density;
	}

	public List<String> getSpeciesList() {
		return speciesList;
	}
	public void setSpeciesList(List<String> speciesList) {
		this.speciesList = speciesList;
	}
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}	

}
