package vstoreframework.config;

import vstoreframework.matching.Matching;

class Config {
	
	//Customizable properties of a vStore configuration
	
	public int defaultRMSThreshold;
	public int defaultDBThreshold;

	public boolean multipleNodesPerRule;
	public Matching.MatchingMode matchingMode;
}
