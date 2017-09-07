package com.bloomberg.emsx.samples;

import java.util.ArrayList;

public class Notification {
	
	public enum NotificationCategory {
		ORDER,
		ROUTE,
		ADMIN
	}
	
	public enum NotificationType { //expired
		NEW,
		INITIALPAINT,
		UPDATE,
		DELETE,
		CANCEL,
		FIELD,
		ERROR
	}

	public NotificationCategory category;
	public NotificationType type;
	public boolean consume = false; 
	
	private Order order;
	private Route route;
	
	private ArrayList<FieldChange>fieldChanges;
	private int errorCode;
	private String errorMessage;
	
	Notification (NotificationCategory category, NotificationType type, Object source, ArrayList<FieldChange> fieldChanges) {
		this.category = category;
		this.type = type;
		if(category==NotificationCategory.ORDER) {
			order = (Order)source;
		} else if(category==NotificationCategory.ROUTE) {
			route = (Route)source;
		}
		this.fieldChanges = fieldChanges;
	}
	
	Notification (NotificationCategory category, NotificationType type, Object source, int errorCode, String errorMessage) {
		this.category = category;
		this.type = type;
		if(category==NotificationCategory.ORDER) {
			order = (Order)source;
		} else if(category==NotificationCategory.ROUTE) {
			route = (Route)source;
		}
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public Order getOrder() {
		return this.order;
	}
	
	public Route getRoute() {
		return this.route;
	}
	
	public ArrayList<FieldChange> getFieldChanges() {
		return this.fieldChanges;
	}
	
	public int errorCode() {
		return this.errorCode;
	}
	
	public String errorMessage() {
		return this.errorMessage;
	}
}
