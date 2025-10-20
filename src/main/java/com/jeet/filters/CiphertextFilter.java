package com.jeet.filters;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import com.jeet.utils.AesUtil;
import com.jeet.utils.CustomNotFoundException;

@Provider
@PreMatching
public class CiphertextFilter implements ContainerRequestFilter {
 
    public CiphertextFilter() {
        super();
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    	System.out.println("Ciphertext filter...");
		System.out.println("Uri: " + requestContext.getUriInfo().getBaseUri().toString());
		System.out.println("Absolute Path: " + requestContext.getUriInfo().getAbsolutePath().toString());
		System.out.println("Path: " + requestContext.getUriInfo().getPath());

    	if(requestContext.getUriInfo().getPath().contains("purchases")
        || requestContext.getUriInfo().getPath().contains("payment")) {
    	System.out.println("Ciphertext filter running...");

    	String headerToken = requestContext.getHeaderString("Ciphertext");
    	System.out.println("headerToken: " + headerToken);
    	System.out.println("token2: " + requestContext.getHeaderString("token2").toString());

          if(!headerToken.trim().equals(requestContext.getHeaderString("token2").toString().trim())) {
          	requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                      .entity("Cannot access")
                      .build());
                }
    		}
    	}
    
}
