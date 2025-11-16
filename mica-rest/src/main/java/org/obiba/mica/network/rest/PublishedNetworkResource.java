/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.rest;

import com.codahale.metrics.annotation.Timed;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * REST controller for managing Network.
 */
@Component
@Path("/network/{id}")
@Scope("request")
public class PublishedNetworkResource {

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private TaxonomiesService taxonomiesService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  @Inject
  private SubjectAclService subjectAclService;

  @GET
  @Timed
  public Mica.NetworkDto get(@PathParam("id") String id) {
    checkAccess(id);
    return dtos.asDto(getNetwork(id));
  }

  @GET
  @Path("/study-annotations")
  public Map<String, List<Taxonomy>> annotations(@PathParam("id") String id) {
    checkAccess(id);
    @NotNull
    final List<Taxonomy> variableTaxonomies = taxonomiesService.getVariableTaxonomies();
    final List<BaseStudy> networkStudies = publishedStudyService.findByIds(getNetwork(id).getStudyIds());

    Map<String, Map<String, Map<String, List<LocalizedString>>>> result = networkStudies.stream()
    .filter(study -> study.getClassName().equals("Study"))
    .collect(Collectors.toMap(
      study -> study.getId(),
      study -> study.getMergedAttributes().stream().collect(
        Collectors.groupingBy(
          Attribute::getNamespace,
          LinkedHashMap::new, Collectors.groupingBy(
            Attribute::getName,
            Collectors.mapping(attribute -> Optional.ofNullable(attribute.getValues()).orElse(new LocalizedString()), Collectors.toList())
          )
        )
      )
    ));

    return result.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> TaxonomiesService.processMergedAttributes(variableTaxonomies, entry.getValue())));
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("id") String id, @PathParam("fileId") String fileId) {
    checkAccess(id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Network network = getNetwork(id);

    if (network.getLogo() == null) throw NoSuchEntityException.withId(Attachment.class, fileId);

    fileResource.setAttachment(network.getLogo());

    return fileResource;
  }

  private void checkAccess(String id) {
    subjectAclService.checkAccess("/network", id);
  }

  private Network getNetwork(String id) {
    Network network = publishedNetworkService.findById(id);
    if (network == null) throw NoSuchNetworkException.withId(id);
    return network;
  }
}
