package org.obiba.mica.person.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.springframework.stereotype.Component;

@Component
@RequiresAuthentication
@Path("/draft/person")
public class PersonResource {

  private final Dtos dtos;

  private final PersonService personService;

  @Inject
  public PersonResource(Dtos dtos, PersonService personService) {
    this.dtos = dtos;
    this.personService = personService;
  }

  @GET
  @Path("/{id}")
  public PersonDto get(@PathParam("id") String id) {
    Person person = personService.findById(id);

    if (person == null) {
      throw new NotFoundException("Person with id \"" + id + "\" not found.");
    }

    return dtos.asDto(person, true);
  }

  @PUT
  @Path("/{id}")
  public PersonDto update(@PathParam("id") String id, PersonDto personDto) {
    if (personDto == null) {
      return dtos.asDto(personService.findById(id), true);
    }

    return dtos.asDto(personService.save(dtos.fromDto(personDto)), true);
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") String id) {
    personService.delete(id);
    return Response.ok().build();
  }
}
