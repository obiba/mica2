package org.obiba.mica.person.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.git.CommitInfo;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.DocumentDifferenceService;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.core.support.RegexHashMap;
import org.obiba.mica.micaConfig.service.EntityConfigKeyTranslationService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;

import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.GitCommitInfoDto;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.MapDifference;

@Component
@RequiresAuthentication
@Path("/draft/person")
public class PersonResource {

  private final Dtos dtos;

  private final PersonService personService;
  private final SubjectAclService subjectAclService;

  private final StudyService studyService;

  private final NetworkService networkService;

  private final EntityConfigKeyTranslationService entityConfigKeyTranslationService;

  private final MicaConfigService micaConfigService;


  @Inject
  public PersonResource(Dtos dtos, PersonService personService, SubjectAclService subjectAclService, StudyService studyService, NetworkService networkService, EntityConfigKeyTranslationService entityConfigKeyTranslationService, MicaConfigService micaConfigService) {
    this.dtos = dtos;
    this.personService = personService;
    this.subjectAclService = subjectAclService;
    this.studyService = studyService;
    this.networkService = networkService;
    this.entityConfigKeyTranslationService = entityConfigKeyTranslationService;
    this.micaConfigService = micaConfigService;
  }

  @GET
  @Path("/{id}")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public PersonDto get(@PathParam("id") String id) {
    Person person = personService.findById(id);

    if (person == null) {
      throw new NotFoundException("Person with id \"" + id + "\" not found.");
    }

    return dtos.asDto(person, true);
  }

  @PUT
  @Path("/{id}")
  @RequiresPermissions({ "/draft/individual-study:EDIT", "/draft/harmonization-study:EDIT", "/draft/network:EDIT" })
  public PersonDto update(@PathParam("id") String id, PersonDto personDto) {
    if (personDto == null) {
      return dtos.asDto(personService.findById(id), true);
    }

    return dtos.asDto(personService.save(dtos.fromDto(personDto)), true);
  }

  @GET
  @Path("/{id}/study/{studyId}")
  public PersonDto getPersonForStudy(@PathParam("id") String id, @PathParam("studyId") String studyId) {
    Person person = personService.findById(id);

    if (person == null) {
      throw new NotFoundException("Person with id \"" + id + "\" not found.");
    }

    if (studyService.isCollectionStudy(studyId)) {
      subjectAclService.checkPermission("/draft/individual-study", "EDIT", studyId);
    } else {
      subjectAclService.checkPermission("/draft/harmonization-study", "EDIT", studyId);
    }

    return dtos.asDto(person, true);
  }

  @DELETE
  @Path("/{id}/study/{studyId}/role/{role}")
  public PersonDto removePersonRoleFromStudy(@PathParam("id") String id, @PathParam("studyId") String studyId, @PathParam("role") String role) {
    Person person = personService.findById(id);

    if (studyService.isCollectionStudy(studyId)) {
      subjectAclService.checkPermission("/draft/individual-study", "EDIT", studyId);
    } else {
      subjectAclService.checkPermission("/draft/harmonization-study", "EDIT", studyId);
    }

    person.getStudyMemberships().removeIf(m -> m.getParentId().equals(studyId) && m.getRole().equals(role));
    return dtos.asDto(personService.save(person), true);
  }

  @PUT
  @Path("/{id}/study/{studyId}/role/{role}")
  public PersonDto updatePersonRoleForStudy(@PathParam("id") String id, @PathParam("studyId") String studyId, PersonDto personDto, @PathParam("role") String role) {
    if (personDto == null) {
      return dtos.asDto(personService.findById(id), true);
    }

    Person person = dtos.fromDto(personDto);
    if (!micaConfigService.getRoles().contains(role)) {
      throw new IllegalArgumentException(String.format("'%s' is not a valid role", role));
    }

    if (studyService.isCollectionStudy(studyId)) {
      subjectAclService.checkPermission("/draft/individual-study", "EDIT", studyId);
    } else {
      subjectAclService.checkPermission("/draft/harmonization-study", "EDIT", studyId);
    }

    person.addStudy(studyService.findStudy(studyId), role);

    return dtos.asDto(personService.save(person), true);
  }

