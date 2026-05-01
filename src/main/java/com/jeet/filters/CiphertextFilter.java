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

        if (requestContext.getUriInfo().getPath().contains("purchases")
                || requestContext.getUriInfo().getPath().contains("payment")) {

            String headerToken = requestContext.getHeaderString("Ciphertext");

            if (!headerToken.trim().equals(requestContext.getHeaderString("token2").toString().trim())) {
                requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("Cannot access")
                                .build());
            }
        }
    }
}
