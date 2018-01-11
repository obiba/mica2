/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.domain;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.LocalizedString;

public class ProjectState extends EntityState {

  @Override
  public String pathPrefix() {
    return "projects";
  }

  private LocalizedString title;

  public LocalizedString getTitle() {
    return title;
  }

  public void setTitle(LocalizedString title) {
    this.title = title;
  }
}
