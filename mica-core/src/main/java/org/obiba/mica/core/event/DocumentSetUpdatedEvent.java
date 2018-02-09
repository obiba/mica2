/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.event;

import org.obiba.mica.core.domain.DocumentSet;

import java.util.Set;

public class DocumentSetUpdatedEvent extends PersistableUpdatedEvent<DocumentSet> {

  private final Set<String> removedIdentifiers;

  public DocumentSetUpdatedEvent(DocumentSet documentSet, Set<String> removedIdentifiers) {
    super(documentSet);
    this.removedIdentifiers = removedIdentifiers;
  }

  public boolean hasRemovedIdentifiers() {
    return removedIdentifiers != null && !removedIdentifiers.isEmpty();
  }

  public Set<String> getRemovedIdentifiers() {
    return removedIdentifiers;
  }
}
