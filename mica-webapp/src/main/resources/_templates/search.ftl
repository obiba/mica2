<!-- Macros -->
<#include "models/search.ftl">

<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "search"/></title>
</head>
<body id="search-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
<!-- Site wrapper -->
<div id="search-application" class="wrapper" :class="{'harmoMode': currentStudyTypeSelection && currentStudyTypeSelection.harmonization}" v-cloak>

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="${portalLink}" class="brand-link bg-white">
      <img src="${brandImageSrc}"
           alt="Logo"
           class="brand-image ${brandImageClass}"
           style="opacity: .8">
      <span class="brand-text ${brandTextClass}">
        <#if brandTextEnabled>
          ${config.name!""}
          <#else>&nbsp;
        </#if>
      </span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
      <!-- Sidebar user (optional) -->
      <div class="user-panel mt-3 pb-3 mb-3 d-flex">
        <div class="info">
          <a href="#" class="d-block"><@message "search-criteria"/></a>
        </div>
      </div>

      <!-- Sidebar Menu -->
      <nav class="mt-2">

        <ul data-widget="treeview" role="menu" data-accordion="false" class="nav nav-pills nav-sidebar flex-column"></ul>

        <search-criteria :study-type-selection="currentStudyTypeSelection"></search-criteria>
      </nav>
      <!-- /.sidebar-menu -->
    </div>
    <!-- /.sidebar -->
  </aside>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1><@message "search"/></h1>
          </div>
          <div class="col-sm-6">

          </div>
        </div>
      </div><!-- /.container-fluid -->
    </section>

    <!-- Main content -->
    <section class="content">

      <@searchInfo/>

      <!-- Query box -->
      <div id="query-builder" class="card card-info card-outline">
        <div class="card-header">
          <h3 class="card-title"><@message "query"/></h3>
          <div class="card-tools">
            <a class="btn btn-secondary btn-sm ml-2" href="javascript:void(0)" @click="onSearchModeToggle" v-cloak>
              <span v-if="advanceQueryMode" title="<@message "search.basic-help"/>"><@message "search-basic-mode"/></span>
              <span v-else title="<@message "search.advanced-help"/>"><@message "search-advanced-mode"/></span>
            </a>
            <#if showCopyQuery>
              <div class="btn-group ml-2">
                <button type="button" class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false"><@message "global.copy-query"/></button>
                <ul class="dropdown-menu dropdown-menu-right">
                  <li class="pr-3 pl-3 pt-3">
                    <div class="input-group mb-2">
                      <input v-model="queryToCopy" disabled type="text" class="form-control" style="width: 300px;">
                      <div class="input-group-append">
                        <button class="btn btn-outline-secondary" type="button" @click="onCopyQuery"
                                title="<@message "global.copy-to-clipboard"/>">
                          <i class="fas fa-copy"></i></button>
                      </div>
                    </div>
                    <div class="text-muted">
                      <small><@message "search.query-copy-help"/></small>
                    </div>
                  </li>
                </ul>
              </div>
            </#if>
            <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip"
                    title="<@message "collapse"/>">
              <i class="fas fa-minus"></i></button>
          </div>
        </div>
        <div class="card-body">
          <div>

            <div class="modal fade" id="taxonomy-modal">
              <div class="modal-dialog modal-xl" role="document">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title"><@message "select-criteria"/></h5>
                    <button type="button" class="btn btn-sm btn-success" data-dismiss="modal"><span aria-hidden="true"><@message "display-results"/></span></button>
                  </div>
                  <div class="modal-body" v-if="selectedTarget">
                    <rql-panel v-bind:target="selectedTarget" v-bind:taxonomy="selectedTaxonomy" v-bind:query="selectedQuery" @update-query="onQueryUpdate" @remove-query="onQueryRemove"></rql-panel>
                  </div>
                </div>
              </div>
            </div>
            <!-- /.modal -->

            <div class="text-muted" v-show="noQueries">
              <@message "no-query"/>
            </div>

            <!-- Query Builder -->
            <rql-query-builder v-for="(query, target) in queries" v-bind:key="target" v-bind:target="target" v-bind:taxonomy="getTaxonomyForTarget(target)" v-bind:query="query" v-bind:advanced-mode="advanceQueryMode" @update-node="onNodeUpdate" @update-query="onQueryUpdate" @remove-query="onQueryRemove"></rql-query-builder>
          </div>
        </div>
        <!-- /.card-body -->
      </div>
      <!-- /.card -->

      <!-- Results box -->
      <div class="row" id="results-tab-content">
        <div class="col-12">
          <!-- Custom Tabs -->
          <div class="card">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "results"/></h3>
              <#if downloadQueryEnabled>
                <div class="mt-2 pt-1">
                  <a id="download-query" href="javascript:void(0)" class="btn btn-sm btn-info ml-2" @click="onDownloadQueryResult"><i class="fas fa-download"></i> <@message "download"/></a>
                </div>
              </#if>
              <ul id="search-tabs" class="nav nav-pills ml-auto p-2">
                <#if searchListDisplay>
                  <li class="nav-item"><a id="lists-tab" class="nav-link active" href="#tab_lists" data-toggle="tab" @click="onSelectSearch()"><@message "lists"/></a></li>
                </#if>
                <#if searchCoverageDisplay>
                  <li class="nav-item"><a id="coverage-tab" class="nav-link" href="#tab_coverage" data-toggle="tab" @click="onSelectCoverage()"><@message "coverage"/></a></li>
                </#if>
                <#if searchGraphicsDisplay>
                  <li v-if="currentStudyTypeSelection && !currentStudyTypeSelection.harmonization" class="nav-item"><a id="graphics-tab" class="nav-link" href="#tab_graphics" data-toggle="tab" @click="onSelectGraphics()"><@message "graphics"/></a></li>
                </#if>
              </ul>
            </div><!-- /.card-header -->
            <div class="card-body">
              <div class="tab-content">

                <div class="tab-pane active" id="tab_lists">
                  <p class="text-muted mt-3">
                    <@message "results-lists-text"/>
                  </p>

                  <div class="row">
                    <div class="mt-3 col clearfix" v-cloak>
                      <ul class="nav nav-pills float-left" id="results-tab" role="tablist">
                          <#if searchVariableListDisplay>
                            <li class="nav-item">
                              <a class="nav-link active" id="variables-tab" data-toggle="pill" href="#variables" role="tab" @click="onSelectResult('variables', 'variable')"
                                 aria-controls="variables" aria-selected="true"><@message "variables"/> <span id="variable-count" class="badge badge-light">{{counts.variables}}</span></a>
                            </li>
                          </#if>
                          <#if searchDatasetListDisplay>
                            <li class="nav-item">
                              <a class="nav-link" id="datasets-tab" data-toggle="pill" href="#datasets" role="tab" @click="onSelectResult('datasets', 'dataset')"
                                 aria-controls="datasets" aria-selected="false"><span>{{currentStudyTypeSelection && currentStudyTypeSelection.harmonization ? '<@message "protocols"/>' : '<@message "datasets"/>'}}</span> <span id="dataset-count" class="badge badge-light">{{counts.datasets}}</span></a>
                            </li>
                          </#if>
                          <#if searchStudyListDisplay>
                            <li class="nav-item">
                              <a class="nav-link" id="studies-tab" data-toggle="pill" href="#studies" role="tab" @click="onSelectResult('studies', 'study')"
                                 aria-controls="studies" aria-selected="false"><span>{{currentStudyTypeSelection && currentStudyTypeSelection.harmonization ? '<@message "initiatives"/>' : '<@message "studies"/>'}}</span> <span id="study-count" class="badge badge-light">{{counts.studies}}</span></a>
                            </li>
                          </#if>
                          <#if searchNetworkListDisplay>
                            <li class="nav-item">
                              <a class="nav-link" id="networks-tab" data-toggle="pill" href="#networks" role="tab" @click="onSelectResult('networks', 'network')"
                                 aria-controls="networks" aria-selected="false"><@message "networks"/> <span id="network-count" class="badge badge-light">{{counts.networks}}</span></a>
                            </li>
                          </#if>
                      </ul>
                      <div class="float-right mt-1">
                          <#if exportStudiesQueryEnabled>
                            <button id="export-studies" type="button" class="btn btn-info btn-sm" v-if="isStudiesToolsVisible" @click="onDownloadExportQueryResult">
                              <i class="fas fa-download"></i> <@message "export"/></span>
                            </button>
                          </#if>
                          <#if studiesCompareEnabled>
                            <button id="compare-studies" type="button" class="btn btn-info btn-sm ml-2" v-if="isStudiesToolsVisible" @click="onCompare">
                              <i class="fas fa-grip-lines-vertical"></i> <@message "compare"/></span>
                            </button>
                          </#if>
                          <#if exportNetworksQueryEnabled>
                            <button id="export-networks" type="button" class="btn btn-info btn-sm" v-if="isNetworksToolsVisible" @click="onDownloadExportQueryResult">
                              <i class="fas fa-download"></i> <@message "export"/></span>
                            </button>
                          </#if>
                          <#if networksCompareEnabled>
                            <button id="compare-networks" type="button" class="btn btn-info btn-sm ml-2" v-if="isNetworksToolsVisible" @click="onCompare">
                              <i class="fas fa-grip-lines-vertical"></i> <@message "compare"/> <span class="badge badge-light studies-selection-count"></span>
                            </button>
                          </#if>
                          <#if cartEnabled>
                              <#if user??>
                                  <#if variablesCartEnabled>
                                      <#if listsEnabled>
                                        <div class="btn-group" v-if="isVariablesToolsVisible">
                                          <button id="cart-add-variables" type="button" class="btn btn-sm btn-success" @click="onAddToCart" title="<@message "sets.cart.add-variables-to-cart"/>">
                                            <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/></button>
                                          <button type="button" class="btn btn-sm btn-success dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"></button>
                                          <div ref="listsDropdownMenu" class="dropdown-menu dropdown-menu-right" style="min-width: 24em;">
                                            <form class="px-3 py-3" v-if="numberOfSetsRemaining > 0">
                                              <div class="form-group mb-0">
                                                <div class="input-group">
                                                  <input type="text" class="form-control" placeholder="<@message "sets.add.modal.create-new"/>" v-model="newVariableSetName" @keyup.enter.prevent.stop="onAddToSet()">
                                                  <div class="input-group-append">
                                                    <button v-bind:class="{ disabled: !newVariableSetName }" class="btn btn-success" type="button" @click="onAddToSet()">
                                                      <i class="fa fa-plus"></i> <@message "global.add"/>
                                                    </button>
                                                  </div>
                                                </div>
                                              </div>
                                            </form>
                                            <div class="dropdown-divider" v-if="variableSets.length > 0 && numberOfSetsRemaining > 0"></div>
                                            <button type="button" class="dropdown-item" v-for="set in variableSets" v-bind:key="set.id" @click="onAddToSet(set.id)">
                                              {{ normalizeSetName(set) }}
                                              <span class="badge badge-light float-right">{{ set.count }}</span>
                                            </button>
                                          </div>
                                        </div>
                                      <#else>
                                        <button id="cart-add-variables" type="button" class="btn btn-sm btn-success" v-if="isVariablesToolsVisible" @click="onAddToCart" title="<@message "sets.cart.add-variables-to-cart"/>">
                                          <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/></button>
                                      </#if>
                                  </#if>
                                  <#if studiesCartEnabled>
                                    <button id="cart-add-studies" type="button" class="btn btn-sm btn-success ml-2" v-if="isStudiesToolsVisible" @click="onAddToCart" title="<@message "sets.cart.add-studies-to-cart"/>">
                                      <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/></button>
                                  </#if>
                                  <#if networksCartEnabled>
                                    <button id="cart-add-networks" type="button" class="btn btn-sm btn-success ml-2" v-if="isNetworksToolsVisible" @click="onAddToCart" title="<@message "sets.cart.add-networks-to-cart"/>">
                                      <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/></button>
                                  </#if>
                              <#else>
                                <a href="${contextPath}/signin?redirect=${contextPath}/search" class="btn btn-sm btn-success" title="<@message "sets.cart.signin-to-add-to-cart"/>">
                                  <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/></a>
                              </#if>
                          </#if>
                      </div>
                    </div>
                  </div>

                  <div id="paging-sorting-container" class="mt-2">
                    <div class="row">
                      <div class="col d-flex align-items-center justify-content-end">
                        <div class="d-inline-flex">
                          <span class="ml-2 mr-1">
                            <select class="custom-select" id="obiba-page-size-selector-top"></select>
                          </span>
                          <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
                            <ul class="pagination mb-0"></ul>
                          </nav>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="mt-3">
                    <div class="tab-content" id="results-tabContent">

                      <div v-show="loading" class="spinner-border spinner-border-sm" role="status"></div>

                      <#if searchVariableListDisplay>
                        <div class="tab-pane fade show active" id="variables" role="tabpanel" aria-labelledby="variables-tab">
                          <p class="text-muted"><@message "results-list-of-variables-text"/></p>
                          <div id="list-variables">
                            <div class="mt-3 text-muted" v-show="!loading && !hasListResult">{{ "no-variable-found" | translate }}</div>
                            <variables-result v-show="!loading && hasListResult"></variables-result>
                          </div>
                        </div>
                      </#if>
                      <#if searchDatasetListDisplay>
                        <div class="tab-pane fade" id="datasets" role="tabpanel" aria-labelledby="datasets-tab">
                          <p class="text-muted"><@message "results-list-of-datasets-text"/></p>
                          <div id="list-datasets">
                            <div class="mt-3 text-muted" v-show="!loading && !hasListResult">{{ "no-dataset-found" | translate }}</div>
                            <datasets-result v-show="!loading && hasListResult"></datasets-result>
                          </div>
                        </div>
                      </#if>
                      <#if searchStudyListDisplay>
                        <div class="tab-pane fade" id="studies" role="tabpanel" aria-labelledby="studies-tab">
                          <p class="text-muted"><@message "results-list-of-studies-text"/></p>
                          <div id="list-studies">
                            <div class="mt-3 text-muted" v-show="!loading && !hasListResult">{{ "no-study-found" | translate }}</div>
                            <studies-result v-show="!loading && hasListResult" :show-checkboxes="studyHasCheckboxes"></studies-result>
                          </div>
                        </div>
                      </#if>
                      <#if searchNetworkListDisplay>
                        <div class="tab-pane fade" id="networks" role="tabpanel" aria-labelledby="networks-tab">
                          <p class="text-muted"><@message "results-list-of-networks-text"/></p>
                          <div id="list-networks">
                            <div class="mt-3 text-muted" v-show="!loading && !hasListResult">{{ "no-network-found" | translate }}</div>
                            <networks-result v-show="!loading && hasListResult" :show-checkboxes="networkHasCheckboxes"></networks-result>
                          </div>
                        </div>
                      </#if>
                    </div>
                  </div>
                </div>
                <!-- /.tab-pane -->

                <#if searchCoverageDisplay>

                  <div class="tab-pane" id="tab_coverage">

                    <div class="mt-3 text-muted" v-show="!hasVariableQuery">{{ "missing-variable-query" | translate }}</div>

                    <div v-show="hasVariableQuery">
                      <div id="coverage">
                        <div class="mt-4 mb-2 clearfix">
                          <ul class="nav nav-pills float-left" role="tablist">
                            <li class="nav-item">
                              <a class="nav-link active"
                                 data-toggle="pill"
                                 id="bucket-study-tab"
                                 href role="tab"
                                 @click="onSelectBucket('study')"
                                 aria-controls="study"
                                 aria-selected="true">{{ bucketTitles.study }}</a>
                            </li>
                            <li class="nav-item">
                              <a class="nav-link"
                                 data-toggle="pill"
                                 id="bucket-dataset-tab"
                                 href role="tab"
                                 @click="onSelectBucket('dataset')"
                                 aria-controls="dataset"
                                 aria-selected="true">{{ bucketTitles.dataset }}</a>
                            </li>
                          </ul>

                          <ul class="nav nav-pills float-right" role="tablist">
                            <li v-if="selectedBucket !==' dataset'" class="mt-auto mb-auto">
                              <div class="custom-control custom-switch">
                                <input type="checkbox"
                                       id="bucket-dce"
                                       v-model="dceChecked"
                                       @change="onSelectBucket(dceChecked ? 'dce' : 'study')"
                                       class="custom-control-input">
                                <label for="bucket-dce" class="custom-control-label">{{ bucketTitles.dce }}</label>
                              </div>
                            </li>
                            <li class="ml-3">
                              <div class="dropleft">
                                <button class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown"><@message "search.filter"/></button>

                                <div class="dropdown-menu">
                                  <button type="button" @click="onFullCoverage()" class="dropdown-item" v-bind:class="{ disabled: !canDoFullCoverage }">
                                      <@message "search.coverage-select.full"/>
                                  </button>
                                  <button type="button" @click="onZeroColumnsToggle()" class="dropdown-item" v-bind:class="{ disabled: !hasCoverageTermsWithZeroHits }">
                                      <@message "search.coverage-without-zeros"/>
                                  </button>
                                </div>
                              </div>
                            </li>
                          </ul>
                        </div>

                        <div v-show="loading" class="spinner-border spinner-border-sm mt-3" role="status"></div>
                        <div class="mt-3 text-muted" v-show="!loading && !hasCoverageResult">{{ "no-coverage-available" | translate }}</div>
                        <coverage-result v-show="!loading && hasCoverageResult" class="mt-2"></coverage-result>
                      </div>
                    </div>

                  </div>
                </#if>
                <!-- /.tab-pane -->

                <#if searchGraphicsDisplay>
                  <div class="tab-pane" id="tab_graphics">
                    <p class="text-muted">
                      <@message "results-graphics-text"/>
                    </p>
                    <div id="graphics">
                      <div v-show="loading" class="spinner-border spinner-border-sm" role="status"></div>
                      <div class="mt-3 text-muted" v-show="!loading && !hasGraphicsResult">{{ "no-graphics-result" | translate }}</div>
                      <graphics-result v-show="!loading && hasGraphicsResult" v-bind:chart-options="chartOptions" :taxonomy="taxonomies['Mica_study']"></graphics-result>
                    </div>
                  </div>
                </#if>
                <!-- /.tab-pane -->
              </div>
              <!-- /.tab-content -->
            </div><!-- /.card-body -->
          </div>
          <!-- ./card -->
        </div>
        <!-- /.col -->
      </div>

      <div class="row">
        <div class="col-12">
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/search-scripts.ftl">

</body>
</html>
