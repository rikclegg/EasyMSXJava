package com.bloomberg.emsx.samples;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberg.emsx.samples.Notification.NotificationCategory;
import com.bloomberg.emsx.samples.Notification.NotificationType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;

class RouteSubscriptionHandler implements MessageHandler {
	
	private static final Name	SUBSCRIPTION_STARTED	= new Name("SubscriptionStarted");

	Routes routes;
	
	RouteSubscriptionHandler(Routes routes) {
		this.routes = routes;
	}

	@Override
	public void processMessage(Message message) {

		Log.LogMessage(LogLevels.DETAILED, "RouteSubscriptionHandler: Processing message");
		
		Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());

		if(message.messageType().equals(SUBSCRIPTION_STARTED)) {
			Log.LogMessage(LogLevels.BASIC, "Route subscription started");
			return;
		}
			
		int eventStatus = message.getElementAsInt32("EVENT_STATUS");

		if(eventStatus==1) {
			Log.LogMessage(LogLevels.DETAILED, "RouteSubscriptionHandler: HEARTBEAT received");
		} else if(eventStatus==4) { //init_paint
			Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: INIT_PAINT message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
		
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			int routeID = message.getElementAsInt32("EMSX_ROUTE_ID");
			
			Route r = routes.getBySequenceNoAndID(sequence, routeID);
			
			if(r==null) r = routes.createRoute(sequence, routeID);

			r.fields.populateFields(message,false);
			r.notify(new Notification(NotificationCategory.ROUTE, NotificationType.INITIALPAINT, r, r.fields.getFieldChanges()));
			
		} else if(eventStatus==6) { //new
			Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: NEW_ORDER_ROUTE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());

			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			int routeID = message.getElementAsInt32("EMSX_ROUTE_ID");
			
			Route r = routes.getBySequenceNoAndID(sequence, routeID);
			
			if(r==null) r = routes.createRoute(sequence,routeID);

			r.fields.populateFields(message,false);
			r.notify(new Notification(NotificationCategory.ROUTE, NotificationType.NEW, r, r.fields.getFieldChanges()));
			
		} else if(eventStatus==7) { // update
			Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: UPD_ORDER_ROUTE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
			
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			int routeID = message.getElementAsInt32("EMSX_ROUTE_ID");

			Route r = routes.getBySequenceNoAndID(sequence, routeID);
			
			if(r==null) { 
				Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: WARNING > Update received for unkown route");
				r = routes.createRoute(sequence, routeID);
			}
			r.fields.populateFields(message,true);
			r.notify(new Notification(NotificationCategory.ROUTE, NotificationType.UPDATE, r, r.fields.getFieldChanges()));
		
		} else if(eventStatus==8) { // deleted/expired
			Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: DELETE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
			
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			int routeID = message.getElementAsInt32("EMSX_ROUTE_ID");

			Route r = routes.getBySequenceNoAndID(sequence, routeID);

			if(r==null) { // Order not found
				Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: WARNING > Delete received for unkown route");
				r = routes.createRoute(sequence, routeID);
			}
			r.fields.populateFields(message,false);
			r.fields.field("EMSX_STATUS").setCurrentValue("EXPIRED");
			r.notify(new Notification(NotificationCategory.ROUTE, NotificationType.DELETE, r, r.fields.getFieldChanges()));
		} else if(eventStatus==11) { // INIT_PAINT_END
			// End of inital paint messages
			Log.LogMessage(LogLevels.BASIC, "RouteSubscriptionHandler: End of Initial Paint");
			routes.emsxapi.routeBlotterInitialized = true;
		}
	}
}
