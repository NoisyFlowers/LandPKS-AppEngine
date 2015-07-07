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
 * Plot.java
 */

package com.noisyflowers.landpks.server.gae.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


@Entity
public class Plot {
	
	public enum Grazing {
		NONE ("none"),
		CATTLE ("cattle"),
		GOAT ("goat"),
		SHEEP ("sheep"),
		WILDLIFE ("wildlife");
		
		public final String name;
		
		Grazing(String name) {
			this.name = name;
		}
	}

	
	@Id
	private String ID;
	
	//Id
	//GeneratedValue(strategy = GenerationType.IDENTITY)
	//private Long ID;
	
	private String name;
	private Boolean testPlot;
	private String recorderName;
	private String organization;
	private Double latitude;
	private Double longitude;
	private String city;
	private Date modifiedDate;
	private String landCover;
	private Boolean grazed;
	private List<Grazing> grazing;
	private Boolean flooding;
	private String slope;
	
	private String slopeShape;
	
	//TODO: Using arrays for these did not work.  The underlying REST interface threw away null values and lost positional relationships.  Might be worth looking into later.
	//private List<String> soilHorizonsRockFragments;
	//private List<Integer> soilHorizonsColors;
	//private List<String> soilHorizonsTextures;	
	
	private String rockFragmentForSoilHorizon1;
	private String rockFragmentForSoilHorizon2;
	private String rockFragmentForSoilHorizon3;
	private String rockFragmentForSoilHorizon4;
	private String rockFragmentForSoilHorizon5;
	private String rockFragmentForSoilHorizon6;
	private String rockFragmentForSoilHorizon7;
	
	private Integer colorForSoilHorizon1;
	private Integer colorForSoilHorizon2;
	private Integer colorForSoilHorizon3;
	private Integer colorForSoilHorizon4;
	private Integer colorForSoilHorizon5;
	private Integer colorForSoilHorizon6;
	private Integer colorForSoilHorizon7;

	private String textureForSoilHorizon1;
	private String textureForSoilHorizon2;
	private String textureForSoilHorizon3;
	private String textureForSoilHorizon4;
	private String textureForSoilHorizon5;
	private String textureForSoilHorizon6;
	private String textureForSoilHorizon7;
	
	private Integer bedrockDepth;
	private Integer stoppedDiggingDepth;

	private Boolean surfaceCracking;
	private Boolean surfaceSalt;
	
	private String landscapeNorthPhotoURL;
	private String landscapeEastPhotoURL;
	private String landscapeSouthPhotoURL;
	private String landscapeWestPhotoURL;
	private String soilPitPhotoURL;
	private String soilSamplesPhotoURL;
	
	private String recommendation;
	private Double grassProductivity;
	private Double grassErosion;
	//private Double maizeProductivity;
	//private Double maizeErosion;
	private Double cropProductivity;
	private Double cropErosion;
	
	private Double gdalElevation;
	private Double gdalAridityIndex;
	private String gdalFaoLgp;
	private Double awcSoilProfile;
	private Double averageAnnualPrecipitation;
	
	private List<Double> monthlyPrecipitation;
	private List<Double> monthlyAvgTemperature;
	private List<Double> monthlyMinTemperature;
	private List<Double> monthlyMaxTemperature;
	
	/***
	public Plot() {	
	}
	***/
	
	/***
	public Long getID() {
		return ID;
	}
	public void setID(Long iD) {
		ID = iD;
	}
	***/
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Boolean isTestPlot() {
		return testPlot;
	}
	public void setTestPlot(Boolean value) {
		this.testPlot = value;
	}

	public String getRecorderName() {
		return recorderName;
	}
	public void setRecorderName(String recorderName) {
		this.recorderName = recorderName;
	}
	
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	
	public Date getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
	public String getLandCover() {
		return landCover;
	}
	public void setLandCover(String landCover) {
		this.landCover = landCover;
	}
	
	public Boolean isGrazed() {
		return grazed;
	}
	public void setGrazed(Boolean grazed) {
		this.grazed = grazed;
	}
	
