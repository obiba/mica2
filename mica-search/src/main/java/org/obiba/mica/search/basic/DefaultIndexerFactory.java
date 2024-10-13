/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DefaultIndexerFactory {

  @Inject
  private Set<DocumentIndexer> documentIndexers;

  public DefaultIndexer newIndexer() {
    return new DefaultIndexer(documentIndexers);
  }
}
