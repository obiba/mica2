/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import org.obiba.git.CommitInfo;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.domain.Timestamped;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.domain.DatasetConfig;
import org.obiba.mica.micaConfig.domain.EntityConfig;
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
import org.obiba.oidc.OIDCAuthProviderSummary;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.shiro.realm.ObibaRealm.Subject;
import org.obiba.web.model.OIDCDtos;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.obiba.mica.web.model.Mica.*;

@Component
@SuppressWarnings("OverlyCoupledClass")
public class Dtos {

  private final StudyDtos studyDtos;

  private final MicaConfigDtos micaConfigDtos;

  private final NetworkDtos networkDtos;

  private final StudySummaryDtos studySummaryDtos;

  private final NetworkSummaryDtos networkSummaryDtos;

  private final DocumentDigestDtos documentDigestDtos;

  private final TempFileDtos tempFileDtos;

  private final AttachmentDtos attachmentDtos;

  private final DatasetDtos datasetDtos;

  private final ProjectDtos projectDtos;

  private final DataAccessRequestDtos dataAccessRequestDtos;

  private final CommentDtos commentDtos;

  private final PersonDtos personDtos;

  private final TaxonomyDtos taxonomyDtos;

  private final LocalizedStringDtos localizedStringDtos;

  private final UserProfileDtos userProfileDtos;

  private final GitCommitInfoDtos gitCommitInfoDtos;

  private final SetDtos setDtos;

  private final AttributeDtos attributeDtos;

  private final OidcAuthProviderSummaryDtos oidcAuthProviderSummaryDtos;

  @Inject
  public Dtos(OidcAuthProviderSummaryDtos oidcAuthProviderSummaryDtos,
              DatasetDtos datasetDtos,
              StudyDtos studyDtos,
              MicaConfigDtos micaConfigDtos,
              NetworkDtos networkDtos,
              StudySummaryDtos studySummaryDtos,
              TaxonomyDtos taxonomyDtos,
              SetDtos setDtos,
              PersonDtos personDtos,
              NetworkSummaryDtos networkSummaryDtos,
              DocumentDigestDtos documentDigestDtos,
              LocalizedStringDtos localizedStringDtos,
              TempFileDtos tempFileDtos,
              AttachmentDtos attachmentDtos,
              ProjectDtos projectDtos,
              GitCommitInfoDtos gitCommitInfoDtos,
              CommentDtos commentDtos,
              UserProfileDtos userProfileDtos,
              DataAccessRequestDtos dataAccessRequestDtos, AttributeDtos attributeDtos) {
    this.oidcAuthProviderSummaryDtos = oidcAuthProviderSummaryDtos;
    this.datasetDtos = datasetDtos;
    this.studyDtos = studyDtos;
    this.micaConfigDtos = micaConfigDtos;
    this.networkDtos = networkDtos;
    this.studySummaryDtos = studySummaryDtos;
    this.taxonomyDtos = taxonomyDtos;
    this.setDtos = setDtos;
    this.personDtos = personDtos;
    this.networkSummaryDtos = networkSummaryDtos;
    this.documentDigestDtos = documentDigestDtos;
    this.localizedStringDtos = localizedStringDtos;
    this.tempFileDtos = tempFileDtos;
    this.attachmentDtos = attachmentDtos;
    this.projectDtos = projectDtos;
    this.gitCommitInfoDtos = gitCommitInfoDtos;
    this.commentDtos = commentDtos;
    this.userProfileDtos = userProfileDtos;
    this.dataAccessRequestDtos = dataAccessRequestDtos;
    this.attributeDtos = attributeDtos;
  }

  public Mica.DocumentSetDto asDto(DocumentSet documentSet) {
    return setDtos.asDto(documentSet);
  }

  public Mica.SetOperationDto asDto(SetOperation setOperation) {
    return setDtos.asDto(setOperation);
  }

  public Mica.DataAccessRequestDto.StatusChangeDto asDto(StatusChange statusChange) {
    return dataAccessRequestDtos.getStatusChangeDtos().asDto(statusChange);
  }

  @NotNull
  public StudyDto asDto(@NotNull BaseStudy study) {
    return asDto(study, false);
  }

  @NotNull
  public StudyDto asDto(@NotNull BaseStudy study, boolean asDraft) {
    return study instanceof Study
      ? studyDtos.asDto((Study)study, asDraft)
      : studyDtos.asDto((HarmonizationStudy)study, asDraft);
  }

