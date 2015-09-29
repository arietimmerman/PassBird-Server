package nl.arietimmerman.server.rest;

import java.io.IOException;

import javax.websocket.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.annotation.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.arietimmerman.server.model.DataStore;
//import nl.arietimmerman.server.rest.model.Request;
import nl.arietimmerman.server.websocket.WebSocketEndpoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("rest")
public class RestService {

	private static final Logger logger = LogManager.getLogger(RestService.class);
	
	// array with browser identifiers
	@POST
	@Path("/getRequest")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getRequest(String jsonStringArray) {
		
		logger.info(String.format("getRequest: %s",jsonStringArray.toString()));
		
		JSONObject result = null;
		
		JSONArray jsonArray = new JSONArray(jsonStringArray);
		
		for (int i = 0; i < jsonArray.length(); i++) {
			logger.info("Check " + jsonArray.getString(i));
			JSONObject request = DataStore.getInstance().jsonPasswordRequests.get(jsonArray.getString(i));
			DataStore.getInstance().jsonPasswordRequests.remove(jsonArray.getString(i));
			
			if(request != null){
				logger.info("Found: " + request.toString());
				result = request;
				break;
			}else{
				logger.info("null found");
			}
			
			
		}
		
		return (result!=null)?result.toString():new JSONObject().toString();
	}
	
	
	@POST
	@Path("/send")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String send(String jsonObjectString) {
		
		JSONObject response = new JSONObject();
		response.put("success", true);
		
		logger.info(String.format("send: %s",jsonObjectString.toString()));
		
		JSONObject jsonObject = new JSONObject(jsonObjectString);
		
		
		//FIXME: for even more security, do something with the inResponseto attribute
		Object inResponseTo = jsonObject.get("inResponseto");
		
		if(inResponseTo instanceof JSONArray){
			
		}else if(inResponseTo instanceof String){
			
		}
		
		Boolean waitForResponse = jsonObject.has("waitForResponse")&&jsonObject.getBoolean("waitForResponse");
		Boolean closeSession = jsonObject.has("closeSession")&&jsonObject.getBoolean("closeSession");
		
		for(Session session : WebSocketEndpoint.allSessions){
			if(jsonObject.getString("inResponseto").equals(session.getUserProperties().get("sessionId"))){
				try {
					logger.info("send message to browser");
					session.getBasicRemote().sendText(jsonObject.toString());
					
					if(waitForResponse){
						logger.info("wait for response");
						
						synchronized (session) {
							session.wait(2000);
							
							response = ((JSONObject)session.getUserProperties().get("message"));
							logger.info(String.format("Received %s", response.toString()));
							
						}
					}
					
					if(closeSession){
						session.close();
					}
					
				} catch (JSONException | IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			}
		}
		
		return response.toString();
		
	}
	
	@POST
	@Path("/browserScanned")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String browserScanned(String jsonObjectString) {
		
		logger.error("browserScanned");
		
		logger.info(String.format("browserScanned: %s",jsonObjectString.toString()));
		
		JSONObject requestJsonObject = new JSONObject(jsonObjectString);
		
		logger.info("Zoeken naar " + requestJsonObject.getString("browser_id"));
		
		JSONObject resultJsonObject = DataStore.getInstance().jsonRegisterRequests.get(requestJsonObject.getString("browser_id"));
		DataStore.getInstance().jsonRegisterRequests.remove(resultJsonObject);
		
		logger.info(String.format("Return: %s",resultJsonObject.toString()));
		
		return resultJsonObject.toString();
	}
	
	@POST
	@Path("/browserRegistered")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String browserRegistered(String jsonObjectString) {
		
		logger.info(String.format("browserRegistered: %s",jsonObjectString.toString()));
		
		JSONObject requestJsonObject = new JSONObject(jsonObjectString);
		
		for(Session session : WebSocketEndpoint.allSessions){
			if(requestJsonObject.getString("browser_id").equals(session.getUserProperties().get("browser_id"))){
				try {
					session.getBasicRemote().sendText(requestJsonObject.toString());
					session.close();
				} catch (JSONException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;
			}
		}
		
		JSONObject resultJsonObject = new JSONObject();
		resultJsonObject.put("succes", true);
		
		return resultJsonObject.toString();
		
	}
	
	
	

}
