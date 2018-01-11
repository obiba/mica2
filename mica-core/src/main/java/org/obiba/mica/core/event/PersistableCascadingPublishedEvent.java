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

import org.obiba.mica.core.domain.PublishCascadingScope;
import org.springframework.data.domain.Persistable;

public class PersistableCascadingPublishedEvent<TPersistable extends Persistable<?>>
  extends PersistablePublishedEvent<TPersistable> {

  private final PublishCascadingScope cascadingScope;

  public PersistableCascadingPublishedEvent(TPersistable persistable, PublishCascadingScope cascadingScope) {
    super(persistable);
    this.cascadingScope = cascadingScope;
  }

  public PublishCascadingScope getCascadingScope() {
    return cascadingScope;
  }
}
