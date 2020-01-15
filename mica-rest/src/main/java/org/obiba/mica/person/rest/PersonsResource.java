package org.obiba.mica.person.rest;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
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

  @Inject
  public PersonsResource(Dtos dtos, PersonService personService) {
    this.dtos = dtos;
    this.personService = personService;
  }

  @GET
  @Path("/study/{studyId}")
  public Set<PersonDto> getStudyMemberships(@PathParam("studyId") String studyId) {
    return personService.getStudyMemberships(studyId).stream().map(member -> dtos.asDto(member, true)).collect(Collectors.toSet());
  }

  @GET
  @Path("/network/{networkId}")
  public Set<PersonDto> getNetworkMemberships(@PathParam("networkId") String networkId) {
    return personService.getNetworkMemberships(networkId).stream().map(member -> dtos.asDto(member, true)).collect(Collectors.toSet());
  }

  @POST
  public PersonDto createPerson(PersonDto personDto) {
    if (personDto == null) {
      return null;
    }

    return dtos.asDto(personService.save(dtos.fromDto(personDto)), true);
  }
}
