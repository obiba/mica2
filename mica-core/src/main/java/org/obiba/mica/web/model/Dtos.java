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
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.git.CommitInfo;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.domain.DatasetConfig;
import org.obiba.mica.micaConfig.domain.EntityConfig;
import org.obiba.mica.micaConfig.domain.HarmonizationPopulationConfig;
import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.NetworkConfig;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Math;
import org.obiba.opal.web.model.Search;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.springframework.stereotype.Component;

import static org.obiba.mica.web.model.Mica.DocumentDigestDto;
import static org.obiba.mica.web.model.Mica.MicaConfigDto;
import static org.obiba.mica.web.model.Mica.MicaConfigDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.NetworkDto;
import static org.obiba.mica.web.model.Mica.NetworkDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudyDto;
import static org.obiba.mica.web.model.Mica.StudyDtoOrBuilder;
import static org.obiba.mica.web.model.Mica.StudySummaryDto;

@Component
@SuppressWarnings("OverlyCoupledClass")
public class Dtos {

  @Inject
  private StudyDtos studyDtos;

  @Inject
  private MicaConfigDtos micaConfigDtos;

  @Inject
  private NetworkDtos networkDtos;

  @Inject
  private StudySummaryDtos studySummaryDtos;

  @Inject
  private NetworkSummaryDtos networkSummaryDtos;

  @Inject
  private DocumentDigestDtos documentDigestDtos;

  @Inject
  private TempFileDtos tempFileDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

  @Inject
  private DatasetDtos datasetDtos;

  @Inject
  private ProjectDtos projectDtos;

  @Inject
  private DataAccessRequestDtos dataAccessRequestDtos;

  @Inject
  private CommentDtos commentDtos;

  @Inject
  private PersonDtos personDtos;

  @Inject
  private TaxonomyDtos taxonomyDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private UserProfileDtos userProfileDtos;

  @Inject
  private GitCommitInfoDtos gitCommitInfoDtos;

  @Inject
  private DocumentSetDtos documentSetDtos;

