<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">
<#include "models/dataset.ftl">
<#include "models/harmonization.ftl">
<#include "models/files.ftl">

<#if !type??>
    <#assign title = "datasets">
    <#assign searchPageQuery = "study(in(Mica_study.id,${study.id}))">
    <#assign detailsPageSearchMode = "search">
<#elseif type == "Harmonized">
    <#assign title = "harmonized-datasets">
    <#assign searchPageQuery = "study(in(Mica_study.className,HarmonizationStudy)),dataset(in(Mica_dataset.id,${dataset.id}))">
    <#assign detailsPageSearchMode = "harmonization-search">
<#else>
    <#assign title = "collected-datasets">
    <#assign searchPageQuery = "study(in(Mica_study.className,Study)),dataset(in(Mica_dataset.id,${dataset.id}))">
    <#assign detailsPageSearchMode = "individual-search">
</#if>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(dataset.acronym)}</title>
</head>
<body id="${type?lower_case}-dataset-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-dataset") title=localize(dataset.acronym) subtitle=localize(dataset.name) breadcrumb=[["${contextPath}/", "home"], ["${contextPath}/${title}", "${title}"], [localize(dataset.acronym)]]/>
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
                          <a href="${contextPath}/${detailsPageSearchMode}#lists?type=networks&query=${searchPageQuery}">
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
                        <a href="${contextPath}/${detailsPageSearchMode}#lists?type=variables&query=${searchPageQuery}">
                          <i class="<#if type == "Harmonized">${dataschemaIcon}<#else>${variableIcon}</#if>"></i>
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

              <#if type == "Harmonized">
                <@harmonizationProtocolGeneralInfo dataset=dataset />
              </#if>

              </div>
                <#if study??>
                  <div class="card-footer">
                    <#if type == "Harmonized"><@message "associated-initiative"/><#else><@message "associated-study"/></#if>
                    <a class="btn btn-success ms-2" href="${contextPath}/study/${study.id}">
                      <i class="<#if type == "Harmonized">${initiativeIcon}<#else>${studyIcon}</#if>"></i> ${localize(study.acronym)}
                    </a>
                    <#if showVariableStatistics && showDatasetContingencyLink>
                      <a class="btn btn-primary float-end ms-2" href="${contextPath}/dataset-crosstab/${dataset.id}">
                        <i class="fa-solid fa-cog"></i> <@message "dataset.crosstab.title"/>
                      </a>
                    </#if>
                    <#if cartEnabled && variablesCartEnabled>
                      <div id="cart-add" class="float-end">
                        <#if user?? || cartAnonymousEnabled>
                          <button type="button" class="btn btn-link" onclick="onVariablesCartAdd('${dataset.id}')">
                            <@message "sets.cart.add-to-cart"/> <i class="fa-solid fa-cart-plus"></i>
                          </button>
                        <#else>
                          <button type="button" class="btn btn-link" onclick="window.location.href='${contextPath}/signin?redirect=${contextPath}/dataset/${dataset.id}';">
                            <@message "sets.cart.add-to-cart"/> <i class="fa-solid fa-cart-plus"></i>
                          </button>
                        </#if>
                      </div>
                    </#if>
                  </div>
                </#if>
            </div>
          </div>
        </div>

        <#if type == "Harmonized">
          <#if localizedStringNotEmpty(dataset.model.informationContent) || localizedStringNotEmpty(dataset.model.additionalInformation)>
            <div class="row d-flex align-items-stretch">
                <#if localizedStringNotEmpty(dataset.model.informationContent)>
                  <div class="col-sm-12 col-md d-flex align-items-stretch">
                    <div class="card card-info card-outline w-100">
                      <div class="card-header">
                        <h3 class="card-title"><@message "harmonization-protocol.information-content"/></h3>
                      </div>
                      <div class="card-body">
                        <div class="marked"><template>${localize(dataset.model.informationContent)}</template></div>
                      </div>
                    </div>
                  </div>
                </#if>
                <#if localizedStringNotEmpty(dataset.model.additionalInformation)>
                  <div class="col-sm-12 col-md d-flex align-items-stretch">
                    <div class="card card-info card-outline w-100">
                      <div class="card-header">
                        <h3 class="card-title"><@message "global.additional-information"/></h3>
                      </div>
                      <div class="card-body">
                        <div class="marked"><template>${localize(dataset.model.additionalInformation)}</template></div>
                      </div>
                    </div>
                  </div>
                </#if>
            </div>
          </#if>
        </#if>

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
                  <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${population.id}"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
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
                  <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${dceId}"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
                </div>
              </div>
            </div>
          <#elseif (studyTables?? && studyTables?size != 0) || (harmonizationTables?? && harmonizationTables?size != 0)>
            <div class="col">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "studies-included"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-bs-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fa-solid fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <#if studyTables?? && studyTables?size != 0>
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
                              <#if table.population??>
                                <#assign popId="${table.study.id}-${table.population.id}">
                                <@populationDialog id=popId population=table.population></@populationDialog>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${popId}">
                                  ${localize(table.population.name)}
                                </a>
                              </#if>
                            </td>
                            <td>
                              <#if table.population?? && table.dce??>
                                <#assign dceId="${table.study.id}-${table.population.id}-${table.dce.id}">
                                <@dceDialog id=dceId dce=table.dce></@dceDialog>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${dceId}">
                                  ${localize(table.dce.name)}
                                </a>
                              </#if>
                            </td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  </#if>
                </div>
              </div>
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "initiatives-included"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-bs-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fa-solid fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <#if harmonizationTables?? && harmonizationTables?size != 0>
                    <div class="table-responsive">
                      <table class="table table-striped">
                        <thead>
                        <tr>
                          <th><@message "initiative"/></th>
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
                  <a href="${contextPath}/ws/harmonized-dataset/${dataset.id}/variables/harmonizations/_export" class="btn btn-primary float-end mb-3">
                    <i class="fa-solid fa-download"></i> <@message "download"/>
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
                          <#if localizedStringNotEmpty(table.description)>
                          <a href="javascript:void(0)"
                             id="popover-${table?counter}"
                             data-bs-toggle="popover"
                             data-bs-trigger="hover"
                             data-bs-placement="top"
                             data-bs-content="${localize(table.description)}"
                             title="<@message "dataset.harmonized-table" />">
                              <span class="d-inline-block marked"><template>${localize(allStudies[table.studyId].acronym)}</template></span>
                          </a>
                          <#else>
                              ${localize(allStudies[table.studyId].acronym)}
                          </#if>
                          <#if table.name??>${localize(table.name)}</#if>
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
<script>
  document.querySelectorAll('#harmonizedTable tr [data-bs-toggle="popover"]').forEach(el => {
    new bootstrap.Popover(el, {
      html: true,
      delay: { show: 250, hide: 750 }
    });
  });
  document.querySelectorAll("[id^='popover-']").forEach(element => {
    element.dataset.content=marked.parse(element.dataset.content).replaceAll('"', "'");
  })

</script>
</body>
</html>
