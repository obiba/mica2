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
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.springframework.stereotype.Component;

@Component
@RequiresAuthentication
@Path("/draft/persons")
public class PersonsResource {

  private final Dtos dtos;

  private final PersonService personService;

  private final EventBus eventBus;

  @Inject
  public PersonsResource(Dtos dtos, PersonService personService, EventBus eventBus) {
    this.dtos = dtos;
    this.personService = personService;
    this.eventBus = eventBus;
  }

  @GET
  @Path("/study/{studyId}")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public Set<PersonDto> getStudyMemberships(@PathParam("studyId") String studyId) {
    return personService.getStudyMemberships(studyId).stream().map(member -> dtos.asDto(member, true)).collect(Collectors.toSet());
  }

  @GET
  @Path("/network/{networkId}")
  @RequiresPermissions({ "/draft/individual-study:VIEW", "/draft/harmonization-study:VIEW", "/draft/network:VIEW" })
  public Set<PersonDto> getNetworkMemberships(@PathParam("networkId") String networkId) {
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
