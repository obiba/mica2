<!-- Studies page macros -->

<!-- Studies in table model templates -->
<#macro studyTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
    <#if showTypeColumn>
      <th><@message "type"/></th>
    </#if>
  </tr>
</#macro>

<#macro studyTableRowModel study>
  <tr>
    <td><a href="${contextPath}/study/${study.id}">${localize(study.acronym)}</a></td>
    <td><small>${localize(study.name)}</small></td>
    <td class="marked"><template><small>${localize(study.objectives)?trim?truncate_w(100, "...")}</small></template></td>
    <#if showTypeColumn>
      <td>
        <#if study.class.simpleName == "HarmonizationStudy">
          <@message "harmonization"/>
        <#else>
          <@message "individual"/>
        </#if>
      </td>
    </#if>
  </tr>
</#macro>

<!-- Studies in lines model template -->
<#macro studyLineModel study>
  <div class="col-lg-3 col-sm-12">
    <#if study.logo??>
      <img class="img-fluid" style="max-height: 150px" alt="${localize(study.acronym)} logo" src="${contextPath}/ws/study/${study.id}/file/${study.logo.id}/_download"/>
    <#else >
      <div class="text-black-50 text-center mt-5">
        <i class="<#if type == "Harmonization">${initiativeIcon}<#else>${studyIcon}</#if> fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(study.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(study.name)}</p>
    <div class="marked">
      <template>${localize(study.objectives)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/study/${study.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<#macro studyCardModelStats>
  <a v-if="study.model && study.model.methods" href="javascript:void(0)" style="cursor: initial;" class="btn btn-sm col text-left">
    <span class="h6 pb-0 mb-0 d-block">{{study.model.methods.design | translate}}</span>
    <span class="text-muted"><small><@message "study_taxonomy.vocabulary.methods-design.title"/></small></span>
  </a>
  <a v-if="study.model && study.model.numberOfParticipants" href="javascript:void(0)" style="cursor: initial;" class="btn btn-sm col text-left">
    <span class="h6 pb-0 mb-0 d-block">{{study.model.numberOfParticipants.participant.number | localize-number}}</span>
    <span class="text-muted"><small><@message "study_taxonomy.vocabulary.numberOfParticipants-participant-number.title"/></small></span>
  </a>
</#macro>

<!-- Studies in cards model template -->
<#macro studyCardModel>

  <!-- Macro variables -->
  <#if !type??>
    <#assign className = "Study,HarmonizationStudy">
    <#assign listPageSearchMode = "search">
  <#elseif type == "Harmonization">
    <#assign className = "HarmonizationStudy">
    <#assign listPageSearchMode = "harmonization-search">
  <#else>
    <#assign className = "Study">
    <#assign listPageSearchMode = "individual-search">
  </#if>

<div v-show="loading" class="spinner-border spinner-border-sm" role="status"></div>
<div v-show="!loading && entities && entities.length > 0" v-cloak></div>
<div id="studies-card">

  <div class="row">
    <div class="col-6">
      <typeahead @typing="onType" @select="onSelect" :items="suggestions" :external-text="initialFilter"></typeahead>
    </div>
    <div class="col-3 ms-auto">
      <a href="${contextPath}/${listPageSearchMode}#lists?type=studies&query=study(in(Mica_study.className,(${className})))" class="btn btn-sm btn-primary float-right">
        <@message "global.search"/> <i class="fa-solid fa-search"></i>
      </a>
    </div>
  </div>

  <div class="row">
    <div class="col-12">
      <div class="d-inline-flex float-right mt-3 mb-3">
        <sorting @sort-update="onSortUpdate" :initial-choice="initialSort" :options-translations="sortOptionsTranslations"></sorting>
        <span class="ms-2">
          <select class="custom-select" id="obiba-page-size-selector-top"></select>
        </span>
        <nav id="obiba-pagination-top" aria-label="Top pagination" class="ms-2 mt-0">
          <ul class="pagination mb-0"></ul>
        </nav>
      </div>
    </div>
  </div>

  <div class="pb-0">
    <div class="tab-pane">
      <div class="row d-flex align-items-stretch">
        <div class="col-md-12 col-lg-6 d-flex align-items-stretch" v-for="study in entities" v-bind:key="study.id">
          <div v-if="study.id === ''" class="card w-100">
            <div class="card-body pt-0 bg-light"></div>
          </div>
          <div v-else class="card w-100">
            <div class="card-body">
              <div class="row h-100">
                <div class="col-xs-12 col">
                  <h4 class="lead">
                    <a v-bind:href="'${contextPath}/study/' + study.id" class="mt-2">
                      <b>{{study.name | localize-string}}</b>
                    </a>
                  </h4>
                  <span class="marked"><small :inner-html.prop="study.objectives | localize-string | ellipsis(300, ('${contextPath}/study/' + study.id)) | markdown"></small></span>
                </div>
                <div class="col-3 mx-auto my-auto" v-if="study.logo">
                  <a v-bind:href="'${contextPath}/study/' + study.id" class="text-decoration-none text-center">
                    <img class="img-fluid" style="max-height: 10em;" v-bind:alt="study.acronym | localize-string | concat(' logo')" v-bind:src="'${contextPath}/ws/study/' + study.id + '/file/' + study.logo.id + '/_download'"/>
                  </a>
                </div>
              </div>
            </div>
            <div class="card-footer py-1">
              <div class="row pt-1 row-cols-4">
                <template v-if="hasStats(study)">
                  <@studyCardModelStats/>
                  <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                    <dataset-stat-item
                            v-bind:type="study.studyResourcePath"
                            v-bind:stats="study.countStats">
                    </dataset-stat-item>
                    <variable-stat-item
                            v-bind:url="variablesUrl(study)"
                            v-bind:type="study.studyResourcePath"
                            v-bind:stats="study.countStats">
                    </variable-stat-item>
                  <#else>
                    <a href="javascript:void(0)" class="btn btn-sm" style="cursor: initial; opacity: 0">
                      <span class="h6 pb-0 mb-0 d-block">0</span>
                      <span class="text-muted"><small><@message "analysis.empty"/></small></span>
                    </a>
                  </#if>
                </template>
                <template v-else>
                  <!-- HACK used 'studiesWithVariables' with opacity ZERO to have the same height as the longest stat item -->
                  <a href="javascript:void(0)" class="btn btn-sm" style="cursor: initial; opacity: 0">
                    <span class="h6 pb-0 mb-0 d-block">0</span>
                    <span class="text-muted"><small><@message "analysis.empty"/></small></span>
                  </a>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-inline-flex pt-0 ms-auto">
    <span>
      <select class="custom-select" id="obiba-page-size-selector-bottom"></select>
    </span>
    <nav id="obiba-pagination-bottom" aria-label="Bottom pagination" class="ms-2 mt-0">
      <ul class="pagination"></ul>
    </nav>
  </div>

</div>
</#macro>
