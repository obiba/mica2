/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.event.DataAccessConfigUpdatedEvent;
import org.obiba.mica.micaConfig.repository.DataAccessConfigRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Service
@Validated
public class DataAccessConfigService {

  private DataAccessConfigRepository dataAccessConfigRepository;

  private EventBus eventBus;

  @Inject
  public DataAccessConfigService(
    DataAccessConfigRepository dataAccessConfigRepository,
    EventBus eventBus) {
    this.dataAccessConfigRepository = dataAccessConfigRepository;
    this.eventBus = eventBus;
  }

  public synchronized DataAccessConfig getOrCreateConfig() {
    if (dataAccessConfigRepository.count() == 0) {
      DataAccessConfig config = new DataAccessConfig();
      dataAccessConfigRepository.save(config);
    }
    DataAccessConfig config = dataAccessConfigRepository.findAll().get(0);
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
