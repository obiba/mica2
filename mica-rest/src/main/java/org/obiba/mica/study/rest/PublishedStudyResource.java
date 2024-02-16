/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Study.
 */
@Component
@Path("/study/{id}")
@Scope("request")
public class PublishedStudyResource extends AbstractPublishedStudyResource {

  @Inject
  private TaxonomiesService taxonomiesService;

  @GET
  @Timed
  public Mica.StudyDto getStudyDto(@PathParam("id") String id, @QueryParam("locale") String locale) {
    checkAccess(id);
    BaseStudy study = getStudy(id, locale);
    return study instanceof Study ? dtos.asDto((Study)study) : dtos.asDto((HarmonizationStudy)study);
  }

  @GET
  @Path("/study-annotations")
  public List<Taxonomy> annotations(@PathParam("id") String id, @QueryParam("locale") String locale) {
    checkAccess(id);
    BaseStudy study = getStudy(id, locale);
    final List<Taxonomy> variableTaxonomies = taxonomiesService.getVariableTaxonomies();

    LinkedHashMap<String, Map<String, List<LocalizedString>>> result = study.getMergedAttributes().stream().collect(
      Collectors.groupingBy(
        Attribute::getNamespace,
        LinkedHashMap::new, Collectors.groupingBy(
          Attribute::getName,
          Collectors.mapping(attribute -> Optional.ofNullable(attribute.getValues()).orElse(new LocalizedString()), Collectors.toList())
        )
      )
    );

    return TaxonomiesService.processMergedAttributes(variableTaxonomies, result);
  }


  @Path("/file/{fileId}")
  public FileResource study(@PathParam("id") String id, @PathParam("fileId") String fileId) {
    return super.getStudyFileResource(id, fileId);
  }
}
