/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.event;

import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.event.PersistableCascadingPublishedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyPublishedEvent extends PersistableCascadingPublishedEvent<Study> {

  private final String publisher;

  public StudyPublishedEvent(Study study, String publisher) {
    this(study, publisher, PublishCascadingScope.NONE);
  }

  public StudyPublishedEvent(Study study, String publisher, PublishCascadingScope cascadingScope) {
    super(study, cascadingScope);
    this.publisher = publisher;
  }

  public String getPublisher() {
    return publisher;
  }
}
