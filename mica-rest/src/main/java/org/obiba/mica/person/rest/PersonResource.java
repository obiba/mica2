package org.obiba.mica.person.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.git.CommitInfo;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.DocumentDifferenceService;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.core.support.RegexHashMap;
import org.obiba.mica.micaConfig.service.EntityConfigKeyTranslationService;
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

  private final EntityConfigKeyTranslationService entityConfigKeyTranslationService;

  @Inject
  public PersonResource(Dtos dtos, PersonService personService, EntityConfigKeyTranslationService entityConfigKeyTranslationService) {
    this.dtos = dtos;
    this.personService = personService;
    this.entityConfigKeyTranslationService = entityConfigKeyTranslationService;
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
