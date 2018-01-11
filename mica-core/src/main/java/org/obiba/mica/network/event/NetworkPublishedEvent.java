/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.event;

import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.event.PersistableCascadingPublishedEvent;
import org.obiba.mica.network.domain.Network;

public class NetworkPublishedEvent extends PersistableCascadingPublishedEvent<Network> {

  private final String publisher;

  public NetworkPublishedEvent(Network persistable, String publisher) {
    super(persistable, PublishCascadingScope.NONE);
    this.publisher = publisher;
  }

  public NetworkPublishedEvent(Network persistable, String publisher, PublishCascadingScope cascadingScope) {
    super(persistable, cascadingScope);
    this.publisher = publisher;
  }

  public String getPublisher() {
    return publisher;
  }
}
