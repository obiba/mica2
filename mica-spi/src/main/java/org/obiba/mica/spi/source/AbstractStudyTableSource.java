/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.source;

/**
 * Helper class for implementing  {@link StudyTableSource}.
 */
public abstract class AbstractStudyTableSource implements StudyTableSource {

  private StudyTableContext context;

  @Override
  public void setStudyTableContext(StudyTableContext context) {
    this.context = context;
  }

  protected StudyTableContext getContext() {
    return context;
  }

}
