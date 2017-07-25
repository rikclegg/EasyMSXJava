package com.bloomberg.emsx.samples;

import com.bloomberglp.blpapi.Message;

interface MessageHandler {
	public void processMessage(Message message);
}
