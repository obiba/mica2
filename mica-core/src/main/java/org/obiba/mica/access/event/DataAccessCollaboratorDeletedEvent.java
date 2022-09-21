/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.event;

import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.event.PersistableUpdatedEvent;

public class DataAccessCollaboratorDeletedEvent extends PersistableUpdatedEvent<DataAccessCollaborator> {

  public DataAccessCollaboratorDeletedEvent(DataAccessCollaborator persistable) {
    super(persistable);
  }

}
