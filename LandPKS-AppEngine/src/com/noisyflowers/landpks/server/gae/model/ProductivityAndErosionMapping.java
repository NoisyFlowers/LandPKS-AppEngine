package com.noisyflowers.landpks.server.gae.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@Entity
public class ProductivityAndErosionMapping {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ID;

	private String surfaceCracked;
	private String topLayerSoilTexture;
	private String slope;
	private Double awhcTotal;
	private Double grassProductivity;
	private Double grassErosion;
	private Double maizeProductivity;
	private Double maizeErosion;
	
	public ProductivityAndErosionMapping() {
	}

	public Long getID() {
		return ID;
	}

	public void setID(Long iD) {
		ID = iD;
	}

	public String getSurfaceCracked() {
		return surfaceCracked;
	}

	public void setSurfaceCracked(String surfaceCracked) {
		this.surfaceCracked = surfaceCracked;
	}

	public String getTopLayerSoilTexture() {
		return topLayerSoilTexture;
	}

	public void setTopLayerSoilTexture(String soilTexture) {
		this.topLayerSoilTexture = soilTexture;
	}

	public String getSlope() {
		return slope;
	}

	public void setSlope(String slope) {
		this.slope = slope;
	}

	public Double getAwhcTotal() {
		return awhcTotal;
	}

	public void setAwhcTotal(Double awhcTotal) {
		this.awhcTotal = awhcTotal;
	}

	public Double getGrassProductivity() {
		return grassProductivity;
	}

	public void setGrassProductivity(Double grassProductivity) {
		this.grassProductivity = grassProductivity;
	}

	public Double getGrassErosion() {
		return grassErosion;
	}

	public void setGrassErosion(Double grassErosion) {
		this.grassErosion = grassErosion;
	}

	public Double getMaizeProductivity() {
		return maizeProductivity;
	}

	public void setMaizeProductivity(Double maizeProductivity) {
		this.maizeProductivity = maizeProductivity;
	}

	public Double getMaizeErosion() {
		return maizeErosion;
	}

	public void setMaizeErosion(Double maizeErosion) {
		this.maizeErosion = maizeErosion;
	}
	
	  @PrePersist
	  @PreUpdate
	  public void prePersist() {
	    if (surfaceCracked != null) {
	    	surfaceCracked = surfaceCracked.toUpperCase();
	    }
	    if (topLayerSoilTexture != null) {
	    	topLayerSoilTexture = topLayerSoilTexture.toUpperCase();
	    }
	    if (slope != null) {
	    	slope = slope.toUpperCase();
	    }
	  } 	
	
}
