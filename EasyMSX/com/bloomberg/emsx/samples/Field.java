package com.bloomberg.emsx.samples;

import java.util.ArrayList;

public class Field {
	
	private String name;
	private String old_value;		// Used to store the previous value when a current value is set from BLP event
	private String current_value;	// Used to store the value last provided by an event - matches BLP
	
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();

	Field(Fields parent) {
	}
	
	Field(Fields parent, String name, String value) {
		this.name = name;
		this.old_value = null;
		this.current_value = null;
	}

	public String name() {
		return name;
	}
	
	public String value() {
		return this.current_value;
	}
	
	void setName(String name) {
		this.name = name;
	}
	
	void setCurrentValue(String value) {
		
		if(value!=this.current_value) {

			this.old_value = this.current_value;
			this.current_value = value;
			
		}
	}
	
	void CurrentToOld() {
		this.old_value = this.current_value;
	}

	FieldChange getFieldChanged() {
		
		FieldChange fc=null;
		
		if(!this.current_value.equals(this.old_value)) {
			fc = new FieldChange();
			fc.field = this;
			fc.oldValue = this.old_value;
			fc.newValue = this.current_value;
		}
		return fc;
	}
	
	public void addNotificationHandler(NotificationHandler notificationHandler) {
		notificationHandlers.add(notificationHandler);
	}
	
	void notify(Notification notification) {
		
		for(NotificationHandler nh: notificationHandlers) {
			if(!notification.consume) nh.processNotification(notification);
		}
	}

}

