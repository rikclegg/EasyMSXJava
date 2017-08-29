package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;

public class Orders implements Iterable<Order>, NotificationHandler {
	
	private ArrayList<Order> orders = new ArrayList<Order>();
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();

	EasyMSX emsxapi;
	
	Orders(EasyMSX emsxapi) {
		this.emsxapi = emsxapi;
		subscribe();
	}
	
	private void subscribe() {
		
		Log.LogMessage(LogLevels.BASIC, "Orders: Subscribing");
		
        String orderTopic = emsxapi.emsxServiceName + "/order";
        		
        if(emsxapi.team!=null) orderTopic = orderTopic + ";team=" + emsxapi.team.name;
        
        orderTopic = orderTopic + "?fields=";
        
        for (SchemaFieldDefinition f : emsxapi.orderFields) {
        	if(f.name.equals("EMSX_ORDER_REF_ID")) { // Workaround for schema field naming
            	orderTopic = orderTopic + "EMSX_ORD_REF_ID" + ",";
        	} else {
        		orderTopic = orderTopic + f.name + ","; 
        	}
        	
        }
        
        orderTopic = orderTopic.substring(0,orderTopic.length()-1); // remove extra comma character

    	Log.LogMessage(LogLevels.DETAILED, "Order Topic: " + orderTopic);

    	emsxapi.subscribe(orderTopic, new OrderSubscriptionHandler(this));
    	Log.LogMessage(LogLevels.BASIC, "Entering Order subscription lock");
        while(!emsxapi.orderBlotterInitialized){
        	try{
        		Thread.sleep(1);
        	} catch (Exception ex) {
        	}
        }
    	Log.LogMessage(LogLevels.BASIC, "Order subscription lock released");
	}
	
	Order createOrder(int sequence) {
		Order o = new Order(this);
		o.sequence = sequence;
		orders.add(o);
		return o;
	}

	@Override
	public Iterator<Order> iterator() {
		return orders.iterator();
	}
	
	public Order getByRefID(String refID) {
		for(Order o: orders) {
			if(o.field("EMSX_ORDER_REF_ID").value()==refID) return o;
		}
		return null;
	}
	
	public Order getBySequenceNo(int sequence) {
		for(Order o: orders) {
			if(o.sequence == sequence) return o;
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
		return orders.size();
	}
}
