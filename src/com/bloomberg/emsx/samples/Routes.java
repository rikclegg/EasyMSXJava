package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Subscription;

public class Routes implements Iterable<Route>, NotificationHandler {
	
	private ArrayList<Route> routes = new ArrayList<Route>();
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();

	private Subscription routeSubscription;
	
	EasyMSX emsxapi;
	
	Routes(EasyMSX emsxapi) {
		this.emsxapi = emsxapi;
	}
	
	void subscribe() {
		
		Log.LogMessage(LogLevels.BASIC, "Routes: Subscribing");
		
        String routeTopic = emsxapi.emsxServiceName + "/route";
        		
        if(emsxapi.team!=null) routeTopic = routeTopic + ";team=" + emsxapi.team.name;
        
        routeTopic = routeTopic + "?fields=";
        
        for (SchemaFieldDefinition f : emsxapi.routeFields) {
    		routeTopic = routeTopic + f.name + ","; 
        }
        
        routeTopic = routeTopic.substring(0,routeTopic.length()-1); // remove extra comma character

        Log.LogMessage(LogLevels.DETAILED, "Route Topic: " + routeTopic);

        emsxapi.subscribe(routeTopic, new RouteSubscriptionHandler(this));
    	Log.LogMessage(LogLevels.BASIC, "Entering Route subscription lock");
        while(!emsxapi.routeBlotterInitialized){
        	try{
        		Thread.sleep(1);
        	} catch (Exception ex) {
        	}
        }
    	Log.LogMessage(LogLevels.BASIC, "Route subscription lock released");
	}
	
	Route createRoute(int sequence, int routeID) {
		Route r = new Route(this);
		r.sequence = sequence;
		r.routeID = routeID;
		routes.add(r);
		return r;
	}

	@Override
	public Iterator<Route> iterator() {
		return routes.iterator();
	}
	
	public Route getByRefID(String refID) {
		for(Route r: routes) {
			if(r.field("EMSX_ROUTE_REF_ID").value()==refID) return r;
		}
		return null;
	}
	
	public Route getBySequenceNoAndID(int sequence, int routeID) {
		for(Route r: routes) {
			if((r.sequence == sequence) && (r.routeID == routeID)) return r;
		}
		return null;
	}

	public void addNotificationHandler(NotificationHandler notificationHandler) {
		notificationHandlers.add(notificationHandler);
	}
	
	@Override
	public void processNotification(Notification notification) {
		for(NotificationHandler nh: notificationHandlers) {
			if(!notification.consume) nh.processNotification(notification);
		}
		if(!notification.consume) emsxapi.processNotification(notification);
	}

	public int count() {
		return routes.size();
	}
}
