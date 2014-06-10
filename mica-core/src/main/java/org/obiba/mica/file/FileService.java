package org.obiba.mica.file;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.service.GitService;
import org.springframework.stereotype.Component;

@Component
public class FileService {

  @Inject
  private GitService gitService;

  public byte[] getContent(@NotNull String gitPersistableId, @NotNull String fileId) {
    return gitService.readFileHead(gitPersistableId, fileId);
  }

}
