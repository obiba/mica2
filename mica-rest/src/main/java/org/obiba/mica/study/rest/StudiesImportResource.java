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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.service.DataCollectionEventConfigService;
import org.obiba.mica.micaConfig.service.EntityConfigService;
import org.obiba.mica.micaConfig.service.HarmonizationStudyConfigService;
import org.obiba.mica.micaConfig.service.IndividualStudyConfigService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
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
import org.obiba.mica.web.model.Mica.MembershipsDto;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import support.legacy.UpgradeLegacyEntities;

import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("request")
@Path("/draft")
@RequiresAuthentication
public class StudiesImportResource {

  private static final String SAVE_STUDIES = "saveStudies";
  private static final String LIST_DIFFERENCES = "listDifferences";
  private static final String LIST_REMOTE_STUDIES = "listRemoteStudies";
  private static final String LIST_DIFFS_FORM = "listDiffsForm";
  private static final String WS_CONFIG_HARMONIZATION_POPULATION_FORM_CUSTOM = "/ws/config/harmonization-population/form-custom";
  private static final String WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM = "/ws/config/harmonization-study/form-custom";
  private static final String WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM = "/ws/config/data-collection-event/form-custom";
  private static final String WS_CONFIG_POPULATION_FORM_CUSTOM = "/ws/config/population/form-custom";
  private static final String WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM = "/ws/config/individual-study/form-custom";

  private static final String RESOURCE_PATH = "<resource_path>";
  private static final String STUDY_ID = "<study_id>";
  private static final String LOGO_ID = "<logo_id>";

  private static final String WS_DRAFT_STUDY_LOGO = "/ws/draft/" + RESOURCE_PATH + "/" + STUDY_ID + "/file/" + LOGO_ID + "/_download";
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
  private static final String NONE = "none";

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
  private TempFileService tempFileService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private PersonService personService;

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper mapper;

  @GET
  @Path("/studies/import/_differences")
  @RequiresPermissions({"/draft/individual-study:ADD", "/draft/harmonization-study:ADD"})
  @Produces({"application/xml", "application/json", "text/plain", "text/html"})
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Response listDifferences(@QueryParam("url") String url,
                                  @QueryParam(USERNAME_PARAM) String username,
                                  @QueryParam(PWORD_PARAM) String password,
                                  @QueryParam(TYPE) String type) {

    try {

      if (!micaConfigService.getConfig().isImportStudiesFeatureEnabled())
        return Response.status(HttpStatus.SC_UNAUTHORIZED).build();

      Map<String, Boolean> result = new LinkedHashMap<>(); //to keep the keys in the order they were inserted

      if (type.equals(INDIVIDUAL_STUDY)) {

        result.putAll(this.compareSchemaDefinition(url, username, password, WS_CONFIG_INDIVIDUAL_STUDY_FORM_CUSTOM,
          (EntityConfigService) individualStudyConfigService, INDIVIDUAL_STUDY_FORM_SECTION, NONE));

        result.putAll(this.compareSchemaDefinition(url, username, password, WS_CONFIG_POPULATION_FORM_CUSTOM,
          (EntityConfigService) populationConfigService, POPULATION_FORM_SECTION, INDIVIDUAL_STUDY_FORM_SECTION));

        result.putAll(this.compareSchemaDefinition(url, username, password, WS_CONFIG_DATA_COLLECTION_EVENT_FORM_CUSTOM,
          (EntityConfigService) dataCollectionEventConfigService, DATA_COLLECTION_EVENT_FORM_SECTION, POPULATION_FORM_SECTION));

      } else if (type.equals(HARMONIZATION_STUDY)) {

        result.putAll(this.compareSchemaDefinition(url, username, password, WS_CONFIG_HARMONIZATION_STUDY_FORM_CUSTOM,
          (EntityConfigService) harmonizationStudyConfigService, HARMONIZATION_STUDY_FORM_SECTION, NONE));
      }

      return Response.ok(result).build();

    } catch (Exception e) {

      log.error(LIST_DIFFERENCES, e);

      return Response.ok(this.handleException(e)).build();
    }
  }