  public Mica.DocumentSetDto asDto(DocumentSet documentSet) {
    return documentSetDtos.asDto(documentSet);
  }

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    return asDto(study, false);
  }

  @NotNull
  public StudyDto asDto(@NotNull BaseStudy study, boolean asDraft) {
    return study instanceof Study
      ? studyDtos.asDto((Study)study, asDraft)
      : studyDtos.asDto((HarmonizationStudy)study, asDraft);
  }

  @NotNull
  public StudyDto asDto(@NotNull Study study, boolean asDraft) {
    return studyDtos.asDto(study, asDraft);
  }

  @NotNull
  public StudyDto asDto(@NotNull HarmonizationStudy study, boolean asDraft, List<HarmonizationDataset> datasets) {
    return datasets.isEmpty() ? studyDtos.asDto(study, asDraft) : studyDtos.asDto(study, asDraft, datasets);
  }

  @NotNull
  public StudyDto asDto(@NotNull HarmonizationStudy study) {
    return asDto(study, false);
  }

  @NotNull
  public StudyDto asDto(@NotNull HarmonizationStudy study, boolean asDraft) {
    return studyDtos.asDto(study, asDraft);
  }

  @NotNull
  public DocumentDigestDto.Builder asDigestDtoBuilder(@NotNull Dataset dataset) {
    return documentDigestDtos.asDtoBuilder(dataset);
  }

  @NotNull
  public DocumentDigestDto asDigestDto(@NotNull Dataset dataset) {
    return documentDigestDtos.asDto(dataset);
  }

  @NotNull
  public DocumentDigestDto.Builder asDigestDtoBuilder(@NotNull BaseStudy study) {
    return documentDigestDtos.asDtoBuilder(study);
  }

  @NotNull
  public DocumentDigestDto asDigestDto(@NotNull Study study) {
    return documentDigestDtos.asDto(study);
  }

  @NotNull
  public DocumentDigestDto.Builder asDigestDtoBuilder(@NotNull Network network) {
    return documentDigestDtos.asDtoBuilder(network);
  }

  @NotNull
  public DocumentDigestDto asDigestDto(@NotNull Network network) {
    return documentDigestDtos.asDto(network);
  }

  @NotNull
  public StudySummaryDto.Builder asSummaryDtoBuilder(@NotNull BaseStudy study) {
    return studySummaryDtos.asDtoBuilder(study);
  }

  @NotNull
  public StudySummaryDto asSummaryDto(@NotNull BaseStudy study) {
    return studySummaryDtos.asDto(study);
  }

  @NotNull
  public StudySummaryDto asSummaryDto(@NotNull BaseStudy study, @NotNull StudyState studyState) {
    return studySummaryDtos.asDto(study, studyState);
  }

  @NotNull
  public StudySummaryDto asDto(@NotNull EntityState studyState) {
    return studySummaryDtos.asDto(studyState);
  }

  @NotNull
  public StudySummaryDto asDto(@NotNull HarmonizationStudyState studyState) {
    return studySummaryDtos.asDto(studyState);
  }

  @NotNull
  public StudySummaryDto asDto(@NotNull StudyState studyState) {
    return studySummaryDtos.asDto(studyState);
  }

  @NotNull
  public BaseStudy fromDto(@NotNull StudyDtoOrBuilder dto) {
    return studyDtos.fromDto(dto);
  }

  @NotNull
  public NetworkDto.Builder asDtoBuilder(@NotNull Network network) {
    return networkDtos.asDtoBuilder(network, false);
  }

  @NotNull
  public NetworkDto asDto(@NotNull Network network) {
    return networkDtos.asDto(network, false);
  }

  @NotNull
  public NetworkDto asDto(@NotNull Network network, boolean asDraft) {
    return networkDtos.asDto(network, asDraft);
  }

  @NotNull
  public Mica.NetworkSummaryDto asSummaryDto(@NotNull Network network, boolean asDraft) {
    return networkSummaryDtos.asDto(network, asDraft);
  }

  @NotNull
  public Network fromDto(@NotNull NetworkDtoOrBuilder dto) {
    return networkDtos.fromDto(dto);
  }

  @NotNull
  public MicaConfigDto asDto(@NotNull MicaConfig micaConfig) {
    return micaConfigDtos.asDto(micaConfig);
  }

  @NotNull
  public Mica.PublicMicaConfigDto asPublicDto(@NotNull MicaConfig micaConfig) {
    return micaConfigDtos.asPublicDto(micaConfig);
  }

  @NotNull
  public MicaConfig fromDto(@NotNull MicaConfigDtoOrBuilder dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.OpalCredentialDto asDto(@NotNull OpalCredential opalCredential) {
    return micaConfigDtos.asDto(opalCredential);
  }

  @NotNull
  public Mica.TimestampsDto asDto(@NotNull Timestamped timestamped) {
    return TimestampsDtos.asDto(timestamped);
  }

  @NotNull
  public Mica.TempFileDto asDto(@NotNull TempFile tempFile) {
    return tempFileDtos.asDto(tempFile);
  }

  @NotNull
  public Mica.AttachmentDto asDto(@NotNull Attachment attachment) {
    return attachmentDtos.asDto(attachment);
  }

  @NotNull
  public Mica.FileDto asFileDto(@NotNull AttachmentState attachmentState) {
    return attachmentDtos.asFileDto(attachmentState);
  }

  @NotNull
  public Mica.FileDto asFileDto(@NotNull AttachmentState attachmentState, boolean publishedFileSystem) {
    return attachmentDtos.asFileDto(attachmentState, publishedFileSystem, true);
  }

  @NotNull
  public Mica.FileDto asFileDto(@NotNull AttachmentState attachmentState, boolean publishedFileSystem, boolean detailed) {
    return attachmentDtos.asFileDto(attachmentState, publishedFileSystem, detailed);
  }

  @NotNull
  public Attachment fromDto(@NotNull Mica.AttachmentDto attachment) {
    return attachmentDtos.fromDto(attachment);
  }

  @NotNull
  public Mica.DatasetDto asDto(@NotNull Dataset dataset) {
    return asDto(dataset, false);
  }

  @NotNull
  public Mica.DatasetDto asDto(@NotNull Dataset dataset, boolean asDraft) {
    return asDto(dataset, asDraft, !asDraft);
  }

  public Mica.DatasetDto asDto(@NotNull Dataset dataset, boolean asDraft, boolean studySummary) {
    if(dataset instanceof StudyDataset) {
      return datasetDtos.asDto((StudyDataset) dataset, asDraft, studySummary);
    } else {
      return datasetDtos.asDto((HarmonizationDataset) dataset, asDraft, studySummary);
    }
  }

  @NotNull
  public Mica.DatasetDto.Builder asDtoBuilder(@NotNull Dataset dataset) {
    if(dataset instanceof StudyDataset) {
      return datasetDtos.asDtoBuilder((StudyDataset) dataset, false, true);
    } else {
      return datasetDtos.asDtoBuilder((HarmonizationDataset) dataset, false, true);
    }
  }

  @NotNull
  public Dataset fromDto(Mica.DatasetDtoOrBuilder dto) {
    return datasetDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable) {
    return datasetDtos.asDto(variable);
  }

  @NotNull
  public List<Mica.DatasetVariableDto> asDtos(Map<String, List<DatasetVariable>> studyIdVariableMap, @NotNull List<Taxonomy> taxonomies) {
    return datasetDtos.asDtos(studyIdVariableMap, taxonomies, "en");
  }

  @NotNull
  public Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, List<Taxonomy> taxonomies, String locale) {
    return datasetDtos.asDto(variable, taxonomies, locale);
  }

  @NotNull
  public Mica.DatasetVariableDto asDto(@NotNull DatasetVariable variable, List<Taxonomy> taxonomies) {
    return datasetDtos.asDto(variable, taxonomies, "en");
  }

  @NotNull
  public Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, OpalTable opalTable, boolean includeSummaries) {
    return datasetDtos.asSummaryDto(variable, opalTable, includeSummaries);
  }

  @NotNull
  public Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver variableResolver) {
    return datasetDtos.asDto(variableResolver);
  }

  @NotNull
  public Mica.DatasetDto.StudyTableDto.Builder asDto(@NotNull StudyTable studyTable) {
    return datasetDtos.asDto(studyTable);
  }

  @NotNull
  public Mica.DatasetVariableAggregationDto.Builder asDto(@NotNull OpalTable opalTable,
    @Nullable Math.SummaryStatisticsDto summary) {
    return datasetDtos.asDto(opalTable, summary);
  }

  @NotNull
  public Mica.DatasetVariableContingencyDto.Builder asContingencyDto(OpalTable opalTable, DatasetVariable variable,
                                                                     DatasetVariable crossVariable, @Nullable Search.QueryResultDto results) {
    return datasetDtos.asContingencyDto(opalTable, variable, crossVariable, results);
  }

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    return dataAccessRequestDtos.asDto(request);
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromDto(dto);
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Taxonomy taxonomy, @Nullable String locale) {
    return taxonomyDtos.asDto(taxonomy, locale);
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Vocabulary vocabulary, @Nullable String locale) {
    return taxonomyDtos.asDto(vocabulary, locale);
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Term term, @Nullable String locale) {
    return taxonomyDtos.asDto(term, locale);
  }

  @NotNull
  public Iterable<Mica.LocalizedStringDto> asDto(LocalizedString string) {
    return localizedStringDtos.asDto(string);
  }


  @NotNull
  public Mica.ProjectDto asDto(@NotNull Project project) {
    return asDto(project, false);
  }

  @NotNull
  public Mica.ProjectDto asDto(@NotNull Project project, boolean asDraft) {
    return projectDtos.asDto(project, asDraft);
  }

  @NotNull
  public Project fromDto(@NotNull Mica.ProjectDto dto) {
    return projectDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DataAccessFormDto asDto(@NotNull DataAccessForm dataAccessForm) {
    return micaConfigDtos.asDto(dataAccessForm);
  }

  @NotNull
  public DataAccessForm fromDto(@NotNull Mica.DataAccessFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.ProjectFormDto asDto(@NotNull ProjectConfig projectConfig) {
    return micaConfigDtos.asDto(projectConfig);
  }

  @NotNull
  public ProjectConfig fromDto(@NotNull Mica.ProjectFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull DatasetConfig  datasetConfig) {
    return micaConfigDtos.asDto(datasetConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull NetworkConfig projectConfig) {
    return micaConfigDtos.asDto(projectConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull StudyConfig studyConfig) {
    return micaConfigDtos.asDto(studyConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull HarmonizationStudyConfig harmonizationStudyConfig) {
    return micaConfigDtos.asDto(harmonizationStudyConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull PopulationConfig populationConfig) {
    return micaConfigDtos.asDto(populationConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull HarmonizationPopulationConfig harmonizationPopulationConfig) {
    return micaConfigDtos.asDto(harmonizationPopulationConfig);
  }

  @NotNull
  public Mica.EntityFormDto asDto(@NotNull DataCollectionEventConfig dataCollectionEventConfig) {
    return micaConfigDtos.asDto(dataCollectionEventConfig);
  }

  @NotNull
  public <T extends EntityConfig> T fromDto(@NotNull Mica.EntityFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.CommentDto asDto(@NotNull Comment comment) {
    return commentDtos.asDto(comment);
  }

  @NotNull
  public Comment fromDto(@NotNull Mica.CommentDto dto) {
    return commentDtos.fromDto(dto);
  }

  @NotNull
  public Mica.PersonDto asDto(@NotNull Person person, boolean asDraft) {
    return personDtos.asDto(person, asDraft);
  }

  @NotNull
  public Person fromDto(@NotNull Mica.PersonDto dto) {
    return personDtos.fromDto(dto);
  }

  @NotNull
  public List<Mica.LocalizedPropertyDto> asDtoList(@NotNull Map<String, LocalizedString> properties) {
    return micaConfigDtos.asDtoList(properties);
  }

  @NotNull
  public Mica.UserProfileDto asDto(Subject subject) {
    return userProfileDtos.asDto(subject);
  }

  @NotNull
  public List<Mica.GitCommitInfoDto> asDto(Iterable<CommitInfo> commitInfos) {
    return gitCommitInfoDtos.asDto(commitInfos);
  }

  @NotNull
  public Mica.GitCommitInfoDto asDto(CommitInfo commitInfo) {
    return gitCommitInfoDtos.asDto(commitInfo);
  }
}
