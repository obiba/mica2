package org.obiba.mica.file;

import java.io.IOException;
import java.util.Collection;

import org.obiba.git.command.AddDeleteFilesCommand;
import org.obiba.mica.domain.AbstractGitPersistable;
import org.springframework.data.domain.Persistable;

public interface AttachmentSerializer<TGitPersistable extends AbstractGitPersistable> {

  void serializeAttachments(TGitPersistable t, Collection<String> existingPathsInRepo,
      AddDeleteFilesCommand.Builder builder) throws IOException;

  default String getPathInRepo(String filename, Persistable<String> parent) {
    return "attachments/" + parent.getClass().getSimpleName() + "-" + parent.getId() + "/" + filename;
  }

}
