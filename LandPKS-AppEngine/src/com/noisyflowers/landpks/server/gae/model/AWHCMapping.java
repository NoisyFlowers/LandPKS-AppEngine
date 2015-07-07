package com.noisyflowers.landpks.server.gae.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.noisyflowers.landpks.server.gae.util.Constants.RockFragmentRange;
import com.noisyflowers.landpks.server.gae.util.Constants.SoilTexture;

@Entity
public class AWHCMapping {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ID;

	String texture;
	String rockFragment;
	//SoilTexture texture;  //TODO: this would be nice
	//RockFragmentRange rockFragment;
	Double awhcValue;
	
	public AWHCMapping() {
		super();
	}
	
	public AWHCMapping(String texture, String rockFragment,
			Double awhcValue) {
		super();
		this.texture = texture;
		this.rockFragment = rockFragment;
		this.awhcValue = awhcValue;
	}

	public Long getID() {
		return ID;
	}

	public void setID(Long iD) {
		ID = iD;
	}

	public String getTexture() {
		return texture;
	}
	public void setTexture(String texture) {
		this.texture = texture;
	}
	public String getRockFragment() {
		return rockFragment;
	}
	public void setRockFragment(String rockFragment) {
		this.rockFragment = rockFragment;
	}
	public Double getAwhcValue() {
		return awhcValue;
	}
	public void setAwhcValue(Double awhcValue) {
		this.awhcValue = awhcValue;
	}
	
	  @PrePersist
	  @PreUpdate
	  public void prePersist() {
	    if (texture != null) {
	    	texture = texture.toUpperCase();
	    }
	    if (rockFragment != null) {
	    	rockFragment = rockFragment.toUpperCase();
	    }
	  } 	
}
