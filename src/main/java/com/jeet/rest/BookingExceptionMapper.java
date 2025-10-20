package com.jeet.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
 
import com.jeet.api.BookingException;
 
@Provider
public class BookingExceptionMapper implements ExceptionMapper<Exception> {
 
    public Response toResponse(Exception arg0) {
       
    	if (arg0 instanceof BookingException)
        
    		return Response.status(404).build();
        else
            return Response.status(100).build();
    }
 
}
