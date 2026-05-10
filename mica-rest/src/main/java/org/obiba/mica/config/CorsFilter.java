package org.obiba.mica.config;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CorsFilter implements ContainerResponseFilter {

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    // Allow specific origin or use "*" for all origins
    responseContext.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:9000");

    // Allow specific HTTP methods
    responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

    // Allow specific headers
    responseContext.getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
    responseContext.getHeaders().add("Access-Control-Expose-Headers", "Location, X-Mica-Version");

    // Allow credentials (if needed)
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
  }
}
