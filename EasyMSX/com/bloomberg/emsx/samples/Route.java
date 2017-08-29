package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.EventListener;

public class Route extends FieldsOwner {
	
	Routes parent;
	int sequence;
	int routeID;
	private Broker broker;
	public Object metaData;
	
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();

	Route(Routes parent) {
		this.parent = parent;
		this.fields = new Fields(this);
		this.sequence = 0;
		this.routeID = 0;
	}
	
	public Field field(String fieldname) {
		return this.fields.field(fieldname);
	}

	public void addNotificationHandler(NotificationHandler notificationHandler) {
		notificationHandlers.add(notificationHandler);
	}

	void notify(Notification notification) {
		
		for(NotificationHandler nh: notificationHandlers) {
			if(!notification.consume) nh.processNotification(notification);
		}
		if(!notification.consume) parent.processNotification(notification);
	
	}
	
	@Override
	void processNotification(Notification notification) {
		for(NotificationHandler nh: notificationHandlers) {
			if(!notification.consume) nh.processNotification(notification);
		}
		if(!notification.consume) parent.processNotification(notification);
	}

}
