/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import java.io.IOException;
import java.net.URISyntaxException;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.contact.event.IndexContactsEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.file.event.IndexFilesEvent;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalCredentialService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.model.Projects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.springframework.web.client.RestClientException;

import static java.util.stream.Collectors.toList;

@Component
@Path("/config")
public class MicaConfigResource {

  private static final Logger logger = LoggerFactory.getLogger(MicaConfigResource.class);

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

  @Inject
  private CacheService cacheService;

  @Inject
  private EventBus eventBus;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Timed
  @RequiresAuthentication
  public Mica.MicaConfigDto get() {
    return dtos.asDto(micaConfigService.getConfig());
  }

  @GET
  @Path("/_public")
  public Mica.PublicMicaConfigDto getPublic() {
    return dtos.asPublicDto(micaConfigService.getConfig());
  }

  @PUT
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.MicaConfigDto dto) {
    micaConfigService.save(dtos.fromDto(dto));
    return Response.noContent().build();
  }

  @GET
  @Path("/style.css")
  @Produces("text/css")
  public Response getStyle() {
    return Response
      .ok(micaConfigService.getConfig().getStyle(), "text/css")
      .header("Content-Disposition", "attachment; filename=\"style.css\"").build();
  }

  @GET
  @Path("/i18n/{locale}.json")
  @Produces("application/json")
  public Response getTranslation(@PathParam("locale") String locale, @QueryParam("default") boolean _default) throws IOException {

    String userProfileTranslations = getUserProfileTranslations(locale);
    String micaTranslations = micaConfigService.getTranslations(locale, _default);

    DocumentContext globalTranslations = JsonPath.parse(micaTranslations);
    globalTranslations.put("$", "userProfile", JsonPath.parse(userProfileTranslations).read("$"));

    return Response.ok(globalTranslations.jsonString(), "application/json").build();
  }

  @Path("/i18n/custom")
  public CustomTranslationsResource customTranslations() {
    return applicationContext.getBean(CustomTranslationsResource.class);
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
  @Path("/opal-projects")
  @Timed
  @RequiresAuthentication
  public List<Projects.ProjectDto> getOpalProjects() throws URISyntaxException {
    return opalService.getProjectDtos(null);
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

  @PUT
  @Path("/_index")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateIndices() {
    cacheService.clearOpalTaxonomiesCache();
    eventBus.post(new IndexStudiesEvent());
    eventBus.post(new IndexFilesEvent());
    eventBus.post(new IndexContactsEvent());
    eventBus.post(new IndexNetworksEvent());
    eventBus.post(new IndexDatasetsEvent());
    eventBus.post(new TaxonomiesUpdatedEvent());
    return Response.noContent().build();
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

  private String getUserProfileTranslations(String locale) {
    try {
      return userProfileService.getUserProfileTranslations(locale);
    } catch (RestClientException e) {
      logger.warn("Cannot get translations about userProfile (from agate)", e);
      return "{}";
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

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @GET
  @Path("/taxonomies")
  @RequiresAuthentication
  @Deprecated
  public List<Opal.TaxonomyDto> getTaxonomies() {
    return opalService.getTaxonomyDtos();
  }

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @GET
  @Path("/taxonomies/summaries")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomiesDto getTaxonomySummaries() {
    return opalService.getTaxonomySummaryDtos();
  }

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @GET
  @Path("/taxonomies/vocabularies/summaries")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomiesDto getTaxonomyVocabularySummaries() {
    return opalService.getTaxonomyVocabularySummaryDtos();
  }

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @GET
  @Path("/studies")
  @RequiresAuthentication
  @Deprecated
  public Opal.TaxonomyDto getStudyTaxonomy() {
    return org.obiba.opal.web.taxonomy.Dtos.asDto(taxonomyService.getStudyTaxonomy());
  }
}
