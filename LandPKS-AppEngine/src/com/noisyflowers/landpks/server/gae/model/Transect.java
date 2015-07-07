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
 * Transect.java
 */

package com.noisyflowers.landpks.server.gae.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

//TODO: We're not supposed to reference classes in repackaged.  I tried to use the original classes from a separate jar.  
//This worked on my local machine but not when deployed.  Some library conflict I guess.  Still need to work this out.  
//In the meantime, fully specifying path in the annotation prevents compile error.
//import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonManagedReference;
//import org.codehaus.jackson.annotate.JsonManagedReference;

@Entity
public class Transect {
	@Id
	private String ID;
	
	private String siteID;
	private String direction;
	private String dominantWoodySpecies;
	private String dominantNonwoodySpecies;
	private Date modifiedDate = new Date(1);

	@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonManagedReference
    @OneToMany(mappedBy = "transect", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Segment> segments = new ArrayList<Segment>();

	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		this.ID = iD;
	}

	public String getSiteID() {
		return siteID;
	}
	public void setSiteID(String siteID) {
		this.siteID = siteID;
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getDominantWoodySpecies() {
		return dominantWoodySpecies;
	}
	public void setDominantWoodySpecies(String species) {
		this.dominantWoodySpecies = species;
	}
	public String getDominantNonwoodySpecies() {
		return dominantNonwoodySpecies;
	}
	public void setDominantNonwoodySpecies(String species) {
		this.dominantNonwoodySpecies = species;
	}

	public List<Segment> getSegments() {
		return segments;
	}
	public void setSegments(List<Segment> segments) {
		this.segments = segments;
	}
	
	public Date getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Date newDate) {
		this.modifiedDate = newDate;
	}
	
	
}
