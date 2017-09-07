package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Message;

public class Fields implements Iterable<Field> {
	
	private ArrayList<Field> fields = new ArrayList<Field>();
	
	FieldsOwner owner;
	
	private ArrayList<FieldChange> fieldChanges;
	
	Fields(FieldsOwner owner) {
		this.owner = owner;
		loadFields(owner);
	}
	
	void loadFields(FieldsOwner owner) {
		
		if(owner instanceof Order) {
			Order o = (Order)owner;
			for(SchemaFieldDefinition sdf: o.parent.emsxapi.orderFields) {
				Field f = new Field(this,sdf.name,"");
				fields.add(f);
			}
		} else if(owner instanceof Route) {
			Route r = (Route)owner;
			for(SchemaFieldDefinition sdf: r.parent.emsxapi.routeFields) {
				Field f = new Field(this,sdf.name,"");
				fields.add(f);
			}
		} 
	}
	
	void populateFields(Message message, boolean dynamicFieldsOnly) {

		Log.LogMessage(LogLevels.BASIC, "Populate fields");
		
		CurrentToOldValues();
		
		int fieldCount = message.numElements();
		
		Element e = message.asElement();
		
		fieldChanges = new ArrayList<FieldChange>();
		
		for(int i=0; i<fieldCount; i++) {

			Boolean load=true;
			
			Element f = e.getElement(i);
			
			String fieldName = f.name().toString();
			// Workaround for schema field nameing
			if(fieldName.equals("EMSX_ORD_REF_ID")) fieldName = "EMSX_ORDER_REF_ID";
			
			if(dynamicFieldsOnly) {
				SchemaFieldDefinition sfd = null;
				if(fieldName.equals("EMSX_ASSET_CLASS")) {
					Log.LogMessage(LogLevels.BASIC,"Here!");
				}
				if(owner instanceof Order) {
					Order o = (Order)owner;
					sfd = findSchemaFieldByName(fieldName,o.parent.emsxapi.orderFields);
				} else if(owner instanceof Route) {
					Route r = (Route)owner;
					sfd = findSchemaFieldByName(fieldName,r.parent.emsxapi.routeFields);
				}
				if(sfd!=null && sfd.isStatic()) {
					load=false;
				}
			}
			
			if(load) {
				Field fd = field(fieldName);
				
				if(fd==null) fd = new Field(this);
				
				fd.setName(fieldName);
				// set the CURRENT value NOT the new_value. new_value is only set by client side.
				fd.setCurrentValue(f.getValueAsString()); 
			
				FieldChange fc = fd.getFieldChanged();
				if(fc!=null) {
					fieldChanges.add(fc);
				}
			}
		}

	}
	
	private SchemaFieldDefinition findSchemaFieldByName(String name, ArrayList<SchemaFieldDefinition>fields) {

		for(SchemaFieldDefinition sfd: fields) {
			if(sfd.name.equals(name)) {
				return sfd;
			}
		}
		return null;
	}
	
	ArrayList<FieldChange> getFieldChanges() {
		return this.fieldChanges;
	}
	
	void CurrentToOldValues() {
	
		for(Field f: fields) {
			f.CurrentToOld();
		}
	}
	
	public Field field(String name) {
		for(Field f: fields) {
			if(f.name().equals(name)) {
				return f;
			}
		}
		return null;
	}

	@Override
	public Iterator<Field> iterator() {
		return fields.iterator();
	}
	

}
