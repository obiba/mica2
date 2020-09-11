<!-- Study page macros -->

<!-- Study model template -->
<#macro studyModel study type>

  <!-- Individual study -->
  <#if type == "Individual">
    <div class="row">
      <div class="col-sm-12 col-lg-6">
        <@studyDesign study=study/>

        <#if study.model.markerPaper??>
          <@studyMarkerPaper study=study/>
        </#if>
      </div>

      <div class="col-sm-12 col-lg-6">
        <#if study.model.methods?? && study.model.methods.recruitments??>
          <@studyRecruitments study=study/>
        </#if>

        <#if study.model.numberOfParticipants??>
          <@studyParticipants study=study/>
        </#if>

        <@studyAccess study=study/>

      </div>
    </div>
  </#if>

  <!-- Harmonization study -->
  <#if type == "Harmonization">
    <#if study.model.harmonizationDesign??>
      <div class="row">
        <div class="col-12">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title">
                <@message "study.design"/>
              </h3>
              <div class="card-tools">
                <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                  <i class="fas fa-minus"></i></button>
              </div>
            </div>
            <!-- /.card-header -->
            <div class="card-body">
              ${localize(study.model.harmonizationDesign)}
            </div>
          </div>
        </div>
      </div>
    </#if>
  </#if>

</#macro>

<#macro studyDesign study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title">
        <@message "design"/>
      </h3>
      <div class="card-tools">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
          <i class="fas fa-minus"></i></button>
      </div>
    </div>
    <!-- /.card-header -->
    <div class="card-body">

      <dl class="row">
        <#if study.model.methods?? && study.model.methods.design??>
          <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.methods-design.description"/>">
            <@message "study_taxonomy.vocabulary.methods-design.title"/>
          </dt>
          <dd class="col-sm-8">
            <#assign text = "study_taxonomy.vocabulary.methods-design.term." + study.model.methods.design + ".title"/>
            <@message text/>
            <#if study.model.methods.design == "other" && study.model.methods.otherDesign??>
              : ${localize(study.model.methods.otherDesign)}
            </#if>
          </dd>
        </#if>

        <#if study.model.startYear??>
          <dt class="col-sm-4">
            <@message "study.start-year"/>
          </dt>
          <dd class="col-sm-8">
            ${study.model.startYear?c}
          </dd>
        </#if>

        <#if study.model.endYear??>
          <dt class="col-sm-4">
            <@message "study.end-year"/>
          </dt>
          <dd class="col-sm-8">
            ${study.model.endYear?c}
          </dd>
        </#if>

        <#if study.model.funding??>
          <dt class="col-sm-4">
            <@message "funding"/>
          </dt>
          <dd class="col-sm-8">
            ${localize(study.model.funding)}
          </dd>
        </#if>

        <#if study.model.methods?? && study.model.methods.followUpInfo??>
          <dt class="col-sm-4">
            <@message "study.follow-up"/>
          </dt>
          <dd class="col-sm-8">
            ${localize(study.model.methods.followUpInfo)}
          </dd>
        </#if>

        <#if study.model.info??>
          <dt class="col-sm-4">
            <@message "suppl-info"/>
          </dt>
          <dd class="col-sm-8">
            ${localize(study.model.info)}
          </dd>
        </#if>
      </dl>
    </div>
  </div>
</#macro>

<#macro studyMarkerPaper study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title">
          <@message "study.marker-paper"/>
      </h3>
      <div class="card-tools">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
          <i class="fas fa-minus"></i></button>
      </div>
    </div>
    <!-- /.card-header -->
    <div class="card-body">
      <p>
          ${study.model.markerPaper}
      </p>
        <#if study.model.pubmedId??>
          <blockquote>
            <a href="http://www.ncbi.nlm.nih.gov/pubmed/${study.model.pubmedId}" target="_blank">PUBMED ${study.model.pubmedId}</a>
          </blockquote>
        </#if>
    </div>
  </div>
</#macro>

<#macro studyRecruitments study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title">
          <@message "population.recruitment"/>
      </h3>
      <div class="card-tools">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
          <i class="fas fa-minus"></i></button>
      </div>
    </div>
    <!-- /.card-header -->
    <div class="card-body">
      <dl class="row">
        <dt class="col-sm-6" title="<@message "study_taxonomy.vocabulary.methods-recruitments.description"/>">
            <@message "study.recruitment-sources.label"/>
        </dt>
        <dd class="col-sm-6">
          <ul class="pl-3">
              <#list study.model.methods.recruitments as type>
                <li>
                    <#assign text = "study_taxonomy.vocabulary.methods-recruitments.term." + type + ".title"/>
                    <@message text/>
                    <#if type == "other" && study.model.methods.otherRecruitment??>
                      : ${localize(study.model.methods.otherRecruitment)}
                    </#if>
                </li>
              </#list>
          </ul>
        </dd>
      </dl>
    </div>
  </div>
