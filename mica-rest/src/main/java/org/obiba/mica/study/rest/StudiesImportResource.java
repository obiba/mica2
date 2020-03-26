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
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;

@Path("/draft")
@RequiresAuthentication
public class StudiesImportResource {

	private static final String WS_CONFIG_HARMONIZATION_POPULATION_FORM_CUSTOM = "/ws/config/harmonization-population/form-custom";
	private static final String WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM = "/ws/config/harmonization-study/form-custom";
	private static final String WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM = "/ws/config/data-collection-event/form-custom";
	private static final String WS_CONFIG_POPULATION_FORM_CUSTOM = "/ws/config/population/form-custom";
	private static final String WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM = "/ws/config/individual-study/form-custom";
	
	private static final String WS_DRAFT_STUDY_STATES = "/ws/draft/study-states";
	private static final String WS_DRAFT_HARMONIZATION_STUDY_ID = "/ws/draft/harmonization-study/{id}";
	private static final String WS_DRAFT_INDIVIDUAL_STUDY_ID = "/ws/draft/individual-study/{id}";

	private static final String IDS_TO_UPDATE = "idsToUpdate";
	private static final String IDS_TO_INCLUDE = "idsToInclude";
	
	private static final String BASIC_AUTHENTICATION = "Basic ";
	private static final String USERNAME_PARAM = "username";
	private static final String PWORD_PARAM = "password";
	private static final String TYPE = "type";
	private static final String IDS = "ids";
	private static final String HARMONIZATION_STUDY = "harmonization-study";
	private static final String INDIVIDUAL_STUDY = "individual-study";
	
	private static final Logger log = LoggerFactory.getLogger(StudiesImportResource.class);
	
	@Inject
	private IndividualStudyService individualStudyService;

	@Inject
	private HarmonizationStudyService harmonizationStudyService;
	
	@Inject
	private StudyService studyService;
	
	@Inject
	private Dtos dtos;

