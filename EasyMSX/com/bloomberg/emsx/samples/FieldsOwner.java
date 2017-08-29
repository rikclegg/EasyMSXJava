package com.bloomberg.emsx.samples;

import java.util.ArrayList;

class FieldsOwner {
	
	public Fields fields;
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();
	void processNotification(Notification notification) {};

}
