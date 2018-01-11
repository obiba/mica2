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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

@Component
public class DeadEventSubscriber {

  private static final Logger log = LoggerFactory.getLogger(DeadEventSubscriber.class);

  @Subscribe
  public void handleDeadEvent(DeadEvent deadEvent) {
    log.error("Event {} from source {} is not handled", deadEvent.getEvent(), deadEvent.getSource());
  }

}