	public Boolean isFlooding() {
		return flooding;
	}
	public void setFlooding(Boolean flooding) {
		this.flooding = flooding;
	}
	
	public String getSlope() {
		return slope;
	}
	public void setSlope(String slope) {
		this.slope = slope;
	}
	
	public String getSlopeShape() {
		return slopeShape;
	}
	public void setSlopeShape(String slopeShape) {
		this.slopeShape = slopeShape;
	}
	
	public Boolean isSurfaceCracking() {
		return surfaceCracking;
	}
	public void setSurfaceCracking(Boolean surfaceCracking) {
		this.surfaceCracking = surfaceCracking;
	}

	public Boolean isSurfaceSalt() {
		return surfaceSalt;
	}
	public void setSurfaceSalt(Boolean surfaceSalt) {
		this.surfaceSalt = surfaceSalt;
	}
	
	public String getRecommendation() {
		return recommendation;
	}
	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}
	public String getRockFragmentForSoilHorizon1() {
		return rockFragmentForSoilHorizon1;
	}
	public void setRockFragmentForSoilHorizon1(String rockFragmentForSoilHorizon1) {
		this.rockFragmentForSoilHorizon1 = rockFragmentForSoilHorizon1;
	}
	public String getRockFragmentForSoilHorizon2() {
		return rockFragmentForSoilHorizon2;
	}
	public void setRockFragmentForSoilHorizon2(String rockFragmentForSoilHorizon2) {
		this.rockFragmentForSoilHorizon2 = rockFragmentForSoilHorizon2;
	}
	public String getRockFragmentForSoilHorizon3() {
		return rockFragmentForSoilHorizon3;
	}
	public void setRockFragmentForSoilHorizon3(String rockFragmentForSoilHorizon3) {
		this.rockFragmentForSoilHorizon3 = rockFragmentForSoilHorizon3;
	}
	public String getRockFragmentForSoilHorizon4() {
		return rockFragmentForSoilHorizon4;
	}
	public void setRockFragmentForSoilHorizon4(String rockFragmentForSoilHorizon4) {
		this.rockFragmentForSoilHorizon4 = rockFragmentForSoilHorizon4;
	}
	public String getRockFragmentForSoilHorizon5() {
		return rockFragmentForSoilHorizon5;
	}
	public void setRockFragmentForSoilHorizon5(String rockFragmentForSoilHorizon5) {
		this.rockFragmentForSoilHorizon5 = rockFragmentForSoilHorizon5;
	}
	public String getRockFragmentForSoilHorizon6() {
		return rockFragmentForSoilHorizon6;
	}
	public void setRockFragmentForSoilHorizon6(String rockFragmentForSoilHorizon6) {
		this.rockFragmentForSoilHorizon6 = rockFragmentForSoilHorizon6;
	}
	public String getRockFragmentForSoilHorizon7() {
		return rockFragmentForSoilHorizon7;
	}
	public void setRockFragmentForSoilHorizon7(String rockFragmentForSoilHorizon7) {
		this.rockFragmentForSoilHorizon7 = rockFragmentForSoilHorizon7;
	}
	public Integer getColorForSoilHorizon1() {
		return colorForSoilHorizon1;
	}
	public void setColorForSoilHorizon1(Integer colorForSoilHorizon1) {
		this.colorForSoilHorizon1 = colorForSoilHorizon1;
	}
	public Integer getColorForSoilHorizon2() {
		return colorForSoilHorizon2;
	}
	public void setColorForSoilHorizon2(Integer colorForSoilHorizon2) {
		this.colorForSoilHorizon2 = colorForSoilHorizon2;
	}
	public Integer getColorForSoilHorizon3() {
		return colorForSoilHorizon3;
	}
	public void setColorForSoilHorizon3(Integer colorForSoilHorizon3) {
		this.colorForSoilHorizon3 = colorForSoilHorizon3;
	}
	public Integer getColorForSoilHorizon4() {
		return colorForSoilHorizon4;
	}
	public void setColorForSoilHorizon4(Integer colorForSoilHorizon4) {
		this.colorForSoilHorizon4 = colorForSoilHorizon4;
	}
	public Integer getColorForSoilHorizon5() {
		return colorForSoilHorizon5;
	}
	public void setColorForSoilHorizon5(Integer colorForSoilHorizon5) {
		this.colorForSoilHorizon5 = colorForSoilHorizon5;
	}
	public Integer getColorForSoilHorizon6() {
		return colorForSoilHorizon6;
	}
	public void setColorForSoilHorizon6(Integer colorForSoilHorizon6) {
		this.colorForSoilHorizon6 = colorForSoilHorizon6;
	}
	public Integer getColorForSoilHorizon7() {
		return colorForSoilHorizon7;
	}
	public void setColorForSoilHorizon7(Integer colorForSoilHorizon7) {
		this.colorForSoilHorizon7 = colorForSoilHorizon7;
	}
	public String getTextureForSoilHorizon1() {
		return textureForSoilHorizon1;
	}
	public void setTextureForSoilHorizon1(String textureForSoilHorizon1) {
		this.textureForSoilHorizon1 = textureForSoilHorizon1;
	}
	public String getTextureForSoilHorizon2() {
		return textureForSoilHorizon2;
	}
	public void setTextureForSoilHorizon2(String textureForSoilHorizon2) {
		this.textureForSoilHorizon2 = textureForSoilHorizon2;
	}
	public String getTextureForSoilHorizon3() {
		return textureForSoilHorizon3;
	}
	public void setTextureForSoilHorizon3(String textureForSoilHorizon3) {
		this.textureForSoilHorizon3 = textureForSoilHorizon3;
	}
	public String getTextureForSoilHorizon4() {
		return textureForSoilHorizon4;
	}
	public void setTextureForSoilHorizon4(String textureForSoilHorizon4) {
		this.textureForSoilHorizon4 = textureForSoilHorizon4;
	}
	public String getTextureForSoilHorizon5() {
		return textureForSoilHorizon5;
	}
	public void setTextureForSoilHorizon5(String textureForSoilHorizon5) {
		this.textureForSoilHorizon5 = textureForSoilHorizon5;
	}
	public String getTextureForSoilHorizon6() {
		return textureForSoilHorizon6;
	}
	public void setTextureForSoilHorizon6(String textureForSoilHorizon6) {
		this.textureForSoilHorizon6 = textureForSoilHorizon6;
	}
	public String getTextureForSoilHorizon7() {
		return textureForSoilHorizon7;
	}
	public void setTextureForSoilHorizon7(String textureForSoilHorizon7) {
		this.textureForSoilHorizon7 = textureForSoilHorizon7;
	}
	
	public Integer getBedrockDepth() {
		return bedrockDepth;
	}
	public void setBedrockDepth(Integer bedrockDepth) {
		this.bedrockDepth = bedrockDepth;
	}

	public Integer getStoppedDiggingDepth() {
		return stoppedDiggingDepth;
	}
	public void setStoppedDiggingDepth(Integer stoppedDiggingDepth) {
		this.stoppedDiggingDepth = stoppedDiggingDepth;
	}

	
	public String getLandscapeNorthPhotoURL() {
		return landscapeNorthPhotoURL;
	}
	public void setLandscapeNorthPhotoURL(String landscapeNorthPhotoURL) {
		this.landscapeNorthPhotoURL = landscapeNorthPhotoURL;
	}
	public String getLandscapeEastPhotoURL() {
		return landscapeEastPhotoURL;
	}
	public void setLandscapeEastPhotoURL(String landscapeEastPhotoURL) {
		this.landscapeEastPhotoURL = landscapeEastPhotoURL;
	}
	public String getLandscapeSouthPhotoURL() {
		return landscapeSouthPhotoURL;
	}
	public void setLandscapeSouthPhotoURL(String landscapeSouthPhotoURL) {
		this.landscapeSouthPhotoURL = landscapeSouthPhotoURL;
	}
	public String getLandscapeWestPhotoURL() {
		return landscapeWestPhotoURL;
	}
	public void setLandscapeWestPhotoURL(String landscapeWestPhotoURL) {
		this.landscapeWestPhotoURL = landscapeWestPhotoURL;
	}
	public String getSoilPitPhotoURL() {
		return soilPitPhotoURL;
	}
	public void setSoilPitPhotoURL(String soilPitPhotoURL) {
		this.soilPitPhotoURL = soilPitPhotoURL;
	}
	public String getSoilSamplesPhotoURL() {
		return soilSamplesPhotoURL;
	}
	public void setSoilSamplesPhotoURL(String soilSamplesPhotoURL) {
		this.soilSamplesPhotoURL = soilSamplesPhotoURL;
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
	
	/***
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
	***/
	public Double getCropProductivity() {
		return cropProductivity;
	}
	public void setCropProductivity(Double cropProductivity) {
		this.cropProductivity = cropProductivity;
	}
	public Double getCropErosion() {
		return cropErosion;
	}
	public void setCropErosion(Double cropErosion) {
		this.cropErosion = cropErosion;
	}

	public Double getGdalElevation() {
		return gdalElevation;
	}
	public void setGdalElevation(Double gdalElevation) {
		this.gdalElevation = gdalElevation;
	}
	public Double getGdalAridityIndex() {
		return gdalAridityIndex;
	}
	public void setGdalAridityIndex(Double gdalAridityIndex) {
		this.gdalAridityIndex = gdalAridityIndex;
	}
	public String getGdalFaoLgp() {
		return gdalFaoLgp;
	}
	public void setGdalFaoLgp(String gdalFaoLgp) {
		this.gdalFaoLgp = gdalFaoLgp;
	}
	public Double getAwcSoilProfile() {
		return awcSoilProfile;
	}
	public void setAwcSoilProfile(Double awcSoilProfile) {
		this.awcSoilProfile = awcSoilProfile;
	}
	public Double getAverageAnnualPrecipitation() {
		return averageAnnualPrecipitation;
	}
	public void setAverageAnnualPrecipitation(Double averageAnnualPrecipitation) {
		this.averageAnnualPrecipitation = averageAnnualPrecipitation;
	}
	public List<Double> getMonthlyPrecipitation() {
		return monthlyPrecipitation;
	}
	public void setMonthlyPrecipitation(List<Double> monthlyPrecipitation) {
		this.monthlyPrecipitation = monthlyPrecipitation;
	}
	public List<Double> getMonthlyAvgTemperature() {
		return monthlyAvgTemperature;
	}
	public void setMonthlyAvgTemperature(List<Double> monthlyAvgTemperature) {
		this.monthlyAvgTemperature = monthlyAvgTemperature;
	}
	public List<Double> getMonthlyMinTemperature() {
		return monthlyMinTemperature;
	}
	public void setMonthlyMinTemperature(List<Double> monthlyMinTemperature) {
		this.monthlyMinTemperature = monthlyMinTemperature;
	}
	public List<Double> getMonthlyMaxTemperature() {
		return monthlyMaxTemperature;
	}
	public void setMonthlyMaxTemperature(List<Double> monthlyMaxTemperature) {
		this.monthlyMaxTemperature = monthlyMaxTemperature;
	}
	
	public List<Grazing> getGrazing() {
		return grazing;
	}
	
	public void setGrazing(List<Grazing> grazing) {
		this.grazing = grazing;
	}
	

	//public List<String> getSoilHorizonsRockFragments() {
	//	return soilHorizonsRockFragments;
	//}
	//public void setSoilHorizonsRockFragments(List<String> soilHorizonsRockFragments) {
	//	this.soilHorizonsRockFragments = soilHorizonsRockFragments;
	//}

	//public List<Integer> getSoilHorizonsColors() {
	//	return soilHorizonsColors;
	//}
	//public void setSoilHorizonsColors(List<Integer> soilHorizonsColors) {
	//	this.soilHorizonsColors = soilHorizonsColors;
	//}

	//public List<String> getSoilHorizonsTextures() {
	//	return soilHorizonsTextures;
	//}
	//public void setSoilHorizonsTextures(List<String> soilHorizonsTextures) {
	//	this.soilHorizonsTextures = soilHorizonsTextures;
	//}
	
}
