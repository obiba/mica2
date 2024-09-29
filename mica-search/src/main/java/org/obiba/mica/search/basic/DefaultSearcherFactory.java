/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;

import org.obiba.mica.study.StudyRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DefaultSearcherFactory {

  @Inject
  private StudyRepository studyRepository;

  public DefaultSearcher newSearcher() {
    DefaultSearcher searcher = new DefaultSearcher();
    searcher.setStudyRepository(studyRepository);
    return searcher;
  }
}
