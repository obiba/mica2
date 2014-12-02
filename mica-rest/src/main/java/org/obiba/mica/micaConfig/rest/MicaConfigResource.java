package org.obiba.mica.micaConfig.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.obiba.mica.micaConfig.MicaConfigService;
import org.obiba.mica.micaConfig.OpalCredentialService;
import org.obiba.mica.micaConfig.OpalService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Opal;

import com.codahale.metrics.annotation.Timed;

import static java.util.stream.Collectors.toList;

@Path("/config")
public class MicaConfigResource {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private OpalService opalService;

  @Inject
  private OpalCredentialService opalCredentialService;

  @Inject
  private Dtos dtos;

  @GET
  @Timed
  public Mica.MicaConfigDto get() {
    return dtos.asDto(micaConfigService.getConfig());
  }

  @PUT
  @Timed
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.MicaConfigDto dto) {
    micaConfigService.save(dtos.fromDto(dto));
    return Response.noContent().build();
  }

  @GET
  @Path("/opal-credentials")
  @Timed
  public List<Mica.OpalCredentialDto> getOpalCredentials(String opalUrl) {
    return opalCredentialService.findAllOpalCredentials().stream().map(dtos::asDto).collect(toList());
  }

  @GET
  @Path("/opal-credential/{id}")
  @Timed
  public Mica.OpalCredentialDto getOpalCredential(@PathParam("id")String opalUrl) {
    return dtos.asDto(opalCredentialService.getOpalCredential(opalUrl));
  }

  @GET
  @Path("/opal-credential/{id}/certificate")
  @Timed
  public Response getOpalCredentialCertificate(@PathParam("id")String opalUrl) {
    return Response.ok(opalCredentialService.getCertificate(opalUrl), MediaType.TEXT_PLAIN_TYPE).header(
      "Content-disposition", "attachment; filename=opal-mica-certificate.pem").build();
  }

  @POST
  @Path("/opal-credentials")
  @Timed
  public Response createCredential(Mica.OpalCredentialDto opalCredentialDto, @Context UriInfo uriInfo) {
    createOrUpdateCredential(opalCredentialDto);

    return Response.created(
      uriInfo.getBaseUriBuilder().segment("opal-credential", opalCredentialDto.getOpalUrl()).build()).build();
  }

  @PUT
  @Path("/opal-credential/{id}")
  @Timed
  public Response updateOpalCredential(@PathParam("id")String id, Mica.OpalCredentialDto opalCredentialDto) {
    createOrUpdateCredential(opalCredentialDto);

    return Response.ok().build();
  }

  @DELETE
  @Path("/opal-credential/{id}")
  @Timed
  public Response updateOpalCredential(@PathParam("id")String id) {
    opalCredentialService.deleteOpalCredential(id);

    return Response.ok().build();
  }

  private void createOrUpdateCredential(Mica.OpalCredentialDto opalCredentialDto) {
    if (opalCredentialDto.getType() == Mica.OpalCredentialType.USERNAME) {
      opalCredentialService.createOrUpdateOpalCredential(opalCredentialDto.getOpalUrl(),
        opalCredentialDto.getUsername(), opalCredentialDto.getPassword());
    } else {
      if (opalCredentialDto.getKeyForm().getKeyType() == Mica.KeyType.KEY_PAIR)
         doCreateOrImportKeyPair(opalCredentialDto.getOpalUrl(), opalCredentialDto.getKeyForm());
      else
        doImportCertificate(opalCredentialDto.getOpalUrl(), opalCredentialDto.getKeyForm());
    }
  }

  private void doImportCertificate(String opalUrl, Mica.KeyForm keyForm) {
    opalCredentialService.createOrUpdateOpalCertificateCredential(opalUrl, keyForm.getPublicImport());
  }

  private void doCreateOrImportKeyPair(String opalUrl, Mica.KeyForm keyForm) {
    if(keyForm.hasPrivateForm() && keyForm.hasPublicForm()) {
      Mica.PublicKeyForm pkForm = keyForm.getPublicForm();
      opalCredentialService.createOrUpdateOpalCertificateCredential(opalUrl, keyForm.getPrivateForm().getAlgo(),
        keyForm.getPrivateForm().getSize(), pkForm.getName(), pkForm.getOrganizationalUnit(), pkForm.getOrganization(),
        pkForm.getLocality(), pkForm.getState(), pkForm.getCountry());
    } else if (keyForm.hasPrivateImport()){
      doImportKeyPair(opalUrl, keyForm);
    } else {
      throw new WebApplicationException("Missing private key", Response.Status.BAD_REQUEST);
    }
  }

  private void doImportKeyPair(String opalUrl, Mica.KeyForm keyForm) {
    if(keyForm.hasPublicForm()) {
      Mica.PublicKeyForm pkForm = keyForm.getPublicForm();
      opalCredentialService.createOrUpdateOpalCertificateCredential(opalUrl, keyForm.getPrivateImport(),
        pkForm.getName(), pkForm.getOrganizationalUnit(), pkForm.getOrganization(), pkForm.getLocality(),
        pkForm.getState(), pkForm.getCountry());
    } else if(keyForm.hasPublicImport()) {
      opalCredentialService.createOrUpdateOpalCertificateCredential(opalUrl, keyForm.getPrivateImport(),
        keyForm.getPublicImport());
    } else {
      throw new WebApplicationException("Missing public key", Response.Status.BAD_REQUEST);
    }
  }

  @GET
  @Path("/languages")
  @Timed
  public Map<String, String> getAvailableLanguages() {
    //TODO support user locale (http://jira.obiba.org/jira/browse/MICASERVER-39)
    Locale locale = Locale.ENGLISH;
    return Arrays.asList(Locale.getISOLanguages()).stream()
        .collect(Collectors.toMap(lang -> lang, lang -> new Locale(lang).getDisplayLanguage(locale)));
  }

  @GET
  @Path("/taxonomies")
  public List<Opal.TaxonomyDto> getTaxonomies() {
    return opalService.getTaxonomyDtos();
  }

  @GET
  @Path("/taxonomies/summaries")
  public Opal.TaxonomiesDto getTaxonomySummaries() {
    return opalService.getTaxonomySummaryDtos();
  }

  @GET
  @Path("/taxonomy/{name}")
  public Opal.TaxonomyDto getTaxonomy(@PathParam("name") String name) {
    return opalService.getTaxonomyDto(name);
  }
}