  @GET
  @Path("/studies/import/_preview")
  @RequiresPermissions({"/draft/individual-study:ADD", "/draft/harmonization-study:ADD"})
  @Produces({"application/xml", "application/json", "text/plain", "text/html"})
  public Response listRemoteStudies(@QueryParam("url") String url,
                                    @QueryParam(USERNAME_PARAM) String username,
                                    @QueryParam(PWORD_PARAM) String password,
                                    @QueryParam(TYPE) String type) {

    try {

      if (!micaConfigService.getConfig().isImportStudiesFeatureEnabled())
        return Response.status(HttpStatus.SC_UNAUTHORIZED).build();

      List<NameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair(TYPE, type));

      return Response.ok(this.getRawContent(url, username, password, params, WS_DRAFT_STUDY_STATES)).build();

    } catch (Exception e) {

      log.error(LIST_REMOTE_STUDIES, e);

      return Response.ok(this.handleException(e)).build();
    }
  }

  @GET
  @Path("/studies/import/_summary")
  @RequiresPermissions({"/draft/individual-study:ADD", "/draft/harmonization-study:ADD"})
  @Produces({"application/xml", "application/json", "text/plain", "text/html"})
  public Response checkIfAlreadyExistsLocally(@QueryParam(IDS) List<String> ids, @QueryParam(TYPE) String type) {

    if (!micaConfigService.getConfig().isImportStudiesFeatureEnabled())
      return Response.status(HttpStatus.SC_UNAUTHORIZED).build();

    Map<String, String> existingIds = new HashMap<>();

    for (String id : ids) {
      try {
        BaseStudy localStudy = studyService.findStudy(id);

        Integer localPopulationSize = localStudy.hasPopulations() ? localStudy.getPopulations().size() : 0;
        Integer localDCEsSize = 0;

        for (Population localPopulation : localStudy.getPopulations()) {
          localDCEsSize += localPopulation.getDataCollectionEvents().size();
        }

        JsonNode jsonDTO = mapper.createObjectNode();
        ((ObjectNode) jsonDTO).put("conflict", !localStudy.getResourcePath().equals(type));
        ((ObjectNode) jsonDTO).put("localPopulationSize", localPopulationSize);
        ((ObjectNode) jsonDTO).put("localDCEsSize", localDCEsSize);

        existingIds.put(localStudy.getId(), jsonDTO.toString());

      } catch (NoSuchEntityException ex) {
        //if study doesn't exist locally, ignore.
        log.info("Study id does not exist locally: {}", id);
      }
    }

    return Response.ok(existingIds).build();
  }

  @PUT
  @Path("/studies/import/_save")
  @RequiresPermissions({"/draft/individual-study:ADD", "/draft/harmonization-study:ADD"})
  public Response saveStudies(@QueryParam("url") String url,
                              @QueryParam(USERNAME_PARAM) String username,
                              @QueryParam(PWORD_PARAM) String password,
                              @QueryParam(TYPE) String type,
                              @QueryParam(IDS) List<String> ids,
                              @QueryParam(LIST_DIFFS_FORM) List<String> listDiffsForm) {

    if (!micaConfigService.getConfig().isImportStudiesFeatureEnabled())
      return Response.status(HttpStatus.SC_UNAUTHORIZED).build();

    Map<String, Integer> idsSavedStatus = new LinkedHashMap<>();

    for (String id : ids) {
      try {

        String remoteContent = this.getRawContent(url, username, password, null,
          (type.equals(INDIVIDUAL_STUDY) ? WS_DRAFT_INDIVIDUAL_STUDY_ID : WS_DRAFT_HARMONIZATION_STUDY_ID).replace("{id}", id));

        Mica.StudyDto.Builder builder = Mica.StudyDto.newBuilder();
        ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

        if (type.equals(INDIVIDUAL_STUDY)) {

          Study studySaved = this.saveIndividualStudy(id, remoteContent, builder, extensionRegistry, listDiffsForm);
          this.saveLogoImage(studySaved, url, username, password);

          idsSavedStatus.put(id, HttpStatus.SC_OK);

        } else if (type.equals(HARMONIZATION_STUDY)) {

          HarmonizationStudy studySaved = this.saveHarmonizationStudy(id, remoteContent, builder, extensionRegistry, listDiffsForm);
          this.saveLogoImage(studySaved, url, username, password);

          idsSavedStatus.put(id, HttpStatus.SC_OK);
        }

      } catch (Exception e) {

        log.error(SAVE_STUDIES, e);

        idsSavedStatus.put(id, this.handleException(e));
      }
    }

    return Response.ok(idsSavedStatus).build();
  }

  private Map<String, Boolean> compareSchemaDefinition(String url, String username, String password,
                                                       String endpoint, EntityConfigService<EntityConfig> configService, String formSection,
                                                       String parentFormSection) throws IOException, URISyntaxException {

    Map<String, Boolean> result = new LinkedHashMap<>();

    Map<String, Object> content = this.getJSONContent(url, username, password, null, endpoint);

    String schema = (mapper.readValue((String) content.get(SCHEMA), JsonNode.class)).toString();
    String definition = (mapper.readValue((String) content.get(DEFINITION), JsonNode.class)).toString();

    EntityConfig entityConfig = configService.findPartial().get();

    String localSchema = (mapper.readValue(entityConfig.getSchema(), JsonNode.class)).toString();
    String localDefinition = (mapper.readValue(entityConfig.getDefinition(), JsonNode.class)).toString();

    JsonNode jsonDTO = mapper.createObjectNode();
    ((ObjectNode) jsonDTO).put("formSection", formSection);
    ((ObjectNode) jsonDTO).put("parentFormSection", parentFormSection);
    ((ObjectNode) jsonDTO).put("endpoint", endpoint);

    result.put(jsonDTO.toString(), Boolean.valueOf(schema.equals(localSchema) && definition.equals(localDefinition)));

    return result;
  }



  private Study saveIndividualStudy(String id, String remoteContent,
                                    Mica.StudyDto.Builder builder, ExtensionRegistry extensionRegistry, List<String> listDiffsForm) throws ParseException {

    JsonFormat.merge(UpgradeLegacyEntities.upgradeStudy(remoteContent), extensionRegistry, builder);

    Mica.StudyDtoOrBuilder dtoBuilder = (Mica.StudyDtoOrBuilder) builder;

    Study remoteStudy = (Study) dtos.fromDto(builder);

    remoteStudy.setId(id); //to make sure the same ID from the remote host

    if (!listDiffsForm.contains(INDIVIDUAL_STUDY_FORM_SECTION)) {

      if (!this.studyIdExistLocally(id)) {
        this.prepareCreateOperation(listDiffsForm, remoteStudy);
      } else {
        this.prepareReplaceOperation(id, listDiffsForm, remoteStudy);
      }

      personService.getStudyMemberships(id).forEach(person -> {
        personService.delete(person.getId());
      });

      individualStudyService.save(remoteStudy);

      for (MembershipsDto membershipsDto : dtoBuilder.getMembershipsList()) {
        for (PersonDto personDto : membershipsDto.getMembersList()) {
          Person person = dtos.fromDto(personDto);

          for (Iterator<Person.Membership> iterator = person.getStudyMemberships().iterator(); iterator.hasNext(); ) {
            Person.Membership membership = iterator.next();
            if (!this.studyIdExistLocally(membership.getParentId())) {
              iterator.remove();
            }
          }

          personService.save(person);
        }
      }
    }

    remoteStudy = individualStudyService.findStudy(remoteStudy.getId());

    return remoteStudy;
  }

  private void saveLogoImage(BaseStudy remoteStudy, String url, String username, String password) throws FileUploadException {

    if (remoteStudy != null && remoteStudy.hasLogo()) {

      String studyLogoId = remoteStudy.getLogo().getId();

      StringBuilder endpoint = new StringBuilder(WS_DRAFT_STUDY_LOGO);

      endpoint.replace(endpoint.indexOf(RESOURCE_PATH), endpoint.indexOf(RESOURCE_PATH) + RESOURCE_PATH.length(), remoteStudy.getResourcePath());
      endpoint.replace(endpoint.indexOf(STUDY_ID), endpoint.indexOf(STUDY_ID) + STUDY_ID.length(), remoteStudy.getId());
      endpoint.replace(endpoint.indexOf(LOGO_ID), endpoint.indexOf(LOGO_ID) + LOGO_ID.length(), studyLogoId);

      try {

        HttpURLConnection con = this.prepareRemoteConnection(url, username, password, null, endpoint.toString());

        String disposition = con.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);

        String fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");

        TempFile tempFile = tempFileService.addTempFile(fileName, con.getInputStream());

        remoteStudy.getLogo().setId(tempFile.getId());
        remoteStudy.getLogo().setJustUploaded(true);

        studyService.save(remoteStudy, null);

      } catch (IOException | URISyntaxException e) {

        throw new FileUploadException();
      }
    }
  }

  private HarmonizationStudy saveHarmonizationStudy(String id, String remoteContent,
                                                    Mica.StudyDto.Builder builder, ExtensionRegistry extensionRegistry, List<String> listDiffsForm) throws ParseException {

    builder.setInitiative(Mica.HarmonizationStudyDto.newBuilder());
    JsonFormat.merge(UpgradeLegacyEntities.upgradeStudy(remoteContent), extensionRegistry, builder);

    Mica.StudyDtoOrBuilder dtoBuilder = (Mica.StudyDtoOrBuilder) builder;

    HarmonizationStudy remoteStudy = (HarmonizationStudy) dtos.fromDto(builder);
    remoteStudy.setId(id); //to make sure the same ID from the remote host

    if (!listDiffsForm.contains(HARMONIZATION_STUDY_FORM_SECTION)) {

      if (!this.studyIdExistLocally(id)) {
        remoteStudy.setId(null);

        if (listDiffsForm.contains(HARMONIZATION_POPULATION_FORM_SECTION)) {
          remoteStudy.setPopulations(Sets.newTreeSet());
        }

      } else {
        HarmonizationStudy localStudy = harmonizationStudyService.findStudy(id);
        if (listDiffsForm.contains(HARMONIZATION_POPULATION_FORM_SECTION)) {
          remoteStudy.setPopulations(localStudy.getPopulations());
        }
      }

      personService.getStudyMemberships(id).forEach(person -> {
        personService.delete(person.getId());
      });

      harmonizationStudyService.save(remoteStudy);

      for (MembershipsDto membershipsDto : dtoBuilder.getMembershipsList()) {
        for (PersonDto personDto : membershipsDto.getMembersList()) {
          Person person = dtos.fromDto(personDto);

          for (Iterator<Person.Membership> iterator = person.getStudyMemberships().iterator(); iterator.hasNext(); ) {
            Person.Membership membership = iterator.next();
            if (!this.studyIdExistLocally(membership.getParentId())) {
              iterator.remove();
            }
          }

          personService.save(person);
        }
      }
    }

    remoteStudy = harmonizationStudyService.findStudy(remoteStudy.getId());

    return remoteStudy;
  }

  private void prepareReplaceOperation(String id, List<String> listDiffsForm, Study remoteStudy) {

    Study localStudy = individualStudyService.findStudy(id);

    if (listDiffsForm.contains(POPULATION_FORM_SECTION)) {
      remoteStudy.setPopulations(localStudy.getPopulations());
    } else if (listDiffsForm.contains(DATA_COLLECTION_EVENT_FORM_SECTION)) {
      for (Population remotePopulation : remoteStudy.getPopulations()) {
        if (!localStudy.hasPopulations() && localStudy.getPopulations().contains(remotePopulation)) {
          remotePopulation.setDataCollectionEvents(Sets.newTreeSet());
        } else {
          for (Population localPopulation : localStudy.getPopulations()) {
            if (localPopulation.equals(remotePopulation)) {
              remotePopulation.setDataCollectionEvents(localPopulation.getDataCollectionEvents());
            }
          }
        }
      }
    }
  }

  private void prepareCreateOperation(List<String> listDiffsForm, Study remoteStudy) {

    remoteStudy.setId(null);

    if (listDiffsForm.contains(POPULATION_FORM_SECTION)) {
      remoteStudy.setPopulations(Sets.newTreeSet());
    } else if (listDiffsForm.contains(DATA_COLLECTION_EVENT_FORM_SECTION)) {
      for (Population population : remoteStudy.getPopulations()) {
        population.setDataCollectionEvents(Sets.newTreeSet());
      }
    }
  }

  private int handleException(Exception e) {

    if (e instanceof UnknownHostException) return HttpStatus.SC_NOT_FOUND;
    else if (e instanceof URISyntaxException) return HttpStatus.SC_BAD_REQUEST;
    else if (e instanceof ProtocolException) return HttpStatus.SC_BAD_REQUEST;
    else if (e instanceof FileNotFoundException) return HttpStatus.SC_SERVICE_UNAVAILABLE;
    else if (e instanceof ConnectException) return HttpStatus.SC_REQUEST_TIMEOUT;
    else if (e instanceof IOException) return HttpStatus.SC_UNAUTHORIZED;
    else if (e instanceof FileUploadException) return HttpStatus.SC_NO_CONTENT;
    else return HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  private boolean studyIdExistLocally(String id) {

    try {
      studyService.findStudy(id);
      return true;
    } catch (NoSuchEntityException ex) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getJSONContent(String url, String username, String password, List<NameValuePair> param, String endpoint)
    throws IOException, URISyntaxException {

    HttpURLConnection con = this.prepareRemoteConnection(url, username, password, param, endpoint);

    return mapper.readValue(con.getInputStream(), Map.class);
  }

  private String getRawContent(String url, String username, String password, List<NameValuePair> param, String endpoint)
    throws IOException, URISyntaxException {

    HttpURLConnection con = this.prepareRemoteConnection(url, username, password, param, endpoint);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder content = new StringBuilder();

    while ((inputLine = in.readLine()) != null) {
      content.append(inputLine);
    }

    in.close();

    return content.toString();
  }

  private HttpURLConnection prepareRemoteConnection(String url, String username, String password,
                                                    List<NameValuePair> param, String endpoint) throws IOException, URISyntaxException {

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

    HttpURLConnection con = (HttpURLConnection) urlCon;
    con.setReadTimeout(7000);
    con.setConnectTimeout(7000);
    con.setRequestMethod(HttpMethod.GET.toString());
    con.setDoInput(true);
    con.setDoOutput(true);

    String originalInput = username + ":" + password;
    String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());

    con.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    con.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTHENTICATION + encodedString);
    con.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);

    con.connect();

    return con;
  }
}
