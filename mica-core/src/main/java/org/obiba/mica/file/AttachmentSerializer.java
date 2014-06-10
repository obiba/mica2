package org.obiba.mica.file;

import java.io.IOException;
import java.util.Collection;

import org.obiba.git.command.AddDeleteFilesCommand;
import org.obiba.mica.domain.AbstractGitPersistable;

public interface AttachmentSerializer<TGitPersistable extends AbstractGitPersistable> {

  void serializeAttachments(TGitPersistable t, Collection<String> existingPathsInRepo,
      AddDeleteFilesCommand.Builder builder) throws IOException;

}
