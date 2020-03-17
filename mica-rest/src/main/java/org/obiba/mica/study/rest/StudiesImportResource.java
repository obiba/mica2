/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Path("/draft")
@RequiresAuthentication
public class StudiesImportResource {

	private static final String BASIC_AUTHENTICATION = "Basic ";
	private static final String USERNAME_PARAM = "username";
	private static final String PWORD_PARAM = "password";
	private static final String TYPE = "type";
	private static final String IDS = "ids";
	
	private static final Logger log = LoggerFactory.getLogger(StudiesImportResource.class);
	
	@Inject
	private StudyService studyService;

	@GET
	@Path("/studies/import/_preview")
	@RequiresPermissions("/draft/individual-study:ADD")
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	public Response listRemoteSourceIndividualStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type) {
		
		try {
			
			HttpsURLConnection con = this.prepareConnection(url, username, password, type, "/ws/draft/study-states");
			
			con.connect();

			int status = con.getResponseCode();
			
			StringBuilder content = this.getContent(con);
			
			con.disconnect();
			
			return Response.ok(content).status(status).build();
			
		} catch (URISyntaxException|ProtocolException e) {
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}
	
	@GET
	@Path("/studies/import/_summary")
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	public Response checkIfAlreadyExistsLocally(@QueryParam(IDS) List<String> ids) {
		
		log.info("checkIfAlreadyExistsLocally ids: {}", ids);
		
		List<String> existingIds = new ArrayList<>();
		
		existingIds.add("action");
		
		for (String id : ids) {

			try {
				
				BaseStudy baseStudy = studyService.findStudy(id);
				
				existingIds.add( baseStudy.getId());
				
			} catch(NoSuchEntityException ex) {
				//ignore if study doesn't exist locally.
				log.info("checkIfAlreadyExistsLocally - id not exists: {}", id);
			}
		}
		
		return Response.ok(existingIds).build();		
	}
	
		
	@POST
	@Path("/individual-studies/_import")
	@RequiresPermissions("/draft/individual-study:ADD")
	public Response importIndividualStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(IDS) List<String> ids) {
	
		log.info("POST importIndividualStudies called: ");
		log.info("importIndividualStudies ids: {} ", ids);
		
		try {
			
			for (String id : ids) {
			
				HttpsURLConnection con = this.prepareConnection(url, username, password, null, "/ws/draft/individual-study/{id}".replace("{id}", id ));
				
				con.connect();

				int status = con.getResponseCode();
				
				log.info("importIndividualStudies RESPONSE CODE: {}", status);
				
				Object result = con.getContent();
				
				log.info( "{}", result );
				
				StringBuilder content = this.getContent(con);
				
				log.info("importIndividualStudies CONTENT: {}", content);
				
				con.disconnect();				
			}
			
			return Response.ok().build();
						
		} catch (URISyntaxException|ProtocolException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}
	
	@POST
	@Path("/harmonization-studies/_import")
	@RequiresPermissions("/draft/harmonization-study:ADD")
	public Response importHarmonizationStudies(@Context HttpServletRequest request) {
		
		log.info("POST importHarmonizationStudies called: {} ",  request);

	    return Response.ok().build();	
	}
	
	
	private HttpsURLConnection prepareConnection(String url, String username, String password, String type, String endpoint)
			throws IOException, URISyntaxException {	
		
		URI preparedURI = new URI((url.endsWith("/")) ? url.substring(0, url.length() - 1) : url);
		
		URIBuilder builder = new URIBuilder();
		builder.setScheme(preparedURI.getScheme())
			.setHost(preparedURI.getHost())
			.setPath(preparedURI.getPath() + endpoint);
		
		if (type != null) {
			builder.setParameter(TYPE, type);
		}
		    
		URI uri = builder.build();
		
		HttpsURLConnection con = (HttpsURLConnection) (uri.toURL()).openConnection();
		con.setReadTimeout(5000);
		con.setConnectTimeout(5000);
		con.setRequestMethod(HttpMethod.GET.toString());
		con.setDoInput(true);
		con.setDoOutput(true);
		
		String originalInput = username + ":" + password;
		String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());
		
		con.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		con.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTHENTICATION + encodedString );
		con.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE );
		
		return con;
	}
	
	
	private StringBuilder getContent(HttpsURLConnection con) throws IOException {
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder content = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		
		in.close();
		
		return content;
	}
}
