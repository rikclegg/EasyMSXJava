package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.EventListener;

public class Order extends FieldsOwner {
	
	Orders parent;
	int sequence;
	private Broker broker;
	public Object metaData;
	
	Order(Orders parent) {
		this.parent = parent;
		this.fields = new Fields(this);
		this.sequence = 0;
	}
	
	public Broker getBroker(){
		return this.broker;
	}
	
	public Field field(String fieldname) {
		return this.fields.field(fieldname);
	}

	public void addNotificationHandler(NotificationHandler notificationHandler) {
		notificationHandlers.add(notificationHandler);
	}

	@Override
	void processNotification(Notification notification) {
		for(NotificationHandler nh: notificationHandlers) {
			if(!notification.consume) nh.processNotification(notification);
		}
		if(!notification.consume) parent.processNotification(notification);
	}

}
