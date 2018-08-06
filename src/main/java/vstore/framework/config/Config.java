package vstore.framework.config;

import vstore.framework.matching.Matching;

class Config {
	
	//Customizable properties of a vStore configuration
	
	public int defaultRMSThreshold;
	public int defaultDBThreshold;

	public boolean multipleNodesPerRule;
	public Matching.MatchingMode matchingMode;
}