	@GET
	@Path("/studies/import/_preview")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	public Response listRemoteSourceStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type) {
		
		try {
			
			Boolean hasSameSchemasAndDefintions = this.checkSchemasAndDefinitions(url, username, password, type);
			
			if (hasSameSchemasAndDefintions) {
				
				List<NameValuePair> params = new ArrayList<>();
				params.add(new BasicNameValuePair(TYPE, type));
				
				HttpURLConnection con = this.prepareRemoteConnection(url, username, password, params, WS_DRAFT_STUDY_STATES);

				int status = con.getResponseCode();
				
				
				return Response.ok( this.getRawContent(con) ).status(status).build();
				
			} else {
				
				return Response.ok("Different schemas and Definitions").build();
			}

			
		} catch (URISyntaxException|ProtocolException e) {
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}
	
	private Boolean checkSchemasAndDefinitions(String url, String username, String password, String type) throws IOException, URISyntaxException {
		
		Boolean areEquals = Boolean.FALSE;
		
		if (type.equals(INDIVIDUAL_STUDY)) {
			
			Map<String, Object> content = this.getJSONContent(url, username, password, null, WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM);
			
			//TODO
			
			content = this.getJSONContent(url, username, password, null, WS_CONFIG_POPULATION_FORM_CUSTOM);
			
			content = this.getJSONContent(url, username, password, null, WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM);
			
		} else if ( type.equals(HARMONIZATION_STUDY) ) {
			
			Map<String, Object> content = this.getJSONContent(url, username, password, null, WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM);
			
			content = this.getJSONContent(url, username, password, null, WS_CONFIG_HARMONIZATION_POPULATION_FORM_CUSTOM);
		}
		
		
		return areEquals;
	}

	@GET
	@Path("/studies/import/_summary")
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	public Response checkIfAlreadyExistsLocally(@QueryParam(IDS) List<String> ids, @QueryParam(TYPE) String type) {
		
		log.info("GET checkIfAlreadyExistsLocally ids: {}", ids);
		
		Map<String, Boolean> existingIds = new HashMap<>();
		
		for (String id : ids) {

			try {
				
				BaseStudy study = studyService.findStudy(id);
				
				existingIds.put( study.getId(), !study.getResourcePath().equals(type) /*conflict condition*/ );

			} catch(NoSuchEntityException ex) {
				//ignore if study doesn't exist locally.
				log.info("checkIfAlreadyExistsLocally - id not exists: {}", id);
			}
		}
		
		return Response.ok(existingIds).build();		
	}
	
		
	@POST
	@Path("/studies/import/_include")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	public Response includeStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type,
			@QueryParam(IDS_TO_INCLUDE) List<String> ids) {
	
		log.info("POST includeStudies. ids = {}", ids);
		
		return this.saveStudiesLocally(url, username, password, type, ids, Boolean.TRUE);
	}

	
	
	@PUT
	@Path("/studies/import/_update")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	public Response updateStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type,
			@QueryParam(IDS_TO_UPDATE) List<String> ids) {
	
		log.info("PUT updateStudies. ids = {}", ids);
		
		return this.saveStudiesLocally(url, username, password, type, ids, Boolean.FALSE);
	}

	
	private Response saveStudiesLocally(String url, String username, String password, String type, List<String> ids, Boolean isToInclude) {
		
		try {
				
			for (String id : ids) {
				
				String content = this.getRawContent(url, username, password, null,
						(type.equals(INDIVIDUAL_STUDY) ? WS_DRAFT_INDIVIDUAL_STUDY_ID : WS_DRAFT_HARMONIZATION_STUDY_ID).replace("{id}", id ));
				
				Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
				 
				ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
				extensionRegistry.add(Mica.CollectionStudyDto.type);
				extensionRegistry.add(Mica.HarmonizationStudyDto.type);
				
				JsonFormat.merge(content, extensionRegistry, builder);
				
				if ( type.equals(INDIVIDUAL_STUDY) ) {
					
					Study study = (Study)dtos.fromDto( builder);
					
					if ( Boolean.TRUE.equals(isToInclude) ) {
						
						study.setId(null);
					}

					individualStudyService.save(study);
					
					log.info("individualStudyService: {}", study);
					
				} else if ( type.equals(HARMONIZATION_STUDY) ) {
					
					HarmonizationStudy study = (HarmonizationStudy)dtos.fromDto( builder);
					
					if ( Boolean.TRUE.equals(isToInclude) ) {
						
						study.setId(null);
					}
					
					harmonizationStudyService.save(study);
					
					log.info("harmonizationStudyService: {}", study);
				}				
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

	
	Map<String, Object> getJSONContent(String url, String username, String password, List<NameValuePair> param, String endpoint)
			throws IOException, URISyntaxException {
		
		return this.getJSONContent( this.prepareRemoteConnection(url, username, password, param, endpoint) );
	}
	
	String getRawContent(String url, String username, String password, List<NameValuePair> param, String endpoint)
			throws IOException, URISyntaxException {
		
		return this.getRawContent( this.prepareRemoteConnection(url, username, password, param, endpoint) );
	}
	
	private Map<String, Object> getJSONContent(HttpURLConnection con) throws IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> jsonMap = mapper.readValue(con.getInputStream(), Map.class);
		
		return jsonMap;
	}
	
	private String getRawContent(HttpURLConnection con) throws IOException {
	
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder content = new StringBuilder();
		
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		
		in.close();
		
		return content.toString();
	}
	
	private HttpURLConnection prepareRemoteConnection(String url, String username, String password, List<NameValuePair> param, String endpoint)
			throws IOException, URISyntaxException {	
		
		
		URI preparedURI = new URI((url.endsWith("/")) ? url.substring(0, url.length() - 1) : url);
		
		URIBuilder builder = new URIBuilder();
		builder.setScheme(preparedURI.getScheme())
			.setHost(preparedURI.getHost())
			.setPath(preparedURI.getPath() + endpoint);
		
		if (param != null) {
			builder.setParameters(param);
		}
		    
		URI uri = builder.build();
		
		URLConnection urlCon = uri.toURL().openConnection();
		
		HttpURLConnection con = (HttpURLConnection)urlCon;
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
}
