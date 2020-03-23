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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.math3.util.Pair;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
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
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Charsets;
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;

@Path("/draft")
@RequiresAuthentication
public class StudiesImportResource {

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
			
			HttpsURLConnection con = this.prepareRemoteConnection(url, username, password, type, WS_DRAFT_STUDY_STATES);
			
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
	public Response checkIfAlreadyExistsLocally(@QueryParam(IDS) List<String> ids, @QueryParam(TYPE) String type) {
		
		log.info("checkIfAlreadyExistsLocally ids: {}", ids);
		
		//List<String> existingIds = new ArrayList<>();
		
		Map<String, Boolean> existingIds = new HashMap<>();
		
		for (String id : ids) {

			try {
				
				BaseStudy study = studyService.findStudy(id);
				
				existingIds.put( study.getId(), !study.getResourcePath().equals(type) /*conflict condition*/ ); 
									
				//existingIds.add( study.getId());

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
				
				StringBuilder content = this.getRemoteContent(url, username, password, type, id);
				
				log.info("CONTENT: {}", content);
				
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

	
	private StringBuilder getRemoteContent(String url, String username, String password, String type, String id)
			throws IOException, URISyntaxException {
		
		HttpsURLConnection con = this.prepareRemoteConnection(url, username, password, null, 
				(type.equals(INDIVIDUAL_STUDY) ? WS_DRAFT_INDIVIDUAL_STUDY_ID : WS_DRAFT_HARMONIZATION_STUDY_ID).replace("{id}", id ));
		
		con.connect();

		int status = con.getResponseCode();
		
		log.info("RESPONSE CODE: {}", status);
		
		StringBuilder content = this.getContent(con);
		
		con.disconnect();
		
		return content;
	}
	
	private HttpsURLConnection prepareRemoteConnection(String url, String username, String password, String type, String endpoint)
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
