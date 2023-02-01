/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.tables;

public class StudyTableContext {

  private final IDataset dataset;

  private final IStudy study;

  private final int privacyThreshold;

  public StudyTableContext(IDataset dataset, IStudy study, int privacyThreshold) {
    this.dataset = dataset;
    this.study = study;
    this.privacyThreshold = privacyThreshold;
  }

  public IDataset getDataset() {
    return dataset;
  }

  public IStudy getStudy() {
    return study;
  }

  public int getPrivacyThreshold() {
    return privacyThreshold;
  }
}
