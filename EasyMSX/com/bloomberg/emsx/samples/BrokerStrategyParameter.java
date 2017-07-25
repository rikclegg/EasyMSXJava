package com.bloomberg.emsx.samples;

public class BrokerStrategyParameter {

	public String name;
	public String value;
	public int disable;
	
	BrokerStrategyParameters parent = null;
	
	BrokerStrategyParameter(BrokerStrategyParameters brokerStrategyParameters, String name, String value, int disable) {
		this.name = name;
		this.value = value;
		this.disable = disable;
		this.parent = brokerStrategyParameters;
	}
}
