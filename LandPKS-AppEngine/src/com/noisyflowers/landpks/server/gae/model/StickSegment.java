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
 * StickSegment.java
 */

package com.noisyflowers.landpks.server.gae.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.google.appengine.api.datastore.Key;

//TODO: We're not supposed to reference classes in repackaged.  I tried to use the original classes from a separate jar.  
//This worked on my local machine but not when deployed.  Some library conflict I guess.  Still need to work this out.  
//In the meantime, fully specifying path in the annotation prevents compile error.
//import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonBackReference;
//import org.codehaus.jackson.annotate.JsonBackReference;

@Entity
public class StickSegment {
	
	//TODO: not sure what, if anything to do with this enum
	public enum Cover {
		COVER_1 ("Bare"),
		COVER_2 ("Trees"),
		COVER_3 ("Shrubs"),
		COVER_4 ("Sub-shrubs"),
		COVER_5 ("Perennial grasses"),
		COVER_6 ("Annuals"),
		COVER_7 ("Herb litter"),
		COVER_8 ("Wood litter"),
		COVER_9 ("Rock");
		
		public final String name;
		
		Cover(String name) {
			this.name = name;
		}
	}
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key ID;

	//public Long segmentID; 
	@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	public Segment segment; 
	
	public int segmentIndex;
	public boolean[] covers = new boolean[Cover.values().length];
	
	public Key getID() {
		return ID;
	}
	public void setID(Key iD) {
		ID = iD;
	}
	
	//@com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonIgnore
	public Segment getSegment() {
		return segment;
	}
	public void setSegment(Segment segment) {
		this.segment = segment;
	}
	
	public int getSegmentIndex() {
		return segmentIndex;
	}
	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
	}
	
	public boolean[] getCovers() {
		return covers;
	}
	public void setCovers(boolean[] covers) {
		this.covers = covers;
	}

	
}
