package org.obiba.mica.person.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.GitCommitInfoDto;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.MapDifference;


/**
 * A person is shared by the studies and networks it is a member of: the details are updated only by a user who can
 * edit all of them, a membership by a user who can edit its study or network.
 */
@Component
@RequiresAuthentication
@Path("/draft/person")
public class PersonResource {

  private static final String EDIT = "EDIT";

  private final Dtos dtos;

  private final PersonService personService;

  private final SubjectAclService subjectAclService;

  private final StudyService studyService;

  private final NetworkService networkService;

  private final EntityConfigKeyTranslationService entityConfigKeyTranslationService;

  private final MicaConfigService micaConfigService;

  private final Validator validator;

  @Inject
  public PersonResource(Dtos dtos, PersonService personService, SubjectAclService subjectAclService,
                        StudyService studyService, NetworkService networkService,
                        EntityConfigKeyTranslationService entityConfigKeyTranslationService,
                        MicaConfigService micaConfigService, Validator validator) {
    this.dtos = dtos;
    this.personService = personService;
    this.subjectAclService = subjectAclService;
    this.studyService = studyService;
    this.networkService = networkService;
    this.entityConfigKeyTranslationService = entityConfigKeyTranslationService;
    this.micaConfigService = micaConfigService;
    this.validator = validator;
  }

  @GET
  @Path("/{id}")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public PersonDto get(@PathParam("id") String id) {
    return asDtoWithCanEdit(findPerson(id));
  }

  /**
   * Updates the details of the person, not its memberships (ignored, see the role endpoints). The version read must
   * be sent: 409 when the person changed meanwhile.
   */
  @PUT
  @Path("/{id}")
  public PersonDto update(@PathParam("id") String id, PersonDto personDto) {
    Person stored = findPerson(id);
    if (!personService.canEditDetails(stored)) throw new ForbiddenException();

    if (personDto == null) throw PersonErrors.missing();
    if (personDto.hasId() && !id.equals(personDto.getId())) throw PersonErrors.idMismatch();
    if (!personDto.hasVersion()) throw PersonErrors.versionMissing();

    if (!PersonService.sameEmail(stored.getEmail(), personDto.getEmail())
      && personService.isEmailUsed(personDto.getEmail(), id)) {
      throw PersonErrors.emailUsed();
    }

    Person details = dtos.fromDto(personDto);
    // the targeted update skips the validation done on a document save
    Set<ConstraintViolation<Person>> violations = validator.validate(details);
    if (!violations.isEmpty()) {
      throw PersonErrors.invalid(violations.stream().map(v -> v.getPropertyPath() + " " + v.getMessage())
        .collect(Collectors.joining(", ")));
    }

    Person updated = personService.updateDetails(id, personDto.getVersion(), details);
    if (updated == null) throw PersonErrors.versionConflict();
    return asDtoWithCanEdit(updated);
  }

  @GET
  @Path("/{id}/study/{studyId}")
  public PersonDto getPersonForStudy(@PathParam("id") String id, @PathParam("studyId") String studyId) {
    Person person = findPerson(id);
    subjectAclService.checkPermission(studyResourceForCheck(studyId), EDIT, studyId);
    return asDtoWithCanEdit(person);
  }

  @DELETE
  @Path("/{id}/study/{studyId}/role/{role}")
  public PersonDto removePersonRoleFromStudy(@PathParam("id") String id, @PathParam("studyId") String studyId, @PathParam("role") String role) {
    subjectAclService.checkPermission(studyResourceForCheck(studyId), EDIT, studyId);
    return dtos.asDto(found(id, personService.removeStudyMembership(id, studyId, role)), true);
  }

  @PUT
  @Path("/{id}/study/{studyId}/role/{role}")
  public PersonDto updatePersonRoleForStudy(@PathParam("id") String id, @PathParam("studyId") String studyId, @PathParam("role") String role) {
    BaseStudy study = studyService.findStudy(studyId); // 404 when missing
    subjectAclService.checkPermission(study instanceof Study ? PersonService.INDIVIDUAL_STUDY_RESOURCE
      : PersonService.HARMONIZATION_STUDY_RESOURCE, EDIT, study.getId());
    checkRole(role);
    return dtos.asDto(found(id, personService.addStudyMembership(id, study.getId(), role)), true);
  }

  @GET
  @Path("/{id}/network/{networkId}")
  public PersonDto getPersonForNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId) {
    Person person = findPerson(id);
    subjectAclService.checkPermission(PersonService.NETWORK_RESOURCE, EDIT, networkId);
    return asDtoWithCanEdit(person);
  }

  @DELETE
  @Path("/{id}/network/{networkId}/role/{role}")
  public PersonDto removePersonRoleFromNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId, @PathParam("role") String role) {
    subjectAclService.checkPermission(PersonService.NETWORK_RESOURCE, EDIT, networkId);
    return dtos.asDto(found(id, personService.removeNetworkMembership(id, networkId, role)), true);
  }

  @PUT
  @Path("/{id}/network/{networkId}/role/{role}")
  public PersonDto updatePersonRoleForNetwork(@PathParam("id") String id, @PathParam("networkId") String networkId, @PathParam("role") String role) {
    String loadedId = networkService.findById(networkId).getId(); // 404 when missing
    subjectAclService.checkPermission(PersonService.NETWORK_RESOURCE, EDIT, loadedId);
    checkRole(role);
    return dtos.asDto(found(id, personService.addNetworkMembership(id, loadedId, role)), true);
  }

  @DELETE
  @Path("/{id}")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response delete(@PathParam("id") String id) {
    findPerson(id);
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

  /**
   * Restores the commit content, without the memberships of missing studies or networks and of roles that are not
   * configured. 409 when the person changed meanwhile.
   */
  @PUT
  @Path("/{id}/commit/{commitId}/restore")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response restoreCommit(@PathParam("id") String id, @NotNull @PathParam("commitId") String commitId) throws IOException {
    Person stored = findPerson(id);
    Person fromCommit = personService.getFromCommit(stored, commitId);
    if (fromCommit == null) return Response.noContent().build();
    if (!PersonService.sameEmail(stored.getEmail(), fromCommit.getEmail())
      && personService.isEmailUsed(fromCommit.getEmail(), id)) {
      throw PersonErrors.emailUsed();
    }
    if (personService.reinstate(stored, fromCommit) == null) {
      throw PersonErrors.versionConflict();
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
    // the version is a write guard, not a person field to compare
    leftCommit.setVersion(null);
    rightCommit.setVersion(null);

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

  private PersonDto asDtoWithCanEdit(Person person) {
    return dtos.asDto(person, true).toBuilder().setCanEdit(personService.canEditDetails(person)).build();
  }

  /** the ACL resource to check for the study, harmonization when there is no such study (not permitted then) */
  private String studyResourceForCheck(String studyId) {
    String resource = personService.studyResource(studyId);
    return resource == null ? PersonService.HARMONIZATION_STUDY_RESOURCE : resource;
  }

  private void checkRole(String role) {
    if (!micaConfigService.getRoles().contains(role)) throw PersonErrors.invalidRole(role);
  }

  private Person findPerson(String id) {
    Person person = personService.findById(id);
    if (person == null) {
      throw new NotFoundException("Person with id \"" + id + "\" not found.");
    }
    return person;
  }

  /** the written person, 404 when it is missing */
  private static Person found(String id, Person person) {
    if (person == null) throw new NotFoundException("Person with id \"" + id + "\" not found.");
    return person;
  }
}
