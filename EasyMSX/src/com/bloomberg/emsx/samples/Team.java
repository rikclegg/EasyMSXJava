package com.bloomberg.emsx.samples;

import com.bloomberg.emsx.samples.Log.LogLevels;

public class Team {
	
	public String name;
	Teams parent;
	
	protected Team(Teams parent, String name) {
		this.name = name;
		this.parent = parent;
	}

	public void select() {
		parent.emsxapi.setTeam(this);
		Log.LogMessage(LogLevels.BASIC, "Team selected: " + name);
	}

}
