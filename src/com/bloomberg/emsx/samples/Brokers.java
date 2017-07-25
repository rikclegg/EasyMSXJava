package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;

public class Brokers implements Iterable<Broker> {
	
	private static final Name 	GET_BROKERS = new Name("GetBrokersWithAssetClass");
	private static final Name 	ERROR_INFO = new Name("ErrorInfo");

	private ArrayList<Broker> brokers = new ArrayList<Broker>();
    EasyMSX emsxapi;
    
	Brokers(EasyMSX emsxapi) {
		this.emsxapi = emsxapi;
		loadBrokers();
	}
	
	private void loadBrokers() {

		Log.LogMessage(LogLevels.BASIC,"Brokers: Loading");
	
		Request reqEQTY = emsxapi.emsxService.createRequest(GET_BROKERS.toString());
		reqEQTY.set("EMSX_ASSET_CLASS", Broker.AssetClass.EQTY.toString());
		emsxapi.submitRequest(reqEQTY,new BrokersHandler(this, Broker.AssetClass.EQTY));

		Request reqOPT = emsxapi.emsxService.createRequest(GET_BROKERS.toString());
		reqOPT.set("EMSX_ASSET_CLASS", Broker.AssetClass.OPT.toString());
		emsxapi.submitRequest(reqOPT,new BrokersHandler(this, Broker.AssetClass.OPT));

		Request reqFUT = emsxapi.emsxService.createRequest(GET_BROKERS.toString());
		reqFUT.set("EMSX_ASSET_CLASS", Broker.AssetClass.FUT.toString());
		emsxapi.submitRequest(reqFUT,new BrokersHandler(this, Broker.AssetClass.FUT));

		Request reqMULTILEFOPT = emsxapi.emsxService.createRequest(GET_BROKERS.toString());
		reqMULTILEFOPT.set("EMSX_ASSET_CLASS", Broker.AssetClass.MULTILEG_OPT.toString());
		emsxapi.submitRequest(reqMULTILEFOPT,new BrokersHandler(this, Broker.AssetClass.MULTILEG_OPT));
	
	}

	class BrokersHandler implements MessageHandler {
		
		Brokers brokers;
		Broker.AssetClass assetClass;
		
		BrokersHandler(Brokers brokers, Broker.AssetClass assetClass) {
			this.brokers = brokers;
			this.assetClass = assetClass;
		}

		@Override
		public void processMessage(Message message) {
			
			Log.LogMessage(LogLevels.BASIC,"Brokers: processing message");
			
	    	if(message.messageType().equals(ERROR_INFO)) {
	        	Log.LogMessage(LogLevels.BASIC,"Brokers: processing RESPONSE error");
	    		Integer errorCode = message.getElementAsInt32("ERROR_CODE");
	    		String errorMessage = message.getElementAsString("ERROR_MESSAGE");
	    		Log.LogMessage(LogLevels.BASIC,"Error getting brokers: [" + errorCode + "] " + errorMessage);
	    	} else if(message.messageType().equals(GET_BROKERS)) {
	        	Log.LogMessage(LogLevels.BASIC,"Brokers: processing successful RESPONSE");
	    		
	        	Element brokerList = message.getElement("EMSX_BROKERS");
	    		
				int numValues = brokerList.numValues();

				for(int i = 0; i < numValues; i++) {
	    			
	    			String brokerName = brokerList.getValueAsString(i);
	    			Broker newBroker = new Broker(brokers,brokerName,assetClass);
	    			brokers.add(newBroker);
	            	Log.LogMessage(LogLevels.DETAILED,"Brokers: added new broker " + newBroker.name);
	    		}
	    	}
		}
	}

	@Override
	public Iterator<Broker> iterator() {
		return brokers.iterator();
	}

	public Broker get(int index) {
		return brokers.get(index);
	}
	
	public Broker get(String name, Broker.AssetClass assetClass) {
		for (Broker b : brokers){
			if(b.name.equalsIgnoreCase(name) && b.assetClass==assetClass)
			 return b;
		}
		return null;
	}
	
	public void add(Broker newBroker) {
		brokers.add(newBroker);
	}
}