</#macro>

<#macro studyParticipants study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title">
          <@message "numberOfParticipants.label"/>
      </h3>
      <div class="card-tools">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
          <i class="fas fa-minus"></i></button>
      </div>
    </div>
    <!-- /.card-header -->
    <div class="card-body">
      <dl class="row">
          <#if study.model.numberOfParticipants.participant.number??>
            <dt class="col-sm-6">
                <@message "numberOfParticipants.participants"/>
            </dt>
            <dd class="col-sm-6">
                ${study.model.numberOfParticipants.participant.number}
                <#if study.model.numberOfParticipants.participant.noLimit == true>
                  (<@message "numberOfParticipants.no-limit"/>)
                </#if>
            </dd>
          <#else>
            <dt class="col-sm-6">
                <@message "numberOfParticipants.participants"/>
            </dt>
            <dd class="col-sm-6">
                <#if study.model.numberOfParticipants.participant.noLimit?? && study.model.numberOfParticipants.participant.noLimit == true>
                    <@message "numberOfParticipants.no-limit"/>
                <#else>
                  <i class="fas fa-minus"></i>
                </#if>
            </dd>
          </#if>

          <#if study.model.numberOfParticipants.sample.number??>
            <dt class="col-sm-6">
                <@message "numberOfParticipants.sample"/>
            </dt>
            <dd class="col-sm-6">
                ${study.model.numberOfParticipants.sample.number}
                <#if study.model.numberOfParticipants.sample.noLimit == true>
                  (<@message "numberOfParticipants.no-limit"/>)
                </#if>
            </dd>
          <#else>
            <dt class="col-sm-6">
                <@message "numberOfParticipants.sample"/>
            </dt>
            <dd class="col-sm-6">
                <#if study.model.numberOfParticipants.sample.noLimit?? && study.model.numberOfParticipants.sample.noLimit == true>
                    <@message "numberOfParticipants.no-limit"/>
                <#else>
                  <i class="fas fa-minus"></i>
                </#if>
            </dd>
          </#if>

          <#if study.model.numberOfParticipants.info??>
            <dt class="col-sm-6">
                <@message "suppl-info"/>
            </dt>
            <dd class="col-sm-6">
                ${localize(study.model.numberOfParticipants.info)}
            </dd>
          </#if>
      </dl>
    </div>
  </div>
</#macro>

