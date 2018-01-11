/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Import a zip file that was exported from Old Mica.
 */
public interface StudyPackageImportService {

  /**
   * Import a study package: read protobuf messages in json of {@link org.obiba.mica.study.domain.Study},
   * {@link org.obiba.mica.network.domain.Network}, {@link org.obiba.mica.dataset.domain.Dataset} and
   * raw byte stream of {@link org.obiba.mica.file.Attachment}.
   * @param inputStream
   * @param publish
   * @throws IOException
   */
  void importZip(InputStream inputStream, boolean publish) throws IOException;

}
