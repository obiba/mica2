/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.Map;


public abstract class AbstractModelAware extends AbstractGitPersistable implements ModelAware{

  private Map<String, Object> model;

  public boolean hasModel() {
    return model != null;
  }

  public void setModel(Map<String, Object> model) {
    this.model = model;
  }

  public Map<String, Object> getModel() {
    return model;
  }
}
