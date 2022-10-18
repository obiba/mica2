/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.obiba.mica.file.FileRuntimeException;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Component;

import com.mongodb.client.gridfs.model.GridFSFile;

@Component
public class GridFsService implements FileStoreService {

  @Inject
  private GridFsOperations gridFsOperations;

  @Inject
  private TempFileService tempFileService;

  @Override
  public InputStream getFile(String id) throws FileRuntimeException {
    GridFSFile f = gridFsOperations.findOne(new Query().addCriteria(Criteria.where("filename").is(id)));

    if(f == null)
      throw new FileRuntimeException(id);

    try {
      return gridFsOperations.getResource(f).getInputStream();
    } catch (IllegalStateException e) {
      throw new FileRuntimeException(id);
    } catch (IOException e) {
      throw new FileRuntimeException(id);
    }
  }

  @Override
  public void save(String id, InputStream input) {
    gridFsOperations.store(input, id);
  }

  @Override
  public void save(String tempFileId) {
    save(tempFileId, tempFileService.getInputStreamFromFile(tempFileId));
    tempFileService.delete(tempFileId);
  }

  @Override
  public void delete(String id) {
    gridFsOperations.delete(new Query().addCriteria(Criteria.where("filename").is(id)));
  }
}