  @GET
  @Path("/{id}/network/{networkId}")
  public PersonDto getPersonForNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId) {
    Person person = personService.findById(id);

    if (person == null) {
      throw new NotFoundException("Person with id \"" + id + "\" not found.");
    }

    subjectAclService.checkPermission("/draft/network", "EDIT", networkId);
    return dtos.asDto(person, true);
  }

  @DELETE
  @Path("/{id}/network/{networkId}/role/{role}")
  public PersonDto removePersonRoleFromNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId, @PathParam("role") String role) {
    Person person = personService.findById(id);
    subjectAclService.checkPermission("/draft/network", "EDIT", networkId);
    person.getNetworkMemberships().removeIf(m -> m.getParentId().equals(networkId) && m.getRole().equals(role));
    return dtos.asDto(personService.save(person), true);
  }

  @PUT
  @Path("/{id}/network/{networkId}/role/{role}")
  public PersonDto updatePersonRoleForNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId, PersonDto personDto, @PathParam("role") String role) {
    if (personDto == null) {
      return dtos.asDto(personService.findById(id), true);
    }

    Person person = dtos.fromDto(personDto);
    if (!micaConfigService.getRoles().contains(role)) {
      throw new IllegalArgumentException(String.format("'%s' is not a valid role", role));
    }

    subjectAclService.checkPermission("/draft/network", "EDIT", networkId);
    person.addNetwork(networkService.findById(networkId), role);
    return dtos.asDto(personService.save(person), true);
  }

  @DELETE
  @Path("/{id}")
  @RequiresPermissions({ "/draft/individual-study:EDIT", "/draft/harmonization-study:EDIT", "/draft/network:EDIT" })
  public Response delete(@PathParam("id") String id) {
    personService.delete(id);
    return Response.ok().build();
  }

  @GET
  @Path("/{id}/commit/{commitId}/view")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public PersonDto getFromCommit(@PathParam("id") String id, @NotNull @PathParam("commitId") String commitId) throws IOException {
    return dtos.asDto(personService.getFromCommit(personService.findById(id), commitId), true);
  }

  @GET
  @Path("/{id}/commits")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public List<GitCommitInfoDto> getCommitsInfo(@PathParam("id") String id) {

    try {
      return dtos.asDto(personService.getCommitInfos(personService.findById(id)));
    } catch(Exception ex) {
      //
    }

    return new ArrayList<>();
  }

  @PUT
  @Path("/{id}/commit/{commitId}/restore")
  @RequiresPermissions({ "/draft/individual-study:EDIT", "/draft/harmonization-study:EDIT", "/draft/network:EDIT" })
  public Response restoreCommit(@PathParam("id") String id, @NotNull @PathParam("commitId") String commitId) throws IOException {
    Person gitPersistable = personService.getFromCommit(personService.findById(id), commitId);

    if (gitPersistable != null){
      personService.save(gitPersistable);
    }

    return Response.noContent().build();
  }

  @GET
  @Path("/{id}/commit/{commitId}")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public GitCommitInfoDto getCommitInfo(@PathParam("id") String id, @NotNull @PathParam("commitId") String commitId) throws IOException {
    CommitInfo commitInfo = personService.getCommitInfo(personService.findById(id), commitId);
    Iterable<String> diffEntries = personService.getDiffEntries(personService.findById(id), commitId, null);

    CommitInfo build = CommitInfo.Builder.createFromObject(commitInfo).diffEntries((List<String>) diffEntries).build();

    return dtos.asDto(build);
  }

  @GET
  @Path("/{id}/_diff")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public Response diff(@PathParam("id") String id, @NotNull @QueryParam("left") String left, @NotNull @QueryParam("right") String right, @QueryParam("locale") @DefaultValue("en") String locale) {
    Person leftCommit = personService.getFromCommit(personService.findById(id), left);
    Person rightCommit = personService.getFromCommit(personService.findById(id), right);

    Map<String, Map<String, List<Object>>> data = new HashMap<>();

    try {
      MapDifference<String, Object> difference = DocumentDifferenceService.diff(leftCommit, rightCommit);
      RegexHashMap completeConfigTranslationMap = entityConfigKeyTranslationService.getCompleteConfigTranslationMap(personService.getTypeName(), locale);

      data = DocumentDifferenceService.withTranslations(difference, completeConfigTranslationMap);

    } catch (JsonProcessingException e) {
      //
    }

    return Response.ok(data, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
