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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
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
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.DataCollectionEventConfigService;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.micaConfig.service.HarmonizationPopulationConfigService;
import org.obiba.mica.micaConfig.service.HarmonizationStudyConfigService;
import org.obiba.mica.micaConfig.service.IndividualStudyConfigService;
import org.obiba.mica.micaConfig.service.PopulationConfigService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Population;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;

@Path("/draft")
@RequiresAuthentication
public class StudiesImportResource {
	
	private static final String LIST_DIFFS_FORM = "listDiffsForm";
	private static final String WS_CONFIG_HARMONIZATION_POPULATION_FORM_CUSTOM = "/ws/config/harmonization-population/form-custom";
	private static final String WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM = "/ws/config/harmonization-study/form-custom";
	private static final String WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM = "/ws/config/data-collection-event/form-custom";
	private static final String WS_CONFIG_POPULATION_FORM_CUSTOM = "/ws/config/population/form-custom";
	private static final String WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM = "/ws/config/individual-study/form-custom";
	
	private static final String WS_DRAFT_STUDY_STATES = "/ws/draft/study-states";
	private static final String WS_DRAFT_HARMONIZATION_STUDY_ID = "/ws/draft/harmonization-study/{id}";
	private static final String WS_DRAFT_INDIVIDUAL_STUDY_ID = "/ws/draft/individual-study/{id}";

	private static final String SCHEMA = "schema";
	private static final String DEFINITION = "definition";
	
	private static final String BASIC_AUTHENTICATION = "Basic ";
	private static final String USERNAME_PARAM = "username";
	private static final String PWORD_PARAM = "password";
	private static final String TYPE = "type";
	private static final String IDS = "ids";
	private static final String HARMONIZATION_STUDY = "harmonization-study";
	private static final String INDIVIDUAL_STUDY = "individual-study";
	
	private static final String INDIVIDUAL_STUDY_FORM_SECTION = INDIVIDUAL_STUDY;
	private static final String POPULATION_FORM_SECTION = "study-population";
	private static final String DATA_COLLECTION_EVENT_FORM_SECTION = "data-collection-event";
	
	private static final String HARMONIZATION_STUDY_FORM_SECTION = HARMONIZATION_STUDY;
	private static final String HARMONIZATION_POPULATION_FORM_SECTION = "harmonization-study-population";

	private static final Logger log = LoggerFactory.getLogger(StudiesImportResource.class);
	
	@Inject
	private IndividualStudyService individualStudyService;

	@Inject
	private HarmonizationStudyService harmonizationStudyService;
	
	@Inject
	private StudyService studyService;
	
	@Inject
	private IndividualStudyConfigService individualStudyConfigService;
	
	@Inject
	private PopulationConfigService populationConfigService;
	
	@Inject
	private DataCollectionEventConfigService dataCollectionEventConfigService;
	
	@Inject
	private HarmonizationStudyConfigService harmonizationStudyConfigService;
	
	@Inject
	private HarmonizationPopulationConfigService harmonizationPopulationConfigService;
	
	@Inject
	private Dtos dtos;
	
