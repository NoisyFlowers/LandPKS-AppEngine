package com.noisyflowers.landpks.server.gae.processing;

public class AWHCKey {
	private String soilTexture;
	private String soilGravel;
	
	public AWHCKey() {
		super();
	}
	public AWHCKey(String soilTexture, String soilGravel) {
		super();
		this.soilTexture = soilTexture;
		this.soilGravel = soilGravel;
	}
	
	public String getSoilTexture() {
		return soilTexture;
	}
	public void setSoilTexture(String soilTexture) {
		this.soilTexture = soilTexture;
	}
	public String getSoilGravel() {
		return soilGravel;
	}
	public void setSoilGravel(String soilGravel) {
		this.soilGravel = soilGravel;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((soilGravel == null) ? 0 : soilGravel.hashCode());
		result = prime * result
				+ ((soilTexture == null) ? 0 : soilTexture.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AWHCKey other = (AWHCKey) obj;
		if (soilGravel == null) {
			if (other.soilGravel != null)
				return false;
		} else if (!soilGravel.equals(other.soilGravel))
			return false;
		if (soilTexture == null) {
			if (other.soilTexture != null)
				return false;
		} else if (!soilTexture.equals(other.soilTexture))
			return false;
		return true;
	}
	

}
