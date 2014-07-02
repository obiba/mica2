/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.event;

import java.util.List;

import org.obiba.mica.study.domain.Study;

public class IndexStudiesEvent {

  private final List<Study> publishedStudies;

  private final List<Study> draftStudies;

  public IndexStudiesEvent(List<Study> publishedStudies, List<Study> draftStudies) {
    this.publishedStudies = publishedStudies;
    this.draftStudies = draftStudies;
  }

  public List<Study> getPublishedStudies() {
    return publishedStudies;
  }

  public List<Study> getDraftStudies() {
    return draftStudies;
  }
}
