package nl.arietimmerman.server.model;

import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

import org.json.JSONObject;

public class DataStore {
	
	// request_id, request
	public ExpiringMap<String,JSONObject> jsonPasswordRequests = ExpiringMap.builder().expiration(120, TimeUnit.SECONDS).build();
	
	// browser_id, request
	public ExpiringMap<String,JSONObject> jsonRegisterRequests = ExpiringMap.builder().expiration(120, TimeUnit.SECONDS).build();
	
	private static DataStore dataStore;

	public static DataStore getInstance() {
		if(dataStore == null) dataStore = new DataStore();
		return dataStore;
	}

}