<#macro studyAccess study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title">
          <@message "study.access.label"/>
      </h3>
      <div class="card-tools">
        <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
          <i class="fas fa-minus"></i></button>
      </div>
    </div>
    <!-- /.card-header -->
    <div class="card-body">

      <#if study.model.access_restrictions?? || study.model.access_fees??>
        <div class="card card-primary card-outline card-outline-tabs">
          <div class="card-header p-0 border-bottom-0">
            <ul class="nav nav-tabs" id="access-tab" role="tablist">
              <li class="nav-item">
                <a class="nav-link active" id="access-info-tab" data-toggle="pill" href="#access-info" role="tab" aria-controls="access-info" aria-selected="true">
                  <@message "study.general-info"/>
                </a>
              </li>
                <#if study.model.access_restrictions?? && study.model.access_restrictions == true>
                  <li class="nav-item">
                    <a class="nav-link" id="access-restrictions-tab" data-toggle="pill" href="#access-restrictions" role="tab" aria-controls="access-restrictions" aria-selected="false">
                        <@message "study.access.access_restrictions.title"/>
                    </a>
                  </li>
                </#if>
                <#if study.model.access_fees?? && study.model.access_fees == true>
                  <li class="nav-item">
                    <a class="nav-link" id="access-fees-tab" data-toggle="pill" href="#access-fees" role="tab" aria-controls="access-fees" aria-selected="false">
                        <@message "study.access.access_fees.title"/>
                    </a>
                  </li>
                </#if>
            </ul>
          </div>
          <div class="card-body">
            <div class="tab-content" id="access-tabContent">
              <div class="tab-pane fade show active" id="access-info" role="tabpanel" aria-labelledby="access-info-tab">
                <@studyAccessInfo study=study/>
              </div>
              <#if study.model.access_restrictions?? && study.model.access_restrictions == true>
                <div class="tab-pane fade" id="access-restrictions" role="tabpanel" aria-labelledby="access-restrictions-tab">

                  <p><b><@message "study.access.access_restrictions.sector-of-research.title"/></b></p>

                  <table class="table table-sm table-striped mt-2">
                    <thead>
                    <th></th>
                    <th>
                        <@message "study_taxonomy.vocabulary.access_data.title"/>
                    </th>
                    <th>
                        <@message "study_taxonomy.vocabulary.access_bio_samples.title"/>
                    </th>
                    </thead>
                    <tbody>
                    <tr>
                      <td><@message "study.access.access_permission_data.public_sector"/></td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_data.public_sector/>
                      </td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_biological_samples.public_sector/>
                      </td>
                    </tr>
                    <tr>
                      <td><@message "study.access.access_permission_data.private_sector"/></td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_data.private_sector/>
                      </td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_biological_samples.private_sector/>
                      </td>
                    </tr>
                    <tr>
                      <td><@message "study.access.access_permission_data.not_for_profit_organization"/></td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_data.not_for_profit_organization/>
                      </td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_permission_biological_samples.not_for_profit_organization/>
                      </td>
                    </tr>
                    </tbody>
                  </table>

                    <#if study.model.access_permission_additional_info??>
                      <p class="text-muted">${localize(study.model.access_permission_additional_info)}</p>
                    </#if>

                  <p><b><@message "study.access.access_restrictions.transfer.title"/></b></p>

                  <table class="table table-sm table-striped mt-2">
                    <thead>
                    <th></th>
                    <th>
                        <@message "study_taxonomy.vocabulary.access_data.title"/>
                    </th>
                    <th>
                        <@message "study_taxonomy.vocabulary.access_bio_samples.title"/>
                    </th>
                    </thead>
                    <tbody>
                    <tr>
                      <td><@message "study.access.access_data_can_leave.study_facility"/></td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_data_can_leave.study_facility/>
                      </td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_biological_samples_can_leave.study_facility/>
                      </td>
                    </tr>
                    <tr>
                      <td><@message "study.access.access_data_can_leave.country"/></td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_data_can_leave.country/>
                      </td>
                      <td class="col-sm-2">
                          <@yesnoToIcon value=study.model.access_biological_samples_can_leave.country/>
                      </td>
                    </tr>
                    </tbody>
                  </table>

                    <#if study.model.access_special_conditions_to_leave??>
                      <p class="text-muted">${localize(study.model.access_special_conditions_to_leave)}</p>
                    </#if>
                </div>
              </#if>
              <#if study.model.access_fees?? && study.model.access_fees == true>
                <div class="tab-pane fade" id="access-fees" role="tabpanel" aria-labelledby="access-fees-tab">

                  <p><b><@message "study.access.access_data_sharing_cost.cost-title"/></b></p>

                  <table class="table table-sm table-striped">
                    <tbody>
                    <tr>
                      <td><@message "study_taxonomy.vocabulary.access_data.title"/></td>
                      <td class="col-sm-6">
                          <@costToLabel value=study.model.access_data_sharing_cost.data/>
                      </td>
                    </tr>
                    <tr>
                      <td><@message "study_taxonomy.vocabulary.access_bio_samples.title"/></td>
                      <td>
                          <@costToLabel value=study.model.access_data_sharing_cost.biological_samples/>
                      </td>
                    </tr>
                    </tbody>
                  </table>

                    <#if study.model.access_cost_additional_information??>
                      <p class="text-muted">${localize(study.model.access_cost_additional_information)}</p>
                    </#if>

                  <p><b><@message "study.access.access_data_sharing_cost.cost-reduction-title"/></b></p>

                  <table class="table table-sm table-striped">
                    <tbody>
                    <tr>
                      <td><@message "study_taxonomy.vocabulary.access_data.title"/></td>
                      <td class="col-sm-6">
                          <@yesnoToIcon value=study.model.access_cost_reduction_consideration.data/>
                      </td>
                    </tr>
                    <tr>
                      <td><@message "study_taxonomy.vocabulary.access_bio_samples.title"/></td>
                      <td>
                          <@yesnoToIcon value=study.model.access_cost_reduction_consideration.bio_samples/>
                      </td>
                    </tr>
                    </tbody>
                  </table>

                    <#if study.model.access_cost_reduction_consideration_specification??>
                      <p class="text-muted">${localize(study.model.access_cost_reduction_consideration_specification)}</p>
                    </#if>
                </div>
              </#if>
            </div>
          </div>
          <!-- /.card -->
        </div>
      <#else>
        <@studyAccessInfo study=study/>
      </#if>

      <#if study.model.access_supplementary_info??>
        <p><b><@message "suppl-info"/></b></p>
        <div>
            ${localize(study.model.access_supplementary_info)}
        </div>
      </#if>
    </div>
    <div class="card-footer">
      <div class="row">
        <div class="col-md-3"><small><i class="fas fa-check"></i> <@message "yes"/></small></div>
        <div class="col-md-3"><small><i class="fas fa-times"></i> <@message "no"/></small></div>
        <div class="col-md-3"><small><i class="fas fa-minus"></i> <@message "na"/></small></div>
        <div class="col-md-3"><small><i class="fas fa-question"></i> <@message "dont-know"/></small></div>
      </div>
    </div>
  </div>
</#macro>

