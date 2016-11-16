package com.bloomberg.emsx.samples;

import java.util.Iterator;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Request;

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
