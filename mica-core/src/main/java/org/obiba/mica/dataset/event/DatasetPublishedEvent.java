/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.event;

import java.util.List;
import java.util.Map;

import org.obiba.mica.core.event.PersistableUpdatedEvent;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;

public class DatasetPublishedEvent extends PersistableUpdatedEvent<Dataset> {

  private final Iterable<DatasetVariable> variables;

  private final Map<String, List<DatasetVariable>> harmonizationVariables;

  private final String publisher;

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
    String publisher) {
    this(persistable, variables, null, publisher);
  }

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizationVariables, String publisher) {
    super(persistable);
    this.publisher = publisher;
    this.variables = variables;
    this.harmonizationVariables = harmonizationVariables;
  }

  public String getPublisher() {
    return publisher;
  }

  public Iterable<DatasetVariable> getVariables() {
    return variables;
  }

  public boolean hasHarmonizationVariables() {
    return harmonizationVariables != null && !harmonizationVariables.isEmpty();
  }

  public Map<String, List<DatasetVariable>> getHarmonizationVariables() {
    return harmonizationVariables;
  }
}