	@GET
	@Path("/studies/import/_differences")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Response listDifferences(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type) {
		
		try {
		
			Map<String, Boolean> result = new LinkedHashMap<>(); //to keep the keys in the order they were inserted
			
			if (type.equals(INDIVIDUAL_STUDY)) {
				
				this.processComparisonSchemasDefinitions(url, username, password, WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM, 
						(EntityConfigService)individualStudyConfigService, INDIVIDUAL_STUDY_FORM_SECTION, result);
				
				this.processComparisonSchemasDefinitions(url, username, password, WS_CONFIG_POPULATION_FORM_CUSTOM, 
						(EntityConfigService)populationConfigService, POPULATION_FORM_SECTION, result);
				
				this.processComparisonSchemasDefinitions(url, username, password, WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM, 
						(EntityConfigService)dataCollectionEventConfigService, DATA_COLLECTION_EVENT_FORM_SECTION, result);
				
			} else if ( type.equals(HARMONIZATION_STUDY) ) {
				
				this.processComparisonSchemasDefinitions(url, username, password, WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM, 
						(EntityConfigService)harmonizationStudyConfigService, HARMONIZATION_STUDY_FORM_SECTION, result);
				
				this.processComparisonSchemasDefinitions(url, username, password, WS_CONFIG_HARMONIZATION_POPULATION_FORM_CUSTOM, 
						(EntityConfigService)harmonizationPopulationConfigService, HARMONIZATION_POPULATION_FORM_SECTION, result);
			}
			
			return Response.ok( result ).build();
			
		} catch (URISyntaxException|ProtocolException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}
	
	
	@GET
	@Path("/studies/import/_preview")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	@Produces({"application/xml", "application/json", "text/plain", "text/html"})
	public Response listRemoteStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type) {
		
		try {
			
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair(TYPE, type));
			
			HttpURLConnection con = this.prepareRemoteConnection(url, username, password, params, WS_DRAFT_STUDY_STATES);

			int status = con.getResponseCode();

			return Response.ok( this.getRawContent(con) ).status(status).build();
			
		} catch (URISyntaxException|ProtocolException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}

