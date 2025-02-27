/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.lang.Assert;

@Component
public class StudyIdGeneratorService {

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  @NotNull
  public String generateId(@NotNull LocalizedString acronym) {
    Assert.notNull(acronym, "Acronym cannot be null.");
    return getNextId(acronym.asUrlSafeString().toLowerCase(), 0);
  }

  private String getNextId(String prefix, int count) {
    String id = prefix + (count > 0 ? "-" + count : "");
    if (!studyStateRepository.existsById(id) && !harmonizationStudyStateRepository.existsById(id)) {
      return id;
    }

    return count < 1000 ? getNextId(prefix, ++count) : null;
  }
}
