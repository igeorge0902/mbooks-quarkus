package com.jeet.utils;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
	public class EntityNotFoundMapper implements ExceptionMapper<CustomExceptions> {

	@Override
	public Response toResponse(CustomExceptions exception) {
		return Response.status(404).
				entity(exception.getMessage() + "hello").
				type("text/plain").
				build();
	}
}
	

