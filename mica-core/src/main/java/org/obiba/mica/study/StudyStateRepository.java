/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study;

import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.study.domain.StudyState;

/**
 * Spring Data MongoDB repository for the StudyState entity.
 */
public interface StudyStateRepository extends EntityStateRepository<StudyState> {
}
