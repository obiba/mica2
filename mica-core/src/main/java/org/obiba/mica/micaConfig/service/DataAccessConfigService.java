/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.event.DataAccessConfigUpdatedEvent;
import org.obiba.mica.micaConfig.repository.DataAccessConfigRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Service
@Validated
public class DataAccessConfigService {

  private final DataAccessConfigRepository dataAccessConfigRepository;

  private final DataAccessFormService dataAccessFormService;

  private final DataAccessPreliminaryFormService dataAccessPreliminaryFormService;

  private final DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  private final DataAccessAmendmentFormService dataAccessAmendmentFormService;

  private final EventBus eventBus;

  @Inject
  public DataAccessConfigService(
    DataAccessConfigRepository dataAccessConfigRepository,
    DataAccessFormService dataAccessFormService,
    DataAccessPreliminaryFormService dataAccessPreliminaryFormService,
    DataAccessFeasibilityFormService dataAccessFeasibilityFormService,
    DataAccessAmendmentFormService dataAccessAmendmentFormService,
    EventBus eventBus) {
    this.dataAccessConfigRepository = dataAccessConfigRepository;
    this.dataAccessFormService = dataAccessFormService;
    this.dataAccessPreliminaryFormService = dataAccessPreliminaryFormService;
    this.dataAccessFeasibilityFormService = dataAccessFeasibilityFormService;
    this.dataAccessAmendmentFormService = dataAccessAmendmentFormService;
    this.eventBus = eventBus;
  }

  public synchronized DataAccessConfig getOrCreateConfig() {
    if (dataAccessConfigRepository.count() == 0) {
      DataAccessConfig config = new DataAccessConfig();
      dataAccessConfigRepository.save(config);
    }
    DataAccessConfig config = dataAccessConfigRepository.findAll().get(0);
    boolean modified = false;
    if (Strings.isNullOrEmpty(config.getCsvExportFormat())) {
      config.setCsvExportFormat(dataAccessFormService.getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      modified = true;
    }
    if (Strings.isNullOrEmpty(config.getPreliminaryCsvExportFormat())) {
      config.setPreliminaryCsvExportFormat(dataAccessPreliminaryFormService.getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      modified = true;
    }
    if (Strings.isNullOrEmpty(config.getFeasibilityCsvExportFormat())) {
      config.setFeasibilityCsvExportFormat(dataAccessFeasibilityFormService.getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      modified = true;
    }
    if (Strings.isNullOrEmpty(config.getAmendmentCsvExportFormat())) {
      config.setAmendmentCsvExportFormat(dataAccessAmendmentFormService.getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      modified = true;
    }
    if (modified) {
      dataAccessConfigRepository.save(config);
    }
    return config;
  }

  public void save(@NotNull @Valid DataAccessConfig config) {
    DataAccessConfig savedConfig = getOrCreateConfig();

    BeanUtils.copyProperties(config, savedConfig, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
      "lastModifiedDate");

    dataAccessConfigRepository.save(savedConfig);
    eventBus.post(new DataAccessConfigUpdatedEvent(savedConfig));
  }

}
