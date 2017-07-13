package com.bloomberg.emsx.samples;

import java.util.ArrayList;
import java.util.Iterator;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;

public class Teams implements Iterable<Team>{

    private static final Name 	GET_TEAMS = new Name("GetTeams");
	private static final Name 	ERROR_INFO = new Name("ErrorInfo");

	private ArrayList<Team> teams = new ArrayList<Team>(); 
    EasyMSX emsxapi;
    
	Teams(EasyMSX emsxapi) {
		this.emsxapi = emsxapi;
		loadTeams();
	}
	
	private void loadTeams() {
		Log.LogMessage(LogLevels.BASIC, "Teams: Loading");
		Request request = emsxapi.emsxService.createRequest(GET_TEAMS.toString());
		emsxapi.submitRequest(request,new TeamHandler(this));
	}

	class TeamHandler implements MessageHandler {
		
		Teams teams;
		
		TeamHandler(Teams teams) {
			this.teams = teams;
		}
		
		@Override
		public void processMessage(Message message) {
			
			Log.LogMessage(LogLevels.BASIC, "Teams: processing message");
			
	    	if(message.messageType().equals(ERROR_INFO)) {
	        	Log.LogMessage(LogLevels.BASIC, "Teams: processing RESPONSE error");
	    		Integer errorCode = message.getElementAsInt32("ERROR_CODE");
	    		String errorMessage = message.getElementAsString("ERROR_MESSAGE");
	    		Log.LogMessage(LogLevels.BASIC, "Error getting teams: [" + errorCode + "] " + errorMessage);
	    	} else if(message.messageType().equals(GET_TEAMS)) {
	        	Log.LogMessage(LogLevels.BASIC, "Teams: processing successful RESPONSE");
	
	        	Element teamList = message.getElement("TEAMS");
	    		
				int numValues = teamList.numValues();
	    		
	    		for(int i = 0; i < numValues; i++) {
	    			String teamName = teamList.getValueAsString(i);
	    			Team newTeam = new Team(teams,teamName);
	    			teams.add(newTeam);
	            	Log.LogMessage(LogLevels.DETAILED, "Teams: Added new team " + newTeam.name);
	    		}
	    	}
		}
	}

	@Override
	public Iterator<Team> iterator() {
		return teams.iterator();
	}
	
	public Team get(int index) {
		return teams.get(index);
	}
	
	public Team get(String name) {
		for(Team t: teams) {
			if(t.name.equals(name)) return t;
		}
		return null;
	}

	private void add(Team newTeam) {
		teams.add(newTeam);
	}
}
