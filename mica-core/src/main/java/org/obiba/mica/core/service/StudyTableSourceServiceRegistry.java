/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import org.obiba.mica.core.source.OpalTableSource;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.spi.source.StudyTableSource;
import org.obiba.mica.spi.source.StudyTableSourceService;
import org.obiba.mica.study.domain.BaseStudy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class StudyTableSourceServiceRegistry {

  @Inject
  private PluginsService pluginsService;

  @Inject
  private OpalService opalService;

  public StudyTableSource makeStudyTableSource(BaseStudy study, String source) {
    if (OpalTableSource.isFor(source)) {
      OpalTableSource tableSource = OpalTableSource.fromURN(source);
      tableSource.init(opalService, study.getOpal());
      return tableSource;
    }
    Optional<StudyTableSourceService> serviceOptional = pluginsService.getStudyTableSourceServices().stream()
      .filter(service -> service.isFor(source)).findFirst();
    if (serviceOptional.isPresent()) {
      // TODO add a context to the study table source
      return serviceOptional.get().makeSource(source);
    }
    throw new NoSuchElementException("Missing study-table-source plugin to handle source: " + source);
  }

}
