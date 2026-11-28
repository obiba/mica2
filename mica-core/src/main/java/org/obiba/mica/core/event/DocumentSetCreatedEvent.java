/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.event;

import org.obiba.mica.core.domain.DocumentSet;

/**
 * A document set was created. It is posted in addition to the {@link DocumentSetUpdatedEvent} that reports the
 * identifiers of the new set.
 */
public class DocumentSetCreatedEvent extends PersistableUpdatedEvent<DocumentSet> {

  public DocumentSetCreatedEvent(DocumentSet documentSet) {
    super(documentSet);
  }
}