  @NotNull
  public StudyDto asDto(@NotNull Study study) {
    return asDto(study, false);
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
  public StudySummaryDto asDto(@NotNull BaseStudy study, @NotNull EntityState studyState) {
    return studySummaryDtos.asDto(study, studyState);
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
  public ActionLog fromDto(@NotNull DataAccessRequestDto.ActionLogDto dto) {
    return dataAccessRequestDtos.fromDto(dto);
  }

  @NotNull
  public NetworkDto.Builder asDtoBuilderForSearchListing(@NotNull Network network) {
    return networkDtos.asDtoBuilderForSearchListing(network, false);
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
  public MicaConfigDto asDto(@NotNull MicaConfig micaConfig, String language) {
    return micaConfigDtos.asDto(micaConfig, language);
  }

  @NotNull
  public MicaConfigDto asDto(@NotNull MicaConfig micaConfig) {
    return micaConfigDtos.asDto(micaConfig, null);
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
  public List<Mica.DatasetVariableDto> asDtoList(Map<String, List<DatasetVariable>> studyIdVariableMap, @NotNull List<Taxonomy> taxonomies) {
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
  public Mica.DatasetHarmonizedVariableSummaryDto asHarmonizedSummaryDto(@NotNull DatasetVariable variable) {
    return datasetDtos.asHarmonizedSummaryDto(variable);
  }

  @NotNull
  public Mica.DatasetVariableSummaryDto asSummaryDto(@NotNull DatasetVariable variable, BaseStudyTable studyTable, boolean includeSummaries) {
    return datasetDtos.asSummaryDto(variable, studyTable, includeSummaries);
  }

  @NotNull
  public Mica.DatasetDto.StudyTableDto.Builder asDto(StudyTable studyTable, boolean includeSummary) {
    return datasetDtos.asDto(studyTable, includeSummary);
  }

  @NotNull
  public Mica.DatasetDto.HarmonizationTableDto.Builder asDto(HarmonizationStudyTable harmonizationTable, boolean includeSummary) {
    return datasetDtos.asDto(harmonizationTable, includeSummary);
  }

  @NotNull
  public Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver variableResolver) {
    return datasetDtos.asDto(variableResolver);
  }

  @NotNull
  public Mica.DatasetVariableResolverDto.Builder asDto(@NotNull DatasetVariable.IdResolver variableResolver, DatasetVariable variable) {
    return datasetDtos.asDto(variableResolver, variable);
  }

  @NotNull
  public Mica.DatasetDto.StudyTableDto.Builder asDto(@NotNull StudyTable studyTable) {
    return datasetDtos.asDto(studyTable);
  }

  @NotNull
  public Mica.DatasetVariableAggregationDto.Builder asDto(@NotNull BaseStudyTable studyTable,
    @Nullable Mica.DatasetVariableAggregationDto summary, boolean withStudySummary) {
    return datasetDtos.asDto(studyTable, summary, withStudySummary);
  }

  @NotNull
  public Mica.DatasetVariableContingencyDto.Builder asContingencyDto(BaseStudyTable studyTable, @Nullable Mica.DatasetVariableContingencyDto results) {
    return datasetDtos.asContingencyDto(studyTable, results);
  }

  @NotNull
  public Mica.DataAccessRequestDto asDto(@NotNull DataAccessRequest request) {
    return dataAccessRequestDtos.asDto(request);
  }

  public List<Mica.DataAccessRequestDto> asDtoList(@NotNull List<DataAccessRequest> requests) {
    return dataAccessRequestDtos.asDtoList(requests);
  }

  @NotNull
  public DataAccessRequest fromDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DataAccessRequestDto asPreliminaryDto(@NotNull DataAccessPreliminary preliminary) {
    return dataAccessRequestDtos.asPreliminaryDto(preliminary);
  }

  @NotNull
  public Mica.DataAccessRequestDto asFeasibilityDto(@NotNull DataAccessFeasibility feasibility) {
    return dataAccessRequestDtos.asFeasibilityDto(feasibility);
  }

  @NotNull
  public Mica.DataAccessRequestDto asAgreementDto(@NotNull DataAccessAgreement agreement) {
    return dataAccessRequestDtos.asAgreementDto(agreement);
  }

  @NotNull
  public DataAccessPreliminary fromPreliminaryDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromPreliminaryDto(dto);
  }

  @NotNull
  public DataAccessFeasibility fromFeasibilityDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromFeasibilityDto(dto);
  }

  @NotNull
  public DataAccessAgreement fromAgreementDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromAgreementDto(dto);
  }

  @NotNull
  public Mica.DataAccessRequestDto asAmendmentDto(@NotNull DataAccessAmendment amendment) {
    return dataAccessRequestDtos.asAmendmentDto(amendment);
  }

  @NotNull
  public DataAccessAmendment fromAmendmentDto(@NotNull Mica.DataAccessRequestDto dto) {
    return dataAccessRequestDtos.fromAmendmentDto(dto);
  }

  @NotNull
  public Mica.DataAccessCollaboratorDto asDto(@NotNull DataAccessCollaborator collaborator) {
    return dataAccessRequestDtos.asDto(collaborator);
  }

  @NotNull
  public List<Mica.DataAccessRequestDto.StatusChangeDto> asStatusChangeDtoList(@NotNull DataAccessEntity entity) {
    return dataAccessRequestDtos.asStatusChangeDtoList(entity);
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
  public Mica.DataAccessConfigDto asDto(@NotNull DataAccessConfig dataAccessConfig) {
    return micaConfigDtos.asDto(dataAccessConfig);
  }

  @NotNull
  public DataAccessConfig fromDto(@NotNull Mica.DataAccessConfigDto dto) {
    return micaConfigDtos.fromDto(dto);
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
  public Mica.DataAccessPreliminaryFormDto asDto(@NotNull DataAccessPreliminaryForm dataAccessPreliminaryForm) {
    return micaConfigDtos.asDto(dataAccessPreliminaryForm);
  }

  @NotNull
  public DataAccessPreliminaryForm fromDto(@NotNull Mica.DataAccessPreliminaryFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DataAccessFeasibilityFormDto asDto(@NotNull DataAccessFeasibilityForm dataAccessFeasibilityForm) {
    return micaConfigDtos.asDto(dataAccessFeasibilityForm);
  }

  @NotNull
  public DataAccessFeasibilityForm fromDto(@NotNull Mica.DataAccessFeasibilityFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DataAccessAgreementFormDto asDto(@NotNull DataAccessAgreementForm dataAccessAgreementForm) {
    return micaConfigDtos.asDto(dataAccessAgreementForm);
  }

  @NotNull
  public DataAccessAgreementForm fromDto(@NotNull Mica.DataAccessAgreementFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.DataAccessAmendmentFormDto asDto(@NotNull DataAccessAmendmentForm dataAccessAmendmentForm, DataAccessConfig dataAccessConfig) {
    return micaConfigDtos.asDto(dataAccessAmendmentForm, dataAccessConfig);
  }

  @NotNull
  public DataAccessAmendmentForm fromDto(@NotNull Mica.DataAccessAmendmentFormDto dto) {
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
  public Mica.EntityFormDto asDto(@NotNull DataCollectionEventConfig dataCollectionEventConfig) {
    return micaConfigDtos.asDto(dataCollectionEventConfig);
  }

  @NotNull
  public <T extends EntityConfig> T fromDto(@NotNull Mica.EntityFormDto dto) {
    return micaConfigDtos.fromDto(dto);
  }

  @NotNull
  public Mica.CommentDto asDto(@NotNull Comment comment, boolean withPermissions) {
    return commentDtos.asDto(comment, withPermissions);
  }

  @NotNull
  public Mica.CommentDto asDto(@NotNull Comment comment) {
    return commentDtos.asDto(comment,true);
  }

  public List<Mica.CommentDto> asDtos(@NotNull List<Comment> comments) {
    return commentDtos.asDtos(comments);
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

  @NotNull
  public OIDCDtos.OIDCAuthProviderSummaryDto asSummaryDto(OIDCAuthProviderSummary summary) {
    return oidcAuthProviderSummaryDtos.asSummaryDto(summary);
  }

  @NotNull
  public Set<Attribute> fromDto(List<Mica.AttributeDto> dtos) {
    return dtos.stream().map(attributeDtos::fromDto).collect(Collectors.toSet());
  }

  @NotNull
  public List<Mica.AttributeDto> asDto(Set<Attribute> attributes) {
    return attributes.stream().map(attributeDtos::asDto).collect(Collectors.toList());
  }

}
