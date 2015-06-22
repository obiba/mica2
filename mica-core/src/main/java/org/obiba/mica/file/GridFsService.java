package org.obiba.mica.file;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.stereotype.Component;

import com.mongodb.gridfs.GridFSDBFile;

@Component
public class GridFsService {

  @Inject
  private GridFsOperations gridFsOperations;

  @Inject
  private TempFileService tempFileService;

  public InputStream getFile(String id) throws IOException {
    GridFSDBFile f = gridFsOperations.findOne(new Query().addCriteria(Criteria.where("filename").is(id)));

    if(f == null)
      throw new GridFSFileNotFoundException(id);

    return f.getInputStream();
  }

  public void save(InputStream input, String id) {
    gridFsOperations.store(input, id);
  }

  public void save(String tempFileId) {
    save(tempFileService.getInputStreamFromFile(tempFileId), tempFileId);
  }

  public void delete(String id) {
    gridFsOperations.delete(new Query().addCriteria(Criteria.where("filename").is(id)));
  }
}
