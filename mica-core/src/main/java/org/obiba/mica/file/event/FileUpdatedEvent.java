/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.file.AttachmentState;

public class FileUpdatedEvent extends PersistablePublishedEvent<AttachmentState> {

  public FileUpdatedEvent(AttachmentState state) {
    super(state);
  }
}
