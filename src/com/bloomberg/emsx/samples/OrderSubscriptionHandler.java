package com.bloomberg.emsx.samples;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberg.emsx.samples.Notification.NotificationCategory;
import com.bloomberg.emsx.samples.Notification.NotificationType;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;

class OrderSubscriptionHandler implements MessageHandler {
	
	private static final Name	SUBSCRIPTION_FAILURE 	= new Name("SubscriptionFailure");
	private static final Name	SUBSCRIPTION_STARTED	= new Name("SubscriptionStarted");
	private static final Name	SUBSCRIPTION_TERMINATED	= new Name("SubscriptionTerminated");

	Orders orders;
	
	OrderSubscriptionHandler(Orders orders) {
		this.orders = orders;
	}

	@Override
	public void processMessage(Message message) {

		Log.LogMessage(LogLevels.DETAILED, "OrderSubscriptionHandler: Processing message");
		
		Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());

		if(message.messageType().equals(SUBSCRIPTION_STARTED)) {
			Log.LogMessage(LogLevels.BASIC, "Order subscription started");
			return;
		} 
			
		int eventStatus = message.getElementAsInt32("EVENT_STATUS");

		if(eventStatus==1) {
			Log.LogMessage(LogLevels.DETAILED, "OrderSubscriptionHandler: HEARTBEAT received");
		} else if(eventStatus==4) { //init_paint
			Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: INIT_PAINT message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
			Order o;
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			o = orders.getBySequenceNo(sequence);
			if(o==null) { // Order not found
				o = orders.createOrder(sequence);
			}
			o.fields.populateFields(message,false);
			o.notify(new Notification(NotificationCategory.ORDER, NotificationType.INITIALPAINT, o, o.fields.getFieldChanges()));
			
		} else if(eventStatus==6) { //new
			Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: NEW_ORDER_ROUTE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());

			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			
			Order o = orders.getBySequenceNo(sequence);
			if(o==null) o = orders.createOrder(sequence);

			o.fields.populateFields(message,false);
			o.notify(new Notification(NotificationCategory.ORDER, NotificationType.NEW, o, o.fields.getFieldChanges()));
			
		} else if(eventStatus==7) { // update
			Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: UPD_ORDER_ROUTE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
			
			// Order should already exists. If it doesn't create it anyway.
			Order o;
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			o = orders.getBySequenceNo(sequence);
			if(o==null) { // Order not found
				Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: WARNING > Update received for unkown order");
				o = orders.createOrder(sequence);
			}
			o.fields.populateFields(message,true);
			o.notify(new Notification(NotificationCategory.ORDER, NotificationType.UPDATE, o, o.fields.getFieldChanges()));
		} else if(eventStatus==8) { // deleted/expired
			Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: DELETE message received");
			Log.LogMessage(LogLevels.DETAILED, "Message: " + message.toString());
			
			// Order should already exists. If it doesn't create it anyway.
			Order o;
			int sequence = message.getElementAsInt32("EMSX_SEQUENCE");
			o = orders.getBySequenceNo(sequence);
			if(o==null) { // Order not found
				Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: WARNING > Delete received for unkown order");
				o = orders.createOrder(sequence);
			}
			o.fields.populateFields(message,false);
			o.fields.field("EMSX_STATUS").setCurrentValue("EXPIRED");
			o.notify(new Notification(NotificationCategory.ORDER, NotificationType.DELETE, o, o.fields.getFieldChanges()));
		} else if(eventStatus==11) { // INIT_PAINT_END
			// End of inital paint messages
			Log.LogMessage(LogLevels.BASIC, "OrderSubscriptionHandler: End of Initial Paint");
			orders.emsxapi.orderBlotterInitialized = true;
		}
	}
}
