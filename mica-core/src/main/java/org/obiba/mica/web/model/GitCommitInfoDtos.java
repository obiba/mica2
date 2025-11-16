/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.validation.constraints.NotNull;

import org.obiba.git.CommitInfo;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class GitCommitInfoDtos {

  @NotNull
  public List<Mica.GitCommitInfoDto> asDto(Iterable<CommitInfo> commitInfos) {
    return StreamSupport.stream(commitInfos.spliterator(), false).map(this::asDto).collect(Collectors.toList());
  }

  @NotNull
  public Mica.GitCommitInfoDto asDto(CommitInfo commitInfo) {
    Mica.GitCommitInfoDto.Builder commitInfoDtoBuilder = Mica.GitCommitInfoDto.newBuilder() //
      .setAuthor(commitInfo.getAuthorName()) //
      .setDate(commitInfo.getDateAsIso8601()) //
      .setCommitId(commitInfo.getCommitId()) //
      .setComment(commitInfo.getComment()) //
      .setIsHead(commitInfo.isHead()) //
      .setIsCurrent(commitInfo.isCurrent());

    List<String> diffEntries = commitInfo.getDiffEntries();
    if(diffEntries != null) {
      commitInfoDtoBuilder.addAllDiffEntries(diffEntries);
    }
    String blob = commitInfo.getBlob();
    if(!Strings.isNullOrEmpty(blob)) {
      commitInfoDtoBuilder.setBlob(blob);
    }
    return commitInfoDtoBuilder.build();
  }
}
