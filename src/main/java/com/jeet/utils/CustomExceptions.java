package com.jeet.utils;

import java.io.Serializable;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.json.JSONObject;

public class CustomExceptions extends WebApplicationException implements Serializable {
	 
	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	  * Create a HTTP 404 (Not Found) exception.
	  */
	  public CustomExceptions() {
	    super(Response.noContent().build());
	  }
	 
	  /**
	  * Creates a custom json response message.
	  * 
	  * @param message the String that is the entity of the response.
	  */
	  public CustomExceptions(String title, String message) {
	    super(Response.status(Status.OK).
	    entity(jsonMessage(title, message)).type(MediaType.APPLICATION_JSON).build());
	  }
	  
	  private static JSONObject jsonMessage(String title, String message) {
	  
		JSONObject json = new JSONObject(); 
		
		json.put("Title", title); 
		json.put("Message", message); 
		
		return json;
	  }
	}