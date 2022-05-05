<!-- Networks page macros -->

<!-- Networks in table model templates -->
<#macro networkTableHeadModel>
  <tr>
    <th><@message "acronym"/></th>
    <th><@message "name"/></th>
    <th><@message "description"/></th>
  </tr>
</#macro>

<#macro networkTableRowModel network>
  <tr>
    <td><a href="${contextPath}/network/${network.id}">${localize(network.acronym)}</a></td>
    <td><small>${localize(network.name)}</small></td>
    <td class="marked"><template><small>${localize(network.description)?trim?truncate_w(100, "...")}</small></template></td>
  </tr>
</#macro>

<!-- Networks in lines model template -->
<#macro networkLineModel network>
  <div class="col-lg-3 col-sm-12">
    <#if network.logo??>
      <img class="img-fluid" style="max-height: 150px" alt="${localize(network.acronym)} logo" src="${contextPath}/ws/network/${network.id}/file/${network.logo.id}/_download"/>
    <#else >
      <div class="text-black-50 text-center mt-5">
        <i class="${networkIcon} fa-3x"></i>
      </div>
    </#if>
  </div>
  <div class="col-lg-9 col-sm-12">
    <h2 class="lead"><b>${localize(network.acronym)}</b></h2>
    <p class="text-muted text-sm">${localize(network.name)}</p>
    <div class="marked">
      <template>${localize(network.description)?trim?truncate_w(200, "...")}</template>
    </div>
    <div class="mt-2">
      <a href="${contextPath}/network/${network.id}" class="btn btn-sm btn-outline-info">
        <@message "global.read-more"/>
      </a>
    </div>
  </div>
</#macro>

<!-- Networks in cards model template -->
<#macro networkCardModel>
<div v-show="loading" class="spinner-border spinner-border-sm" role="status"></div>
<div v-show="!loading && entities && entities.length > 0" v-cloak></div>
<div id="networks-card">

  <div class="row">
    <div class="col-6">
      <typeahead @typing="onType" @select="onSelect" :items="suggestions" :external-text="initialFilter"></typeahead>
    </div>
    <div class="col-3 ml-auto">
      <a href="${contextPath}/search#lists?type=networks" class="btn btn-sm btn-primary float-right">
        <@message "global.search"/> <i class="fas fa-search"></i>
      </a>
    </div>
  </div>

  <div class="row">
    <div class="col-12">
      <div class="d-inline-flex float-right mt-3 mb-3">
        <sorting @sort-update="onSortUpdate" :initial-choice="initialSort" :options-translations="sortOptionsTranslations"></sorting>
        <span class="ml-2">
          <select class="custom-select" id="obiba-page-size-selector-top"></select>
        </span>
        <nav id="obiba-pagination-top" aria-label="Top pagination" class="ml-2 mt-0">
          <ul class="pagination mb-0"></ul>
        </nav>
      </div>
    </div>
  </div>

  <div class="pb-0">
    <div class="tab-pane">
      <div class="row d-flex align-items-stretch">
        <div class="col-md-12 col-lg-6 d-flex align-items-stretch" v-for="network in entities" v-bind:key="network.id">
          <div v-if="network.id === ''" class="card w-100">
            <div class="card-body pt-0 bg-light">
            </div>
          </div>
          <div v-else class="card w-100">
            <div class="card-body">
              <div class="row h-100">
                <div class="col-xs-12 col">
                  <h4 class="lead">
                    <a v-bind:href="'${contextPath}/network/' + network.id" class="mt-2">
                      <b>{{network.name | localize-string}}</b>
                    </a>
                  </h4>
                  <span class="marked"><small :inner-html.prop="network.description | localize-string | ellipsis(300, ('${contextPath}/network/' + network.id)) | markdown"></small></span>
                </div>
                <div class="col-3 mx-auto my-auto" v-if="network.logo">
                  <a v-bind:href="'${contextPath}/network/' + network.id" class="text-decoration-none text-center">
                    <img class="img-fluid" style="max-height: 10em;" v-bind:alt="network.acronym | localize-string | concat(' logo')" v-bind:src="'${contextPath}/ws/network/' + network.id + '/file/' + network.logo.id + '/_download'"/>
                  </a>
                </div>
              </div>
            </div>
            <div class="card-footer py-1">
              <div v-if="hasStats(network['obiba.mica.CountStatsDto.networkCountStats'])" class="row pt-1 row-cols-5">
                <stat-item
                        v-bind:count="network['obiba.mica.CountStatsDto.networkCountStats'].individualStudies"
                        v-bind:singular="'individual-study' | translate"
                        v-bind:plural="'individual-studies' | translate"
                        v-bind:url="individualStudies(network.id)">
                </stat-item>
                <#if config.studyDatasetEnabled>
                  <stat-item
                          v-bind:count="network['obiba.mica.CountStatsDto.networkCountStats'].studiesWithVariables"
                          v-bind:singular="'study-with-variables' | translate"
                          v-bind:plural="'studies-with-variables' | translate"
                          v-bind:url="individualStudiesWithVariables(network.id)">
                  </stat-item>
                  <stat-item
                          v-bind:count="network['obiba.mica.CountStatsDto.networkCountStats'].studyVariables"
                          v-bind:singular="'study-variable' | translate"
                          v-bind:plural="'study-variables' | translate"
                          v-bind:url="individualStudyVariables(network.id)">
                  </stat-item>
                </#if>
                <stat-item
                        v-bind:count="network['obiba.mica.CountStatsDto.networkCountStats'].harmonizationStudies"
                        v-bind:singular="'harmonization-study' | translate"
                        v-bind:plural="'harmonization-studies' | translate"
                        v-bind:url="harmonizationStudies(network.id)">
                </stat-item>
                <#if config.harmonizationDatasetEnabled>
                  <stat-item
                          v-bind:count="network['obiba.mica.CountStatsDto.networkCountStats'].dataschemaVariables"
                          v-bind:singular="'harmonization-study-variable' | translate"
                          v-bind:plural="'harmonization-study-variables' | translate"
                          v-bind:url="harmonizationStudyVariables(network.id)">>
                  </stat-item>
                </#if>
              </div>
              <div v-else class="row pt-1 row-cols-5" >
                <!-- HACK used 'studiesWithVariables' with opacity ZERO to have the same height as the longest stat item -->
                <a href="javascript:void(0)" class="btn btn-sm" style="cursor: initial; opacity: 0">
                  <span class="h6 pb-0 mb-0 d-block">0</span>
                  <span class="text-muted"><small><@message "analysis.empty"/></small></span>
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="d-inline-flex pt-0 ml-auto">
    <span>
      <select class="custom-select" id="obiba-page-size-selector-bottom"></select>
    </span>
    <nav id="obiba-pagination-bottom" aria-label="Bottom pagination" class="ml-2 mt-0">
      <ul class="pagination"></ul>
    </nav>
  </div>

</div>
</#macro>