<#macro studyAccessInfo study>
  <p><b><@message "study.access.access_external_researchers_permitted_foreseen.title"/></b></p>

  <table class="table table-sm table-striped mb-2">
    <tbody>
      <tr>
        <td><@message "study_taxonomy.vocabulary.access_data.title"/></td>
        <td>
          <#if study.model.access??>
            <@yesnoToIcon value=study.model.access.access_data/>
          </#if>
        </td>
      </tr>
      <tr>
        <td><@message "study_taxonomy.vocabulary.access_bio_samples.title"/></td>
        <td>
          <#if study.model.access??>
            <@yesnoToIcon value=study.model.access.access_bio_samples/>
          </#if>
        </td>
      </tr>
      <tr>
        <td><@message "study_taxonomy.vocabulary.access_other.title"/></td>
        <td>
          <#if study.model.access??>
          <@yesnoToIcon value=study.model.access.access_other/>
            <#if study.model.otherAccess??>
              <div>
                ${localize(study.model.otherAccess)}
              </div>
            </#if>
          </#if>
        </td>
      </tr>
    </tbody>
  </table>

  <#if study.model.access_info_location?? && study.model.access_info_location?size != 0>
    <p><b><@message "study.access.external_researchers_obtaining_study_data_bio_info.title"/></p></b></p>

    <#if study.model.access_info_location?seq_contains("study_website")>
      <p>
          <@message "study.access.external_researchers_obtaining_study_data_bio_info.access_info_location.study_website"/>
          <#if study.model.website??>
            : <a href="${study.model.website}" target="_blank">${study.model.website?truncate(50, "...")}</a>
          </#if>
      </p>
    </#if>

    <#if study.model.access_info_location?seq_contains("study_representative")>
      <p>
        <a href="#" data-toggle="modal" data-target="#modal-rep">
            <@message "study.access.external_researchers_obtaining_study_data_bio_info.access_info_location.study_representative"/>
          <i class="fas fa-info-circle"></i>
        </a>
      <div class="modal fade" id="modal-rep">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h4 class="modal-title">Representative</h4>
              <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                <span aria-hidden="true">×</span>
              </button>
            </div>
            <div class="modal-body">
              <dl class="row">
                <dt class="col-4"><@message "contact.name"/></dt>
                <dd class="col-8">${study.model.access_info_representative.title!""} ${study.model.access_info_representative.name!""}</dd>
                <dt class="col-4"><@message "contact.institution"/></dt>
                <dd class="col-8">${study.model.access_info_representative.institution!""}</dd>
                <dt class="col-4"><@message "contact.email"/></dt>
                <dd class="col-8">${study.model.access_info_representative.email!""}</dd>
                <dt class="col-4"><@message "contact.phone"/></dt>
                <dd class="col-8">${study.model.access_info_representative.phone!""}</dd>
              </dl>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
            </div>
          </div>
          <!-- /.modal-content -->
        </div>
        <!-- /.modal-dialog -->
      </div>
      </p>
    </#if>
  </#if>

</#macro>

<!-- Yes-no etc. coded answers -->
<#macro yesnoToIcon value="">
  <#if value??>
    <#if value == "yes">
      <i class="fas fa-check"></i>
    <#elseif value == "no">
      <i class="fas fa-times"></i>
    <#elseif value == "na">
      <i class="fas fa-minus"></i>
    <#elseif value == "dk">
      <i class="fas fa-question"></i>
    <#else>
      <@message value/>
    </#if>
  <#else>
    <i class="fas fa-times"></i>
  </#if>
</#macro>

<!-- Cost selection to labels -->
<#macro costToLabel value>
  <#if value??>
    <#if value == "na">
      <@message "global.not-applicable"/>
    <#else>
      <#assign label="study.access.access_data_sharing_cost." + value/>
      <@message label/>
    </#if>
  </#if>
</#macro>

<!-- Files -->
<#macro studyFilesBrowser study>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "files"/></h3>
    </div>
    <div class="card-body">

      <div id="study-files-app">
        <@filesBrowser/>
      </div>

    </div>
  </div>
</#macro>

<!-- Variables classifications -->
<#macro variablesClassifications study>
  <img id="loadingClassifications" src="${assetsPath}/images/loading.gif">
  <div id="classificationsContainer" style="display: none;" class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "variables-classifications"/></h3>
    </div>
    <div class="card-body">
      <div>
        <div class="mb-4">
          <select id="select-bucket" class="form-control select2">
            <option value="_all" selected><#if type == "Individual"><@message "all-dces"/><#else><@message "all-populations"/></#if></option>
          </select>
        </div>
        <div id="chartsContainer"></div>
      </div>
      <div id="noVariablesClassifications" style="display: none">
        <span class="text-muted"><@message "no-variables-classifications"/></span>
      </div>
    </div>
  </div>
</#macro>
