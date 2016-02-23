package org.obiba.mica.micaConfig.rest;

import java.io.IOException;
import java.security.KeyStoreException;
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

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalCredentialService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Opal;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;

import static java.util.stream.Collectors.toList;

@Component
@Path("/config")
public class MicaConfigResource {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private TaxonomyService taxonomyService;

  @Inject
  private OpalService opalService;

  @Inject
  private OpalCredentialService opalCredentialService;

  @Inject
  private KeyStoreService keyStoreService;

  @Inject
  private Dtos dtos;

  @GET
  @Timed
  @RequiresAuthentication
  public Mica.MicaConfigDto get() {
    return dtos.asDto(micaConfigService.getConfig());
  }

  @PUT
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.MicaConfigDto dto) {
    micaConfigService.save(dtos.fromDto(dto));
    return Response.noContent().build();
  }

  @PUT
  @Path("/keystore/{name}/{alias}")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateEncryptionKey(@PathParam("name") String name, @PathParam("alias") String alias,
    Mica.KeyForm keyForm) {
    if(keyForm.getKeyType() == Mica.KeyType.KEY_PAIR) {
      doCreateOrImportKeyPair(KeyStoreService.SYSTEM_KEY_STORE, "https", keyForm);
    } else {
      doImportCertificate(KeyStoreService.SYSTEM_KEY_STORE, "https", keyForm);
    }

    return Response.ok().build();
  }

  @GET
  @Path("/keystore/{name}/{alias}")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response getEncryptionKeyCertificate(@PathParam("name") String name, @PathParam("alias") String alias)
    throws IOException, KeyStoreException {

    if(!Sets.newHashSet(KeyStoreService.SYSTEM_KEY_STORE, OpalService.OPAL_KEYSTORE).contains(name)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if(keyStoreService.getKeyStore(name).aliasExists(alias)) {
      return Response.ok(keyStoreService.getPEMCertificate(name, alias), MediaType.TEXT_PLAIN_TYPE)
        .header("Content-disposition", String.format("attachment; filename=%s-%s-certificate.pem", name, alias))
        .build();
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }

  @GET
  @Path("/opal-credentials")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public List<Mica.OpalCredentialDto> getOpalCredentials(String opalUrl) {
    return opalCredentialService.findAllOpalCredentials().stream().map(dtos::asDto).collect(toList());
  }

  @GET
  @Path("/opal-credential/{id}")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Mica.OpalCredentialDto getOpalCredential(@PathParam("id") String opalUrl) {
    return dtos.asDto(opalCredentialService.getOpalCredential(opalUrl));
  }

  @GET
  @Path("/opal-credential/{id}/certificate")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response getOpalCredentialCertificate(@PathParam("id") String opalUrl) {
    return Response.ok(opalCredentialService.getCertificate(opalUrl), MediaType.TEXT_PLAIN_TYPE)
      .header("Content-disposition", "attachment; filename=opal-mica-certificate.pem").build();
  }

  @POST
  @Path("/opal-credentials")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response createCredential(Mica.OpalCredentialDto opalCredentialDto, @Context UriInfo uriInfo) {
    createOrUpdateCredential(opalCredentialDto);

    return Response
      .created(uriInfo.getBaseUriBuilder().segment("opal-credential", opalCredentialDto.getOpalUrl()).build()).build();
  }

  @PUT
  @Path("/opal-credential/{id}")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateOpalCredential(@PathParam("id") String id, Mica.OpalCredentialDto opalCredentialDto) {
    createOrUpdateCredential(opalCredentialDto);

    return Response.ok().build();
  }

  @DELETE
  @Path("/opal-credential/{id}")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateOpalCredential(@PathParam("id") String id) {
    opalCredentialService.deleteOpalCredential(id);

    return Response.ok().build();
  }

  private void createOrUpdateCredential(Mica.OpalCredentialDto opalCredentialDto) {
    if(opalCredentialDto.getType() == Mica.OpalCredentialType.USERNAME) {
      opalCredentialService
        .createOrUpdateOpalCredential(opalCredentialDto.getOpalUrl(), opalCredentialDto.getUsername(),
          opalCredentialDto.getPassword());
    } else {
      opalCredentialService.saveOrUpdateOpalCertificateCredential(opalCredentialDto.getOpalUrl());

      if(opalCredentialDto.getKeyForm().getKeyType() == Mica.KeyType.KEY_PAIR)
        doCreateOrImportKeyPair(OpalService.OPAL_KEYSTORE, opalCredentialDto.getOpalUrl(),
          opalCredentialDto.getKeyForm());
      else
        doImportCertificate(OpalService.OPAL_KEYSTORE, opalCredentialDto.getOpalUrl(), opalCredentialDto.getKeyForm());
    }
  }

  private void doImportCertificate(String name, String alias, Mica.KeyForm keyForm) {
    keyStoreService.createOrUpdateCertificate(name, alias, keyForm.getPublicImport());
  }

  private void doCreateOrImportKeyPair(String name, String alias, Mica.KeyForm keyForm) {
    if(keyForm.hasPrivateForm() && keyForm.hasPublicForm()) {
      Mica.PublicKeyForm pkForm = keyForm.getPublicForm();
      keyStoreService
        .createOrUpdateCertificate(name, alias, keyForm.getPrivateForm().getAlgo(), keyForm.getPrivateForm().getSize(),
          pkForm.getName(), pkForm.getOrganizationalUnit(), pkForm.getOrganization(), pkForm.getLocality(),
          pkForm.getState(), pkForm.getCountry());
    } else if(keyForm.hasPrivateImport()) {
      doImportKeyPair(name, alias, keyForm);
    } else {
      throw new WebApplicationException("Missing private key", Response.Status.BAD_REQUEST);
    }
  }

  private void doImportKeyPair(String name, String alias, Mica.KeyForm keyForm) {
    if(keyForm.hasPublicForm()) {
      Mica.PublicKeyForm pkForm = keyForm.getPublicForm();
      keyStoreService.createOrUpdateCertificate(name, alias, keyForm.getPrivateImport(), pkForm.getName(),
        pkForm.getOrganizationalUnit(), pkForm.getOrganization(), pkForm.getLocality(), pkForm.getState(),
        pkForm.getCountry());
    } else if(keyForm.hasPublicImport()) {
      keyStoreService.createOrUpdateCertificate(name, alias, keyForm.getPrivateImport(), keyForm.getPublicImport());
    } else {
      throw new WebApplicationException("Missing public key", Response.Status.BAD_REQUEST);
    }
  }

  @GET
  @Path("/languages")
  @Timed
  @RequiresAuthentication
  public Map<String, String> getAvailableLanguages() {
    //TODO support user locale (http://jira.obiba.org/jira/browse/MICASERVER-39)
    Locale locale = Locale.ENGLISH;
    return Arrays.asList(Locale.getISOLanguages()).stream()
      .collect(Collectors.toMap(lang -> lang, lang -> new Locale(lang).getDisplayLanguage(locale)));
  }

  @GET
  @Path("/taxonomies")
  @RequiresAuthentication
  @Deprecated
  public List<Opal.TaxonomyDto> getTaxonomies() {
    return opalService.getTaxonomyDtos();
  }

  @GET
  @Path("/taxonomies/summaries")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomiesDto getTaxonomySummaries() {
    return opalService.getTaxonomySummaryDtos();
  }

  @GET
  @Path("/taxonomies/vocabularies/summaries")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomiesDto getTaxonomyVocabularySummaries() {
    return opalService.getTaxonomyVocabularySummaryDtos();
  }

  @GET
  @Path("/studies")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomyDto getStudyTaxonomy() {
    return org.obiba.opal.web.taxonomy.Dtos.asDto(taxonomyService.getStudyTaxonomy());
  }
}
