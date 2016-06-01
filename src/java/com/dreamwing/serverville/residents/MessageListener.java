package com.dreamwing.serverville.residents;

public interface MessageListener
{
	String getId();
	
	void onListeningTo(BaseResident resident);
	void onStoppedListeningTo(BaseResident resident);
	void onResidentJoined(Resident resident, Channel viaChannel);
	void onResidentLeft(Resident resident, Channel viaChannel);
	void onStateChange(String messageBody, long when, String fromId, Channel viaChannel);
	void onMessage(String messageType, String messageBody, String fromId, Channel viaChannel);
	
	void destroy();
}
