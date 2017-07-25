package com.bloomberg.emsx.samples;

public class BrokerStrategy {

	public String name;
	
	BrokerStrategies parent;
	
	public BrokerStrategyParameters parameters = null;
	
	BrokerStrategy(BrokerStrategies parent, String name) {
		this.parent = parent;
		this.name = name;
		parameters = new BrokerStrategyParameters(this);
	}


}
