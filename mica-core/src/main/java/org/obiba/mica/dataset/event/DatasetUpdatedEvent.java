/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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

public class DatasetUpdatedEvent extends PersistableUpdatedEvent<Dataset> {

  private final Iterable<DatasetVariable> variables;

  private final Map<String, List<DatasetVariable>> harmonizationVariables;

  public DatasetUpdatedEvent(Dataset persistable, Iterable<DatasetVariable> variables) {
    this(persistable, variables, null);
  }

  public DatasetUpdatedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizationVariables) {
    super(persistable);
    this.variables = variables;
    this.harmonizationVariables = harmonizationVariables;
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
