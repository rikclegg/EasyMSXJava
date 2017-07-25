package com.bloomberg.emsx.samples;

public class Broker {
	
	public static enum AssetClass {
		EQTY,
		FUT,
		OPT,
		MULTILEG_OPT
	}

	public String name;
	public AssetClass assetClass;
	
	Brokers parent;
	
	public BrokerStrategies strategies = null;
	
	Broker(Brokers parent, String name, AssetClass assetClass) {
		this.parent = parent;
		this.name = name;
		this.assetClass = assetClass;
		strategies = new BrokerStrategies(this);
	}

}
