package com.bloomberg.emsx.samples;

public class SchemaFieldDefinition {

	public String name = "";
	public String status = "";
	public String type = "";
	public int min = 0;
	public int max = 0;
	public String description = "";
	
	public SchemaFieldDefinition(String name) {
		this.name = name;
	}

	public SchemaFieldDefinition(String name, String status, String type, int min, int max, String description) {
		this.name = name;
		this.status = status;
		this.type = type;
		this.min = min;
		this.max = max;
		this.description = description;
	}
	
	public boolean isStatic() {
		
		if(description.indexOf("Static") > -1) {
			return true;
		} else return false;
	}
	
	public boolean isOrderField() {
		if((description.indexOf("Order") > -1) || (description.indexOf("O,R") > -1)) {
			return true;
		} else return false;
	}
	
	public boolean isRouteField() {
		if((description.indexOf("Route") > -1) || (description.indexOf("O,R") > -1)) {
			return true;
		} else return false;
	}
	
	public boolean isSpecialField() {
		if(description.indexOf("Special") > -1)  {
			return true;
		} else return false;
	}
	

}
