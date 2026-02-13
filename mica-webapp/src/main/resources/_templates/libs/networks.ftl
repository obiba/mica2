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
    <div class="col-3 ms-auto">
      <a href="${contextPath}/search#lists?type=networks" class="btn btn-sm btn-primary float-end">
        <@message "global.search"/> <i class="fa-solid fa-search"></i>
      </a>
    </div>
  </div>

  <div class="row">
    <div class="col-12">
      <div class="d-inline-flex float-end mt-3 mb-3">
        <sorting @sort-update="onSortUpdate" :initial-choice="initialSort" :options-translations="sortOptionsTranslations"></sorting>
        <span class="ms-2">
          <select class="form-select" id="obiba-page-size-selector-top"></select>
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
                      <b>{{localizeString(network.name)}}</b>
                    </a>
                  </h4>
                  <span class="marked"><small :inner-html.prop="markdown(ellipsis(localizeString(network.description), 300, ('${contextPath}/network/' + network.id)))"></small></span>
                </div>
                <div class="col-3 mx-auto my-auto" v-if="network.logo">
                  <a v-bind:href="'${contextPath}/network/' + network.id" class="text-decoration-none text-center">
                    <img class="img-fluid" style="max-height: 10em;" v-bind:alt="concat(localizeString(network.acronym), ' logo')" v-bind:src="'${contextPath}/ws/network/' + network.id + '/file/' + network.logo.id + '/_download'"/>
                  </a>
                </div>
              </div>
            </div>
            <div class="card-footer py-1">
              <div v-if="hasStats(network.countStats)" class="row pt-1 row-cols-5">
                <stat-item
                        v-bind:count="network.countStats.individualStudies"
                        v-bind:singular="translate('individual-study')"
                        v-bind:plural="translate('individual-studies')"
                        v-bind:url="individualStudies(network.id)">
                </stat-item>
                <#if config.studyDatasetEnabled>
                  <stat-item
                          v-bind:count="network.countStats.studiesWithVariables"
                          v-bind:singular="translate('study-with-variables')"
                          v-bind:plural="translate('studies-with-variables')"
                          v-bind:url="individualStudiesWithVariables(network.id)">
                  </stat-item>
                  <stat-item
                          v-bind:count="network.countStats.studyVariables"
                          v-bind:singular="translate('study-variable')"
                          v-bind:plural="translate('study-variables')"
                          v-bind:url="individualStudyVariables(network.id)">
                  </stat-item>
                </#if>
                <stat-item
                        v-bind:count="network.countStats.harmonizationStudies"
                        v-bind:singular="translate('harmonization-study')"
                        v-bind:plural="translate('harmonization-studies')"
                        v-bind:url="harmonizationStudies(network.id)">
                </stat-item>
                <#if config.harmonizationDatasetEnabled>
                  <stat-item
                          v-bind:count="network.countStats.dataschemaVariables"
                          v-bind:singular="translate('harmonization-study-variable')"
                          v-bind:plural="translate('harmonization-study-variables')"
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

  <div class="d-inline-flex pt-0 ms-auto">
    <span>
      <select class="form-select" id="obiba-page-size-selector-bottom"></select>
    </span>
    <nav id="obiba-pagination-bottom" aria-label="Bottom pagination" class="ms-2 mt-0">
      <ul class="pagination"></ul>
    </nav>
  </div>

</div>
</#macro>
