package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Arrays;

import com.bloomberg.emsx.samples.Notification.NotificationCategory;
import com.bloomberg.emsx.samples.Notification.NotificationType;

public class Field {
	
	private String name;
	private String old_value;		// Used to store the previous value when a current value is set from BLP event
	private String current_value;	// Used to store the value last provided by an event - matches BLP
	private Fields parent;
	
	ArrayList<NotificationHandler> notificationHandlers = new ArrayList<NotificationHandler>();

	Field(Fields parent) {
		this.parent = parent;
	}
	
	Field(Fields parent, String name, String value) {
		this.name = name;
		this.old_value = null;
		this.current_value = value;
		this.parent = parent;
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
			if(this.parent.owner instanceof Order) {
				this.notify(new Notification(NotificationCategory.ORDER, NotificationType.FIELD, this.parent.owner, new ArrayList<FieldChange>(Arrays.asList(this.getFieldChanged()))));
			} else if(this.parent.owner instanceof Route) {
				this.notify(new Notification(NotificationCategory.ROUTE, NotificationType.FIELD, this.parent.owner, new ArrayList<FieldChange>(Arrays.asList(this.getFieldChanged()))));
			}
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

