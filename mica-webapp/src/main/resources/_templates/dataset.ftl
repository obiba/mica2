<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">
<#include "models/dataset.ftl">
<#include "models/harmonization.ftl">
<#include "models/files.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(dataset.acronym)}</title>
</head>
<body id="${type?lower_case}-dataset-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-dataset") title=localize(dataset.acronym) subtitle=localize(dataset.name) breadcrumb=[["${contextPath}/", "home"], ["${contextPath}/datasets", "datasets"], [localize(dataset.acronym)]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if draft>
          <div class="alert alert-warning" role="alert">
            <i class="icon fas fa-exclamation-triangle"></i> <@messageArgs code="viewing-draft-version" args=["/dataset/${dataset.id}"]/>
          </div>
        </#if>

        <!-- General Information content -->
        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-body">
                <div class="row">
                  <div class="col-lg-12">
                    <h3 class="mb-4">${localize(dataset.name)}</h3>
                  </div>
                </div>
                <div class="row">
                  <div class="col-md-3 col-sm-6 col-12">
                    <p class="text-muted text-center">
                      <#if type == "Collected">
                        <i class="${datasetIcon} fa-4x"></i>
                      <#else >
                        <i class="${harmoDatasetIcon} fa-4x"></i>
                      </#if>
                    </p>
                  </div>

                  <#if config.networkEnabled && !config.singleNetworkEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-info">
                          <a href="${contextPath}/search#lists?type=networks&query=dataset(in(Mica_dataset.id,${dataset.id}))">
                            <i class="${networkIcon}"></i>
                          </a>
                        </span>
                        <div class="info-box-content">
                          <span class="info-box-text"><@message "networks"/></span>
                          <span class="info-box-number" id="network-hits">-</span>
                        </div>
                        <!-- /.info-box-content -->
                      </div>
                    </div>
                  </#if>

                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-danger">
                        <a href="${contextPath}/search#lists?type=variables&query=dataset(in(Mica_dataset.id,${dataset.id}))">
                          <i class="${variableIcon}"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text"><@message "variables"/></span>
                        <span class="info-box-number" id="variable-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                </div>

                <div class="card-text marked mt-3">
                  <template>${localize(dataset.description)}</template>
                </div>
              </div>
                <#if study??>
                  <div class="card-footer">
                    <@message "associated-study"/>
                    <a class="btn btn-success ml-2" href="${contextPath}/study/${study.id}">
                      <i class="${studyIcon}"></i> ${localize(study.acronym)}
                    </a>
                    <#if showDatasetContingencyLink>
                      <a class="btn btn-primary float-right ml-2" href="${contextPath}/dataset-crosstab/${dataset.id}">
                        <i class="fas fa-cog"></i> <@message "dataset.crosstab.title"/>
                      </a>
                    </#if>
                    <#if cartEnabled>
                      <div id="cart-add" class="float-right">
                        <#if user??>
                          <button type="button" class="btn btn-link" onclick="onVariablesCartAdd('${dataset.id}')">
                            <@message "sets.cart.add-to-cart"/> <i class="fas fa-cart-plus"></i>
                          </button>
                        <#else>
                          <button type="button" class="btn btn-link" onclick="window.location.href='${contextPath}/signin?redirect=${contextPath}/dataset/${dataset.id}';">
                            <@message "sets.cart.add-to-cart"/> <i class="fas fa-cart-plus"></i>
                          </button>
                        </#if>
                      </div>
                    </#if>
                  </div>
                </#if>
            </div>
          </div>
        </div>

        <!-- Population and DCE content -->
        <div class="row">
          <#if population??>
            <div class="col-lg-6">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "population"/></h3>
                </div>
                <div class="card-body">
                  <h5>${localize(population.name)}</h5>
                  <div class="marked"><template>${localize(population.description)}</template></div>
                  <@populationDialog id=population.id population=population></@populationDialog>
                </div>
                <div class="card-footer">
                  <a href="#" data-toggle="modal" data-target="#modal-${population.id}"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
            </div>
          </#if>
          <#if dce??>
            <div class="col-lg-6">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "data-collection-event"/></h3>
                </div>
                <div class="card-body">
                  <h5>${localize(dce.name)}</h5>
                  <div class="marked"><template>${localize(dce.description)}</template></div>
                  <#assign dceId="${population.id}-${dce.id}">
                  <@dceDialog id=dceId dce=dce></@dceDialog>
                </div>
                <div class="card-footer">
                  <a href="#" data-toggle="modal" data-target="#modal-${dceId}"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
            </div>
          <#elseif (studyTables?? && studyTables?size != 0) || (harmonizationTables?? && harmonizationTables?size != 0)>
            <div class="col-lg-6">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "studies-included"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <#if studyTables?? && studyTables?size != 0>
                    <h5><@message "individual-studies"/></h5>
                    <div class="table-responsive">
                      <table class="table table-striped mb-3">
                        <thead>
                        <tr>
                          <th><@message "study"/></th>
                          <th><@message "population"/></th>
                          <th><@message "data-collection-event"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list studyTables as table>
                          <tr>
                            <td>
                              <a href="${contextPath}/study/${table.study.id}">
                                ${localize(table.study.acronym)}
                              </a>
                            </td>
                            <td>
                              <#assign popId="${table.study.id}-${table.population.id}">
                              <@populationDialog id=popId population=table.population></@populationDialog>
                              <a href="#" data-toggle="modal" data-target="#modal-${popId}">
                                ${localize(table.population.name)}
                              </a>
                            </td>
                            <td>
                              <#assign dceId="${table.study.id}-${table.population.id}-${table.dce.id}">
                              <@dceDialog id=dceId dce=table.dce></@dceDialog>
                              <a href="#" data-toggle="modal" data-target="#modal-${dceId}">
                                ${localize(table.dce.name)}
                              </a>
                            </td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  </#if>
                    <#if harmonizationTables?? && harmonizationTables?size != 0>
                      <h5><@message "harmonization-studies"/></h5>
                      <div class="table-responsive">
                        <table class="table table-striped">
                          <thead>
                          <tr>
                            <th><@message "study"/></th>
                            <th><@message "population"/></th>
                          </tr>
                          </thead>
                          <tbody>
                          <#list harmonizationTables as table>
                            <tr>
                              <td>
                                <a href="${contextPath}/study/${table.study.id}">
                                  ${localize(table.study.acronym)}
                                </a>
                              </td>
                              <td>
                                <#assign popId="${table.study.id}-${table.population.id}">
                                <@populationDialog id=popId population=table.population></@populationDialog>
                                <a href="#" data-toggle="modal" data-target="#modal-${popId}">
                                  ${localize(table.population.name)}
                                </a>
                              </td>
                            </tr>
                          </#list>
                          </tbody>
                        </table>
                      </div>
                    </#if>
                </div>
              </div>
            </div>
          </#if>
        </div>

        <!-- Dataset model -->
        <@datasetModel dataset=dataset type=type/>

        <!-- Harmonization content -->
        <#if type == "Harmonized">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "harmonization"/></h3>
            </div>
            <div class="card-body">
              <div class="row">
                <div class="col-lg-8 col-sm-6">
                  <@harmonizationLegend/>
                </div>
                <div class="col-lg-4 col-sm-6">
                  <a href="${contextPath}/ws/harmonized-dataset/${dataset.id}/variables/harmonizations/_export" class="btn btn-primary float-right mb-3">
                    <i class="fas fa-download"></i> <@message "download"/>
                  </a>
                </div>
              </div>
              <div id="loadingSummary" class="spinner-border spinner-border-sm" role="status"></div>
              <div class="table-responsive">
                <table id="harmonizedTable" class="table table-striped">
                  <thead>
                    <tr>
                      <th><@message "variable"/></th>
                      <#list allTables as table>
                        <th>
                          <a href="${contextPath}/study/${table.studyId}">${localize(allStudies[table.studyId].acronym)}</a>
                          <#if table.name??>${localize(table.name)}</#if>
                          <#if table.description??><i class="fas fa-info-circle" title="${localize(table.description)}"></i></#if>
                        </th>
                      </#list>
                    </tr>
                  </thead>
                  <tbody></tbody>
                </table>
              </div>
            </div>
          </div>
        </#if>

        <!-- Files -->
        <#if showDatasetFiles>
          <@datasetFilesBrowser dataset=dataset/>
        </#if>

        <!-- Variables classifications -->
        <#if datasetVariablesClassificationsTaxonomies?? && datasetVariablesClassificationsTaxonomies?size gt 0>
          <@variablesClassifications dataset=dataset/>
        </#if>

      </div>
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->
  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/dataset-scripts.ftl">

</body>
</html>