	private void processComparisonSchemasDefinitions(String url, String username, String password, 
			String endpoint, EntityConfigService<EntityConfig> configService, String formTitle,
			Map<String, Boolean> result) throws IOException, URISyntaxException {
		
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Object> content = this.getJSONContent(url, username, password, null, endpoint);
		
		String schema = (mapper.readValue( (String)content.get(SCHEMA), JsonNode.class)).toString();
		String definition = (mapper.readValue( (String)content.get(DEFINITION), JsonNode.class)).toString();
		
		String localSchema = (mapper.readValue( configService.findPartial().get().getSchema(), JsonNode.class)).toString();
		String localDefinition = (mapper.readValue( configService.findPartial().get().getDefinition(), JsonNode.class)).toString();
		
		String formSection = "{ \"formTitle\" : \"" + formTitle + "\", \"endpoint\" : \"" + endpoint + "\" }";
		
		result.put(formSection, Boolean.valueOf(schema.equals(localSchema) && definition.equals(localDefinition)) );
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
	
	
	@PUT
	@Path("/studies/import/_save")
	@RequiresPermissions( {"/draft/individual-study:ADD", "/draft/harmonization-study:ADD" })
	public Response saveStudies(@QueryParam("url") String url, 
			@QueryParam(USERNAME_PARAM) String username, 
			@QueryParam(PWORD_PARAM) String password, 
			@QueryParam(TYPE) String type,
			@QueryParam(IDS) List<String> ids,
			@QueryParam(LIST_DIFFS_FORM) List<String> listDiffsForm) {
	
		log.info("PUT saveStudies. ids = {}", ids);
		log.info("PUT saveStudies. listDiffsForm = {}", listDiffsForm);

		try {
			
			List<String> idsSaved = new ArrayList<>();
			
			for (String id : ids) {
				
				String remoteContent = this.getRawContent(url, username, password, null,
						(type.equals(INDIVIDUAL_STUDY) ? WS_DRAFT_INDIVIDUAL_STUDY_ID : WS_DRAFT_HARMONIZATION_STUDY_ID).replace("{id}", id ));
				
				Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
				ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
				
				if ( type.equals(INDIVIDUAL_STUDY) ) {
					
					this.saveIndividualStudy(idsSaved, id, remoteContent, builder, extensionRegistry, listDiffsForm);
					
				} else if ( type.equals(HARMONIZATION_STUDY) ) {
					
					this.saveHarmonizationStudy(idsSaved, id, remoteContent, builder, extensionRegistry, listDiffsForm);
				}				
			}
			
			return Response.ok(idsSaved).build();
			
			
		} catch (URISyntaxException|ProtocolException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			
		} catch (IOException e) {
			
			log.error( Arrays.toString( e.getStackTrace()) );
			
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
	}


	private void saveHarmonizationStudy(List<String> idsSaved, String id, String remoteContent,
			Mica.StudyDto.Builder builder, ExtensionRegistry extensionRegistry, List<String> listDiffsForm) throws ParseException {
		
		extensionRegistry.add(Mica.HarmonizationStudyDto.type);
		JsonFormat.merge(remoteContent, extensionRegistry, builder);
		
		HarmonizationStudy remoteStudy = (HarmonizationStudy)dtos.fromDto( builder);

		if (!listDiffsForm.contains(HARMONIZATION_STUDY_FORM_SECTION)) {
			
			if (!this.studyIdExistLocally(id)) {
				remoteStudy.setId(null);
				
				if (listDiffsForm.contains(HARMONIZATION_POPULATION_FORM_SECTION)) {
					remoteStudy.setPopulations(null);
				}
				
			} else {
				HarmonizationStudy localStudy =  harmonizationStudyService.findStudy(id);
				
				if (listDiffsForm.contains(HARMONIZATION_POPULATION_FORM_SECTION)) {
					
					remoteStudy.setPopulations(localStudy.getPopulations());
					
				}
			}
			
			harmonizationStudyService.save(remoteStudy);
			
			idsSaved.add(remoteStudy.getId());
			
			log.info("harmonizationStudyService: {}", remoteStudy);
		}

	}


	private void saveIndividualStudy(List<String> idsSaved, String id, String remoteContent,
			Mica.StudyDto.Builder builder, ExtensionRegistry extensionRegistry, List<String> listDiffsForm) throws ParseException {
		
		extensionRegistry.add(Mica.CollectionStudyDto.type);
		JsonFormat.merge(remoteContent, extensionRegistry, builder);
		
		Study remoteStudy = (Study)dtos.fromDto( builder);

		if (!listDiffsForm.contains(INDIVIDUAL_STUDY_FORM_SECTION)) {
			
			if (!this.studyIdExistLocally(id)) {
				remoteStudy.setId(null);

				if (listDiffsForm.contains(POPULATION_FORM_SECTION)) {
					remoteStudy.setPopulations(null);
					
				} else if (listDiffsForm.contains(DATA_COLLECTION_EVENT_FORM_SECTION)) {
					
					for (Population population : remoteStudy.getPopulations()) {
						population.setDataCollectionEvents(null);
					}
				}			
			} else {
				
				Study localStudy = individualStudyService.findStudy(id);
				
				if (listDiffsForm.contains(POPULATION_FORM_SECTION)) {
					
					remoteStudy.setPopulations(localStudy.getPopulations());
					
				} else if (listDiffsForm.contains(DATA_COLLECTION_EVENT_FORM_SECTION)) {
					
					/*for (Population remotePopulation : remoteStudy.getPopulations()) {
						
						for (Population localPopulation : localStudy.getPopulations() ) {
							
							if (remotePopulation.equals(localPopulation) ) {
								
								//TODO: O que fazer neste caso?
								//remotePopulation.getDataCollectionEvents();//??
							}
						}
					}*/
				}
				
			}
			
			individualStudyService.save(remoteStudy);
			
			idsSaved.add(remoteStudy.getId());
			
			log.info("individualStudyService: {}", remoteStudy);
		
		}
	}


	private boolean studyIdExistLocally(String id) {
		
		try {
			studyService.findStudy(id);
			
			return true;

		} catch(NoSuchEntityException ex) {
			return false;
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
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getJSONContent(HttpURLConnection con) throws IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		
		return mapper.readValue(con.getInputStream(), Map.class);
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
		con.setReadTimeout(7000);
		con.setConnectTimeout(7000);
		con.setRequestMethod(HttpMethod.GET.toString());
		con.setDoInput(true);
		con.setDoOutput(true);
		
		String originalInput = username + ":" + password;
		String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());
		
		con.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		con.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTHENTICATION + encodedString );
		con.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE );
		
		con.connect();
		
		return con;
	}
}
