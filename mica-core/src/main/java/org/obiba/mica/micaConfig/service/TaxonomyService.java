/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

@Service
public class TaxonomyService {

  @Inject
  private OpalService opalService;

  @Inject
  private MicaConfigService micaConfigService;

  private Taxonomy variableTaxonomy;

  private Taxonomy datasetTaxonomy;

  private Taxonomy studyTaxonomy;

  private Taxonomy networkTaxonomy;

  @NotNull
  public Taxonomy getTaxonomyTaxonomy() {
    return micaConfigService.getTaxonomyTaxonomy();
  }

  @NotNull
  public Taxonomy getNetworkTaxonomy() {
    if(networkTaxonomy == null) {
      networkTaxonomy = new Taxonomy();
      BeanUtils.copyProperties(micaConfigService.getNetworkTaxonomy(), networkTaxonomy);
    }
    return networkTaxonomy;
  }

  @NotNull
  public Taxonomy getStudyTaxonomy() {
    if(studyTaxonomy == null) {
      studyTaxonomy = new Taxonomy();
      BeanUtils.copyProperties(micaConfigService.getStudyTaxonomy(), studyTaxonomy);
    }
    return studyTaxonomy;
  }

  @NotNull
  public Taxonomy getDatasetTaxonomy() {
    if(datasetTaxonomy == null) {
      datasetTaxonomy = new Taxonomy();
      BeanUtils.copyProperties(micaConfigService.getDatasetTaxonomy(), datasetTaxonomy);
    }
    return datasetTaxonomy;
  }

  @NotNull
  public Taxonomy getVariableTaxonomy() {
    if(variableTaxonomy == null) {
      variableTaxonomy = new Taxonomy();
      BeanUtils.copyProperties(micaConfigService.getVariableTaxonomy(), variableTaxonomy);
    }
    return variableTaxonomy;
  }

  @NotNull
  public List<Taxonomy> getVariableTaxonomies() {
    return Stream.concat(getOpalTaxonomies().stream(), Stream.of(getVariableTaxonomy()))
      .collect(Collectors.toList());
  }

  @NotNull
  public List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  //
  // Event handling
  //

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    networkTaxonomy = null; // id
    variableTaxonomy = null; // networkId
    datasetTaxonomy = null; // networkId
  }

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    networkTaxonomy = null; // id
    variableTaxonomy = null; // networkId
    datasetTaxonomy = null; // networkId
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    studyTaxonomy = null; // id
    networkTaxonomy = null; // studyIds
    variableTaxonomy = null; // studyIds, dceIds
    datasetTaxonomy = null; // studyId
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    studyTaxonomy = null; // id
    networkTaxonomy = null; // studyIds
    variableTaxonomy = null; // studyIds, dceIds
    datasetTaxonomy = null; // studyId
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    datasetTaxonomy = null; // id
    variableTaxonomy = null; // datasetId
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    datasetTaxonomy = null; // id
    variableTaxonomy = null; // datasetId
  }


}
