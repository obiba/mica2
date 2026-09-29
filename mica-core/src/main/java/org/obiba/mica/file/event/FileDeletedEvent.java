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

import org.obiba.mica.core.event.PersistableDeletedEvent;
import org.obiba.mica.file.AttachmentState;

public class FileDeletedEvent extends PersistableDeletedEvent<AttachmentState> {

  private final boolean inFolderDelete;

  public FileDeletedEvent(AttachmentState state) {
    this(state, false);
  }

  /**
   * @param state
   * @param inFolderDelete the file is deleted with its folder, which posts a {@link FolderDeletedEvent} when done
   */
  public FileDeletedEvent(AttachmentState state, boolean inFolderDelete) {
    super(state);
    this.inFolderDelete = inFolderDelete;
  }

  public boolean isInFolderDelete() {
    return inFolderDelete;
  }
}
