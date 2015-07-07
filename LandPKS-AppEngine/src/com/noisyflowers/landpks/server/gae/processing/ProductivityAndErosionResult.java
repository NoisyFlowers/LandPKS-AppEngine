package com.noisyflowers.landpks.server.gae.processing;

public class ProductivityAndErosionResult {
	public Double grassProductivity;
	public Double grassErosion;
	public Double maizeProducitivity;
	public Double maizeErosion;
	
	public ProductivityAndErosionResult(Double grassProductivity,
			Double grassErosion, Double maizeProducitivity, Double maizeErosion) {
		super();
		this.grassProductivity = grassProductivity;
		this.grassErosion = grassErosion;
		this.maizeProducitivity = maizeProducitivity;
		this.maizeErosion = maizeErosion;
	}
}
