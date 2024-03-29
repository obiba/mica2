package org.obiba.mica.person.rest;

import com.google.common.eventbus.EventBus;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.contact.event.IndexContactsEvent;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.springframework.stereotype.Component;

@Component
@RequiresAuthentication
@Path("/draft/persons")
public class PersonsResource {

  private final Dtos dtos;

  private final PersonService personService;

  private final SubjectAclService subjectAclService;

  private final StudyService studyService;

  private final EventBus eventBus;

  @Inject
  public PersonsResource(Dtos dtos, PersonService personService, SubjectAclService subjectAclService,
    StudyService service, EventBus eventBus) {
    this.dtos = dtos;
    this.personService = personService;
    this.subjectAclService = subjectAclService;
    this.studyService = service;
    this.eventBus = eventBus;
  }

  @GET
  @Path("/study/{studyId}")
  public Set<PersonDto> getStudyMemberships(@PathParam("studyId") String studyId) {
    if (studyService.isCollectionStudy(studyId)) {
      subjectAclService.checkPermission("/draft/individual-study", "VIEW", studyId);
    } else {
      subjectAclService.checkPermission("/draft/harmonization-study", "VIEW", studyId);
    }
    return personService.getStudyMemberships(studyId).stream().map(member -> dtos.asDto(member, true)).collect(Collectors.toSet());
  }

  @GET
  @Path("/network/{networkId}")
  public Set<PersonDto> getNetworkMemberships(@PathParam("networkId") String networkId) {
    subjectAclService.checkPermission("/draft/network", "VIEW", networkId);
    return personService.getNetworkMemberships(networkId).stream().map(member -> dtos.asDto(member, true)).collect(Collectors.toSet());
  }

  @PUT
  @Path("/_index")
  @RequiresPermissions({ "/draft/individual-study:EDIT", "/draft/harmonization-study:EDIT", "/draft/network:EDIT" })
  public Response index() {
    eventBus.post(new IndexContactsEvent());
    return Response.noContent().build();
  }

  @POST
  @RequiresPermissions({ "/draft/individual-study:EDIT", "/draft/harmonization-study:EDIT", "/draft/network:EDIT" })
  public PersonDto createPerson(PersonDto personDto) {
    if (personDto == null) {
      return null;
    }

    return dtos.asDto(personService.save(dtos.fromDto(personDto)), true);
  }
}
