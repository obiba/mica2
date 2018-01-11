/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.event;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.core.event.PersistableDeletedEvent;

public class DatasetDeletedEvent extends PersistableDeletedEvent<Dataset> {

  public DatasetDeletedEvent(Dataset persistable) {
    super(persistable);
  }

  public boolean isStudyDataset() {
    return getPersistable() instanceof StudyDataset;
  }
}
