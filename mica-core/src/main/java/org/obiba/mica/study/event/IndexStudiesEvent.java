/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.event;

import org.obiba.mica.core.event.IndexPersistablesEvent;
import org.obiba.mica.study.domain.BaseStudy;

import java.util.List;

public class IndexStudiesEvent extends IndexPersistablesEvent<BaseStudy> {

  public IndexStudiesEvent() {
    super();
  }

  public IndexStudiesEvent(List<String> ids) {
    super(ids);
  }

}
