/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.contact.event.IndexContactsEvent;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.service.KeyStoreService;
import org.obiba.mica.file.event.IndexFilesEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.*;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
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
import org.springframework.web.client.RestClientException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.util.*;
import java.util.stream.Collectors;

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

  @Inject
  private MicaMetricsService micaMetricsService;

  @GET
  @Timed
  //@RequiresAuthentication
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
    MicaConfig micaConfig = dtos.fromDto(dto);
    taxonomyService.refreshTaxonomyTaxonomyIfNeeded(micaConfigService.getConfig(), micaConfig);
    micaConfigService.save(micaConfig);
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
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJsonTranslations(@PathParam("locale") String locale, @QueryParam("default") boolean _default) throws IOException {
    StreamingOutput stream = os -> {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(os))) {
        writer.write(getGlobalTranslationsAsJson(locale, _default));
        writer.flush();
      }
    };
    return Response.ok(stream).build();
  }

  @GET
  @Path("/i18n/{locale}.po")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getGettextTranslations(@PathParam("locale") String locale, @QueryParam("default") boolean _default) throws IOException {
    StreamingOutput stream = os -> {
      try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {
        Properties properties = getGlobalTranslationsAsProperties(locale, _default);
        writer.println("# Translations extracted from Mica");
        writer.println("msgid \"\"");
        writer.println("msgstr \"\"");
        writer.println(String.format("\"Project-Id-Version: Mica %s\\n\"", micaConfigService.getConfig().getMicaVersion()));
        writer.println(String.format("\"PO-Revision-Date: %s\\n\"", new Date()));
        writer.println("\"MIME-Version: 1.0\\n\"");
        writer.println("\"Content-Type: text/plain; charset=UTF-8\\n\"");
        writer.println("\"Content-Transfer-Encoding: 8bit\\n\"");
        writer.println(String.format("\"Language: %s\\n\"", locale));
        writer.println();
        properties.keySet().stream().sorted().forEach(key -> {
          writer.println(String.format("msgid \"%s\"", key));
          String value = properties.getProperty(key.toString());
          if (!Strings.isNullOrEmpty(value)) {
            value = value.replaceAll("\\{\\{([\\w]+)\\}\\}", "@$1");
          }
          writer.println(String.format("msgstr \"%s\"", value));
          writer.println();
        });
        writer.flush();
      }
    };
    return Response.ok(stream).build();
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

  @Path("/document-sets/permissions")
  public SubjectAclResource documentSetsPermissionsResource() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/set/documents", "_opal");
    return subjectAclResource;
  }

  @Path("/contingencies/permissions")
  public SubjectAclResource crosstabsPermissionsResource() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/analyses/contingencies", "_opal");
    return subjectAclResource;
  }

  @GET
  @Path("/opal-projects")
  @Timed
  @RequiresAuthentication
  public List<Projects.ProjectDto> getOpalProjects() throws URISyntaxException {
    try {
      return opalService.getProjectDtos(null);
    } catch (Exception e) {
      logger.warn("Failed at retrieving opal projects: {}", e.getMessage());
      return Lists.newArrayList();
    }
  }

  @GET
  @Path("/opal-credentials")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public List<Mica.OpalCredentialDto> getOpalCredentials() {
    return opalCredentialService.findAllOpalCredentials().stream().map(dtos::asDto).collect(toList());
  }

  @GET
  @Path("/opal-credential")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Mica.OpalCredentialDto getOpalCredential(@QueryParam("id") String opalUrl) {
    return dtos.asDto(opalCredentialService.getOpalCredential(opalUrl));
  }

  @GET
  @Path("/opal-credential/certificate")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response getOpalCredentialCertificate(@QueryParam("id") String opalUrl) {
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
  @Path("/opal-credential")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateOpalCredential(@QueryParam("id") String id, Mica.OpalCredentialDto opalCredentialDto) {
    createOrUpdateCredential(opalCredentialDto);

    return Response.ok().build();
  }

  @DELETE
  @Path("/opal-credential")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateOpalCredential(@QueryParam("id") String id) {
    opalCredentialService.deleteOpalCredential(id);

    return Response.ok().build();
  }

  @PUT
  @Path("/_index")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateIndices() {
    eventBus.post(new IndexStudiesEvent());
    eventBus.post(new IndexFilesEvent());
    eventBus.post(new IndexContactsEvent());
    eventBus.post(new IndexNetworksEvent());
    eventBus.post(new IndexDatasetsEvent());
    eventBus.post(new IndexProjectsEvent());
    eventBus.post(new TaxonomiesUpdatedEvent());
    return Response.noContent().build();
  }

  @GET
  @Path("/metrics")
  @RequiresAuthentication
  public Mica.MicaMetricsDto getMetrics() {
    MicaConfig config = micaConfigService.getConfig();
    Mica.MicaMetricsDto.Builder builder = Mica.MicaMetricsDto.newBuilder();

    if (config.isRepositoryEnabled()) {
      // Network
      if (config.isNetworkEnabled()) {
        builder.addDocuments(
          Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
            .setType(Network.class.getSimpleName())
            .addAllProperties(
              micaMetricsService.getNetworksStateCount()
                .entrySet()
                .stream()
                .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                  .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
                .collect(Collectors.toList())
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("indexed")
                .setValue(micaMetricsService.getPublishedNetworksCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("totalFiles")
                .setValue(micaMetricsService.getDraftNetworkFilesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("publishedFiles")
                .setValue(micaMetricsService.getPublishedNetworkFilesCount())
                .build()
            )
        );
      }

      // Individual Study
      builder.addDocuments(Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
        .setType(Study.class.getSimpleName())
        .addAllProperties(
          micaMetricsService.getIndividualStudiesStateCount()
            .entrySet()
            .stream()
            .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
              .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
            .collect(Collectors.toList())
        )
        .addProperties(
          Mica.MicaMetricsDto.PropertyDto.newBuilder()
            .setName("indexed")
            .setValue(micaMetricsService.getPublishedIndividualStudiesCount())
            .build()
        )
        .addProperties(
          Mica.MicaMetricsDto.PropertyDto.newBuilder()
            .setName("variables")
            .setValue(micaMetricsService.getPublishedIndividualStudiesVariablesCount())
            .build()
        )
        .addProperties(
          Mica.MicaMetricsDto.PropertyDto.newBuilder()
            .setName("totalWithVariable")
            .setValue(micaMetricsService.getPublishedIndividualStudiesWithVariablesCount())
            .build()
        )
        .addProperties(
          Mica.MicaMetricsDto.PropertyDto.newBuilder()
            .setName("totalFiles")
            .setValue(micaMetricsService.getDraftIndividualStudiesFilesCount())
            .build()
        )
        .addProperties(
          Mica.MicaMetricsDto.PropertyDto.newBuilder()
            .setName("publishedFiles")
            .setValue(micaMetricsService.getPublishedIndividualStudiesFilesCount())
            .build()
        )
      );

      // Harmonization Study
      if (config.isHarmonizationDatasetEnabled()) {
        builder.addDocuments(
          Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
            .setType(HarmonizationStudy.class.getSimpleName())
            .addAllProperties(
              micaMetricsService.getHarmonizationStudiesStateCount()
                .entrySet()
                .stream()
                .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                  .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
                .collect(Collectors.toList())
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("indexed")
                .setValue(micaMetricsService.getPublishedHarmonizationStudiesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("variables")
                .setValue(micaMetricsService.getPublishedHarmonizationStudiesVariablesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("totalFiles")
                .setValue(micaMetricsService.getDraftHarmonizationStudiesFilesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("publishedFiles")
                .setValue(micaMetricsService.getDraftHarmonizationStudiesFilesCount())
                .build()
            )
        );
      }

      // StudyDataset
      if (config.isStudyDatasetEnabled()) {
        builder.addDocuments(
          Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
            .setType(StudyDataset.class.getSimpleName())
            .addAllProperties(
              micaMetricsService.getStudyDatasetsStateCount()
                .entrySet()
                .stream()
                .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                  .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
                .collect(Collectors.toList())
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("indexed")
                .setValue(micaMetricsService.getPublishedStudyDatasetsCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("totalFiles")
                .setValue(micaMetricsService.getDraftStudyDatasetFilesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("publishedFiles")
                .setValue(micaMetricsService.getPublishedStudyDatasetFilesCount())
                .build()
            )
        );
      }

      // HarmonizationDataset
      if (config.isHarmonizationDatasetEnabled()) {
        builder.addDocuments(
          Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
            .setType(HarmonizationDataset.class.getSimpleName())
            .addAllProperties(
              micaMetricsService.getHarmonizationDatasetsStateCount()
                .entrySet()
                .stream()
                .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                  .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
                .collect(Collectors.toList())
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("indexed")
                .setValue(micaMetricsService.getPublishedHarmonizationDatasetsCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("totalFiles")
                .setValue(micaMetricsService.getDraftHarmonizationDatasetFilesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("publishedFiles")
                .setValue(micaMetricsService.getPublishedHarmonizationDatasetFilesCount())
                .build()
            )
        );
      }

      // Variables
      if (config.isVariablesCountEnabled()) {
        builder.addDocuments(
          Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
            .setType(DatasetVariable.class.getSimpleName())
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("published")
                .setValue(micaMetricsService.getPublishedVariablesCount())
                .build()
            )
            .addProperties(
              Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName("harmonized")
                .setValue(micaMetricsService.getHarmonizedVariablesCount())
                .build()
            )
        );
      }
    }

    // Projects
    if (config.isProjectsCountEnabled()) {
      builder.addDocuments(
        Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
          .setType(Project.class.getSimpleName())
          .addAllProperties(
            micaMetricsService.getProjectsStateCount()
              .entrySet()
              .stream()
              .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
              .collect(Collectors.toList())
          )
          .addProperties(
            Mica.MicaMetricsDto.PropertyDto.newBuilder()
              .setName("indexed")
              .setValue(micaMetricsService.getPublishedProjectsCount())
              .build()
          )
          .addProperties(
            Mica.MicaMetricsDto.PropertyDto.newBuilder()
              .setName("totalFiles")
              .setValue(micaMetricsService.getDraftProjectFilesCount())
              .build()
          )
          .addProperties(
            Mica.MicaMetricsDto.PropertyDto.newBuilder()
              .setName("publishedFiles")
              .setValue(micaMetricsService.getPublishedProjectFilesCount())
              .build()
          )
      );
    }

    // Data Access Requests
    if (config.isDataAccessRequestCountEnabled()) {
      builder.addDocuments(
        Mica.MicaMetricsDto.DocumentMetricsDto.newBuilder()
          .setType(DataAccessRequest.class.getSimpleName())
          .addAllProperties(
            micaMetricsService.getDataAccessRequestsStateCount()
              .entrySet()
              .stream()
              .map(entry -> Mica.MicaMetricsDto.PropertyDto.newBuilder()
                .setName(entry.getKey()).setValue(Long.parseLong(entry.getValue().toString())).build())
              .collect(Collectors.toList())
          )
      );
    }

    return builder.build();
  }

  private DocumentContext getGlobalTranslations(String locale, boolean _default) throws IOException {
    String userProfileTranslations = getUserProfileTranslations(locale);
    String micaTranslations = micaConfigService.getTranslations(locale, _default);

    DocumentContext globalTranslations = JsonPath.parse(micaTranslations);
    globalTranslations.put("$", "userProfile", JsonPath.parse(userProfileTranslations).read("$"));

    return globalTranslations;
  }

  private String getGlobalTranslationsAsJson(String locale, boolean _default) throws IOException {
    return getGlobalTranslations(locale, _default).jsonString();
  }

  private Properties getGlobalTranslationsAsProperties(String locale, boolean _default) throws IOException {
    return JSONUtils.toProperties(getGlobalTranslationsAsJson(locale, _default));
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
      logger.warn("Cannot get translations about userProfile (from agate): {}", e.getMessage());
      return "{}";
    }
  }

  @GET
  @Path("/languages")
  @Timed
  @RequiresAuthentication
  public Map<String, String> getAvailableLanguages(@QueryParam("locale") @DefaultValue("en") String languageTag) {
    Locale locale = Locale.forLanguageTag(languageTag);
    return Arrays.stream(Locale.getISOLanguages())
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
