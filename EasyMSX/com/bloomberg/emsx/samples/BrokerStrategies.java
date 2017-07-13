package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;

public class BrokerStrategies implements Iterable<BrokerStrategy> {
	
    private static final Name 	GET_BROKER_STRATEGIES = new Name("GetBrokerStrategiesWithAssetClass");
	private static final Name 	ERROR_INFO = new Name("ErrorInfo");
	
	private ArrayList<BrokerStrategy> strategies = new ArrayList<BrokerStrategy>();
	Broker broker;
	
	BrokerStrategies(Broker broker) {
		this.broker = broker;
		loadStrategies();
	}

	private void loadStrategies() {
		Log.LogMessage(LogLevels.DETAILED, "Broker [" + broker.name + "]: Loading Strategies");
		Request request = broker.parent.emsxapi.emsxService.createRequest(GET_BROKER_STRATEGIES.toString());
    	request.set("EMSX_BROKER", broker.name);
    	request.set("EMSX_ASSET_CLASS", broker.assetClass.toString());
		broker.parent.emsxapi.submitRequest(request,new BrokerStrategiesHandler(this));		
	}
	
	class BrokerStrategiesHandler implements MessageHandler {
		
		BrokerStrategies brokerStrategies;
		
		BrokerStrategiesHandler(BrokerStrategies brokerStrategies) {
			this.brokerStrategies = brokerStrategies;
		}

		@Override
		public void processMessage(Message message) {
			
			Log.LogMessage(LogLevels.DETAILED, "Broker Strategies ["+ broker.name + "]: processing message");
			
	    	if(message.messageType().equals(ERROR_INFO)) {
	        	Log.LogMessage(LogLevels.BASIC, "Broker Strategies ["+ broker.name + "]: processing RESPONSE error");
	    		Integer errorCode = message.getElementAsInt32("ERROR_CODE");
	    		String errorMessage = message.getElementAsString("ERROR_MESSAGE");
	    		Log.LogMessage(LogLevels.BASIC, "Broker Strategies ["+ broker.name + "]: [" + errorCode + "] " + errorMessage);
	    	} else if(message.messageType().equals(GET_BROKER_STRATEGIES)) {
	        	Log.LogMessage(LogLevels.DETAILED, "Broker Strategies ["+ broker.name + "]: processing succesful RESPONSE");
	    		
	    		Element strategies = message.getElement("EMSX_STRATEGIES");
	    		
				int numValues = strategies.numValues();
	    		
	    		for(int i = 0; i < numValues; i++) {
	    			
	    			String strategy = strategies.getValueAsString(i);
	    			if(!strategy.isEmpty()) {
	    				BrokerStrategy newBrokerStrategy = new BrokerStrategy(brokerStrategies, strategy);
	    				brokerStrategies.add(newBrokerStrategy);
	    	        	Log.LogMessage(LogLevels.DETAILED, "Broker Strategies ["+ broker.name + "]: added new strategy " + newBrokerStrategy.name);
	    			}
	    		}
	    	}		
	    }
	}

	@Override
	public Iterator<BrokerStrategy> iterator() {
		return strategies.iterator();
	}

	public BrokerStrategy get(int index) {
		return strategies.get(index);
	}
	
	public BrokerStrategy get(String name) {
		for (BrokerStrategy s : strategies){
			if(s.name.equalsIgnoreCase(name))
			 return s;
		}
		return null;
	}
	
	public void add(BrokerStrategy newBrokerStrategy) {
		strategies.add(newBrokerStrategy);
	}

}
