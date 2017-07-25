package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;

public class BrokerStrategyParameters implements Iterable<BrokerStrategyParameter>{

	private static final Name 	GET_BROKER_STRATEGY_INFO = new Name("GetBrokerStrategyInfoWithAssetClass");
	private static final Name 	ERROR_INFO = new Name("ErrorInfo");

	private ArrayList<BrokerStrategyParameter> parameters = new ArrayList<BrokerStrategyParameter>();
	BrokerStrategy brokerStrategy;
	
	BrokerStrategyParameters(BrokerStrategy brokerStrategy) {
		this.brokerStrategy = brokerStrategy;
		loadStrategyParameters();
	}
	
	private void loadStrategyParameters() {
		Log.LogMessage(LogLevels.DETAILED, "Broker Strategy [" + brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "]: Loading strategy parameters");
		Request request = brokerStrategy.parent.broker.parent.emsxapi.emsxService.createRequest(GET_BROKER_STRATEGY_INFO.toString());
    	request.set("EMSX_BROKER", brokerStrategy.parent.broker.name);
    	request.set("EMSX_STRATEGY", brokerStrategy.name);
    	request.set("EMSX_ASSET_CLASS", brokerStrategy.parent.broker.assetClass.toString());
    	brokerStrategy.parent.broker.parent.emsxapi.submitRequest(request,new BrokerStrategyParametersHandler(this));		
	}
	
	class BrokerStrategyParametersHandler implements MessageHandler {
		
		BrokerStrategyParameters brokerStrategyParameters;
		
		BrokerStrategyParametersHandler(BrokerStrategyParameters brokerStrategyParameters) {
			this.brokerStrategyParameters = brokerStrategyParameters;
		}

		@Override
		public void processMessage(Message message) {
			
			Log.LogMessage(LogLevels.DETAILED, "Broker Strategy Parameters ["+ brokerStrategyParameters.brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "]: processing message");
			
	    	if(message.messageType().equals(ERROR_INFO)) {
	        	Log.LogMessage(LogLevels.DETAILED, "Broker Strategy Parameters ["+ brokerStrategyParameters.brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "]: processing RESPONSE error");
	    		Integer errorCode = message.getElementAsInt32("ERROR_CODE");
	    		String errorMessage = message.getElementAsString("ERROR_MESSAGE");
	    		Log.LogMessage(LogLevels.DETAILED, "Broker Strategy Parameters ["+ brokerStrategyParameters.brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "]: [" + errorCode + "] " + errorMessage);
	    	} else if(message.messageType().equals(GET_BROKER_STRATEGY_INFO)) {
	        	Log.LogMessage(LogLevels.DETAILED, "Broker Strategy Parameters ["+ brokerStrategyParameters.brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "]: processing succesful RESPONSE");
	    		
	    		Element parameters = message.getElement("EMSX_STRATEGY_INFO");
	    		
				int numValues = parameters.numValues();
	    		
	    		for(int i = 0; i < numValues; i++) {
	    			
	    			Element parameter = parameters.getValueAsElement(i);

	    			String parameterName = parameter.getElementAsString("FieldName");
	    			int disable = parameter.getElementAsInt32("Disable");
	    			String stringValue = parameter.getElementAsString("StringValue");

	    			BrokerStrategyParameter newParameter = new BrokerStrategyParameter(brokerStrategyParameters, parameterName,stringValue,disable);
	    			brokerStrategyParameters.add(newParameter);
	            	Log.LogMessage(LogLevels.DETAILED, "Broker Strategy Parameters ["+ brokerStrategyParameters.brokerStrategy.parent.broker.name + "." + brokerStrategy.name + "] Added new parameter " + parameterName);
	    		}
	    	}		
	    }
	}

	@Override
	public Iterator<BrokerStrategyParameter> iterator() {
		return parameters.iterator();
	}

	public BrokerStrategyParameter get(int index) {
		return parameters.get(index);
	}
	
	public void add(BrokerStrategyParameter newBrokerStrategyParameter) {
		parameters.add(newBrokerStrategyParameter);
	}


}
