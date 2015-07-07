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
 * com.noisyflowers.landpks.server.gae.util
 * Constants.java
 */

package com.noisyflowers.landpks.server.gae.util;


public class Constants {

	public enum SoilHorizonName {
		HORIZON_1_NAME ("0-1cm"),
		HORIZON_2_NAME ("1-10cm"),
		HORIZON_3_NAME ("10-20cm"),
		HORIZON_4_NAME ("20-50cm"),
		HORIZON_5_NAME ("50-70cm"),
		HORIZON_6_NAME (">70cm");
		
		public final String name;
		
		SoilHorizonName(String name) {
			this.name = name;
		}
	}
	
	public enum SoilTexture {
		SAND ("Sand"),
		LOAMY_SAND ("Loamy sand"),
		SANDY_LOAM ("Sandy loam"),
		SILT_LOAM ("Silt loam"),
		LOAM ("Loam"),
		SANDY_CLAY_LOAM ("Sandy clay loam"),
		SILTY_CLAY_LOAM ("Silty clay loam"),
		CLAY_LOAM ("Clay loam"),
		SANDY_CLAY ("Sandy clay"),
		SILTY_CLAY ("Silty clay"),
		CLAY ("Clay");
		
		public final String name;
		
		SoilTexture(String name) {
			this.name = name;
		}
	}

	public enum RockFragmentRange {
		RANGE_1 ("0-1%"),
		RANGE_2 ("1-10%"),
		RANGE_3 ("10-20%"),
		RANGE_4 ("20-50%"),
		RANGE_5 ("50-70%"),
		RANGE_6 (">70%");
		
		public final String name;
		
		RockFragmentRange(String name) {
			this.name = name;
		}
	}
    
}
