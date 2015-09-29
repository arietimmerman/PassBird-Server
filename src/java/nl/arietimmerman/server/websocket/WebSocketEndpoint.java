package nl.arietimmerman.server.websocket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.arietimmerman.server.model.DataStore;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

@ServerEndpoint(value = "/websocket")
public class WebSocketEndpoint {

	private static final Logger logger = LogManager.getLogger(WebSocketEndpoint.class);
	private static final String configFileName = System.getenv("PASSBIRD_CONFIG_FILE");
	public static Set<Session> allSessions = new HashSet<Session>();
	
	//Google api key
	private static String API_KEY;
	
	public static String getGoogleAPIKey() throws IOException {
		if (API_KEY == null) {
			
			logger.info(String.format("Config file: %s",configFileName));
			
			InputStream inputStream = null;
			try {
				Properties properties = new Properties();
				
				inputStream = new FileInputStream(configFileName);
				
				properties.load(inputStream);
				
				API_KEY = properties.getProperty("google_api_key");
								
			} catch (Exception e) {
				logger.error("Exception: " + e);
			} finally {
				inputStream.close();
			}
		}

		return API_KEY;
	}

	private static void sendPush(String sessionId, String browserId, String registrationId, Boolean quiet) {
		try {

			HttpClient httpClient = HttpClientBuilder.create().build();

			JSONObject pushMessageJsonObject = new JSONObject();

			JSONObject pushMessageContentJsonObject = new JSONObject();

			// message contents
			pushMessageContentJsonObject.put("title", "Password Request");
			pushMessageContentJsonObject.put("quiet", quiet);
			pushMessageContentJsonObject.put("sessionId", sessionId);
			pushMessageContentJsonObject.put("browserId", browserId);

			JSONArray registrationIds = new JSONArray();
			registrationIds.put(registrationId);

			pushMessageJsonObject.put("registration_ids", registrationIds);
			pushMessageJsonObject.put("data", pushMessageContentJsonObject);

			HttpPost postRequest = new HttpPost("https://android.googleapis.com/gcm/send");

			postRequest.addHeader("Content-type", "application/json");
			postRequest.addHeader("Authorization", String.format("key=%s", getGoogleAPIKey()));

			postRequest.setEntity(new StringEntity(pushMessageJsonObject.toString()));

			HttpResponse response = httpClient.execute(postRequest);

			logger.info(response);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@OnOpen
	public void onOpen(Session session) {
		logger.info("session openend");
		
		allSessions.add(session);
	}
	
	@OnMessage
	public void receivedMessage(Session session, String msg, boolean last) {
		
		System.out.println(String.format("Received raw %s", msg));

		JSONObject jsonObject = new JSONObject(msg);

		if (session.isOpen() && jsonObject != null) {

			System.out.println(String.format("Received %s", jsonObject.toString()));

			session.getUserProperties().put("sessionId", jsonObject.getString("id"));
			session.getUserProperties().put("browser_id", jsonObject.getString("browser_id"));

			switch (jsonObject.getString("action")) {
			
			case "requestPassword":

				DataStore.getInstance().jsonPasswordRequests.put(jsonObject.getString("browser_id"), jsonObject);

				sendPush(jsonObject.getString("id"), jsonObject.getString("browser_id"), jsonObject.getString("registration_id"), jsonObject.getBoolean("quiet"));

				break;

			case "registerBrowser":

				System.out.println(String.format("registerBrowser: %s", jsonObject.getString("browser_id")));

				DataStore.getInstance().jsonRegisterRequests.put(jsonObject.getString("browser_id"), jsonObject);

				break;

			case "updatePassword":

				System.out.println(String.format("updatePassword: %s", jsonObject.getString("browser_id")));

				session.getUserProperties().put("message", jsonObject);

				synchronized (session) {
					session.notify();
				}

				break;

			case "send":

				System.out.println(String.format("respond: %s", jsonObject.getString("browser_id")));

				session.getUserProperties().put("message", jsonObject);

				synchronized (session) {
					session.notify();
				}

				break;

			default:
				break;

			}

		}

	}

	@OnClose
	public void onClose(Session session) {

		logger.error(String.format("session closed"));

		// DataStore.getInstance().jsonRegisterRequests.remove((String)
		// session.getUserProperties().get("browser_id"));
		// DataStore.getInstance().jsonPasswordRequests.remove((String)
		// session.getUserProperties().get("browser_id"));

		allSessions.remove(session);

		System.out.println("Session " + session.getId() + " has ended");
	}

}
