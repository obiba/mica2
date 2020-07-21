/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.event;

import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.event.PersistableCascadingPublishedEvent;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;

public class DatasetPublishedEvent extends PersistableCascadingPublishedEvent<Dataset> {

  private final Iterable<DatasetVariable> variables;

  private final Iterable<DatasetVariable> harmonizationVariables;

  private final String publisher;

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
    String publisher) {
    this(persistable, variables, publisher, PublishCascadingScope.NONE);
  }

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
    String publisher, PublishCascadingScope cascadingScope) {
    this(persistable, variables, null, publisher, cascadingScope);
  }

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
                               Iterable<DatasetVariable> harmonizationVariables, String publisher) {
    this(persistable, variables, harmonizationVariables, publisher, PublishCascadingScope.NONE);
  }

  public DatasetPublishedEvent(Dataset persistable, Iterable<DatasetVariable> variables,
                               Iterable<DatasetVariable> harmonizationVariables,
                               String publisher,
                               PublishCascadingScope cascadingScope) {
    super(persistable, cascadingScope);
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
    return harmonizationVariables != null && harmonizationVariables.iterator().hasNext();
  }

  public Iterable<DatasetVariable> getHarmonizationVariables() {
    return harmonizationVariables;
  }
}
