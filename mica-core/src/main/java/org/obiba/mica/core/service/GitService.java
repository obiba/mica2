/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.util.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.PushResult;
import org.obiba.core.util.FileUtil;
import org.obiba.git.CommitInfo;
import org.obiba.git.GitException;
import org.obiba.git.GitUtils;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.git.command.AddDeleteFilesCommand;
import org.obiba.git.command.CommitLogCommand;
import org.obiba.git.command.DiffAsStringCommand;
import org.obiba.git.command.FetchBlobCommand;
import org.obiba.git.command.GitCommandHandler;
import org.obiba.git.command.LogsCommand;
import org.obiba.git.command.ReadFileCommand;
import org.obiba.mica.core.domain.GitIdentifier;
import org.obiba.mica.core.domain.GitPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;

@Component
@Validated
public class GitService implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(GitService.class);

  private static final String PATH_DATA = "${MICA_HOME}/data/git";

  private static final String PATH_CLONES = "${MICA_HOME}/work/git";

  private static final String ATTACHMENTS_PATH = "attachments/";

  @Inject
  private GitCommandHandler gitCommandHandler;

  @Inject
  private ObjectMapper objectMapper;

  private File repositoriesRoot;

  private File clonesRoot;

  @Override
  public void afterPropertiesSet() throws Exception {
    if(repositoriesRoot == null) {
      repositoriesRoot = new File(PATH_DATA.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
    if(clonesRoot == null) {
      clonesRoot = new File(PATH_CLONES.replace("${MICA_HOME}", System.getProperty("MICA_HOME")));
    }
  }

  @VisibleForTesting
  public void setRepositoriesRoot(File repositoriesRoot) {
    this.repositoriesRoot = repositoriesRoot;
  }

  @VisibleForTesting
  public void setClonesRoot(File clonesRoot) {
    this.clonesRoot = clonesRoot;
  }

  public boolean hasGitRepository(GitIdentifier persistable) {
    return getRepositoryPath(persistable).exists() || getCloneRepositoryPath(persistable).exists();
  }

  public void deleteGitRepository(GitPersistable persistable) {
    try {
      FileUtil.delete(getRepositoryPath(persistable));
      FileUtil.delete(getCloneRepositoryPath(persistable));
    } catch(IOException e) {
      Throwables.propagate(e);
    }
  }

  public Iterable<CommitInfo> getCommitsInfo(@NotNull @Valid GitPersistable persistable, Class clazz) {
    return gitCommandHandler.execute( //
      new LogsCommand.Builder(getRepositoryPath(persistable), new File(clonesRoot, persistable.pathPrefix())) //
        .path(getJsonFileName(clazz.getSimpleName())) //
        .excludeDeletedCommits(true) //
        .build() //
    ); //
  }

  public CommitInfo getCommitInfo(@NotNull @Valid GitPersistable persistable, @NotNull String commitId, Class clazz) {
    return gitCommandHandler.execute(
      new CommitLogCommand.Builder(getRepositoryPath(persistable), new File(clonesRoot, persistable.pathPrefix()),
        getJsonFileName(clazz.getSimpleName()), commitId).build());
  }

  public String getBlob(@NotNull @Valid GitPersistable persistable, @NotNull String commitId, Class clazz) {
    return gitCommandHandler.execute( //
      new FetchBlobCommand.Builder(getRepositoryPath(persistable), //
        new File(clonesRoot, persistable.pathPrefix()), //
        getJsonFileName(clazz.getSimpleName()) //
      ) //
        .commitId(commitId) //
        .build() //
    ); //
  }

  public Iterable<String> getDiffEntries(@NotNull @Valid GitPersistable persistable, @NotNull String commitId,
    @Nullable String prevCommitId, Class clazz) {
    return gitCommandHandler.execute( //
      new DiffAsStringCommand.Builder(getRepositoryPath(persistable), //
        new File(clonesRoot, persistable.pathPrefix()), //
        commitId //
      ).path(getJsonFileName(clazz.getSimpleName())) //
        .previousCommitId(prevCommitId) //
        .build() //
    );

  }

  public void save(@NotNull @Valid GitPersistable persistable) {
    saveInternal(persistable, null);
  }

  public void save(@NotNull @Valid GitPersistable persistable, @Nullable String comment) {
    saveInternal(persistable, comment);
  }

  private void saveInternal(GitPersistable persistable, String comment) {
    AddDeleteFilesCommand.Builder builder = new AddDeleteFilesCommand.Builder(getRepositoryPath(persistable),
      new File(clonesRoot, persistable.pathPrefix()),
      Strings.isNullOrEmpty(comment) ? persistable.isNew() ? "Created" : "Updated" : comment);

    persistable.parts().entrySet().forEach(p -> {
      try {
        builder.addFile(getJsonFileName(p.getKey()), serializePersistable(p.getValue()));
        //noinspection ResultOfMethodCallIgnored
      } catch(IOException e) {
        throw new RuntimeException("Cannot persist " + persistable + " to " + persistable.getId() + " repo", e);
      }
    });

    gitCommandHandler.execute(builder.build());
  }

  private ByteArrayInputStream serializePersistable(Object persistable) throws IOException {
    return new ByteArrayInputStream(objectMapper.writeValueAsBytes(persistable));
  }

  public <T> T readFromTag(GitIdentifier persistable, String tag, Class<T> clazz) {
    return read(persistable, tag, clazz);
  }

  private <T> T read(GitIdentifier persistable, @Nullable String tag, Class<T> clazz) {
    try {
      try(InputStream inputStream = gitCommandHandler.execute(
        new ReadFileCommand.Builder(getRepositoryPath(persistable), getJsonFileName(clazz.getSimpleName())).tag(tag)
          .build())) {
        return objectMapper.readValue(inputStream, clazz);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read " + clazz.getSimpleName() + " from " + persistable + " repo", e);
    }
  }

  public byte[] readFileHead(GitPersistable persistable, String fileId) {
    return readFile(persistable, fileId, null);
  }

  private byte[] readFile(GitPersistable persistable, String fileId, @Nullable String tag) {
    try {
      try(InputStream inputStream = gitCommandHandler
        .execute(new ReadFileCommand.Builder(getRepositoryPath(persistable), getPathInRepo(fileId)).tag(tag).build())) {
        return ByteStreams.toByteArray(inputStream);
      }
    } catch(IOException e) {
      throw new RuntimeException("Cannot read file " + fileId + " from " + persistable + " repo", e);
    }
  }

  private String getPathInRepo(String attachmentId) {
    return ATTACHMENTS_PATH + attachmentId;
  }

  public Pair<String, String> tag(GitIdentifier gitIdentifier) {
    IncrementTagCommand command = new IncrementTagCommand(getRepositoryPath(gitIdentifier),
      new File(clonesRoot, gitIdentifier.pathPrefix()));
    gitCommandHandler.execute(command);

    return Pair.create(String.valueOf(command.getNewTag()), command.getHeadCommitId());
  }

  public File getRepositoriesRoot() {
    return repositoriesRoot;
  }

  private File getRepositoryPath(GitIdentifier persistable) {
    return new File(repositoriesRoot, Paths.get(persistable.pathPrefix(), persistable.getId() + ".git").toString());
  }

  private File getCloneRepositoryPath(GitIdentifier persistable) {
    return new File(clonesRoot, Paths.get(persistable.pathPrefix(), persistable.getId()).toString());
  }

  private String getJsonFileName(String filename) {
    return filename + ".json";
  }

  private static class IncrementTagCommand extends AbstractGitWriteCommand {

    private int newTag = 1;

    private String headCommitId = "";

    private IncrementTagCommand(@NotNull File repositoryPath, @NotNull File clonesPath) {
      super(repositoryPath, clonesPath, null);
    }

    @Override
    public Iterable<PushResult> execute(Git git) {
      try {
        List<Ref> refs = git.tagList().call();
        if(!refs.isEmpty()) {
          int maxTagNumber = 0;
          for(Ref ref : refs) {
            try {
              int tagNumber = parseTagNumber(ref);
              if(tagNumber > maxTagNumber) {
                maxTagNumber = tagNumber;
              }
            } catch(NumberFormatException e) {
              // ignore
            }
          }
          newTag = maxTagNumber + 1;
        }
        git.tag().setMessage("Create tag " + newTag).setName(String.valueOf(newTag))
          .setTagger(new PersonIdent(getAuthorName(), getAuthorEmail())).call();
        headCommitId = GitUtils.getHeadCommitId(git.getRepository());
        return git.push().setPushTags().setRemote("origin").call();
      } catch(IOException | GitAPIException e) {
        throw new GitException(e);
      }
    }

    private int parseTagNumber(Ref ref) {
      String name = ref.getName();
      return Integer.valueOf(name.substring(name.lastIndexOf('/') + 1, name.length()));
    }

    public int getNewTag() {
      return newTag;
    }

    public String getHeadCommitId() {
      return headCommitId;
    }
  }
}
