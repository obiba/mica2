<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <title>${config.name!""} | ${dataset.acronym[.lang]!""}</title>
  <#include "libs/head.ftl">
  <!-- Ionicons -->
  <link rel="stylesheet" href="https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css">
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-dataset") title=dataset.acronym[.lang]!"" subtitle=dataset.name[.lang]!"" breadcrumb=[["..", "home"], ["../datasets", "datasets"], [dataset.acronym[.lang]!""]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <!-- General Information content -->
        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-body">
                <div class="row">
                  <div class="col-lg-12">
                    <h3 class="mb-4">${dataset.name[.lang]!""}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <p class="text-muted text-center">
                      <#if type == "Collected">
                        <i class="ion ion-grid fa-5x"></i>
                      <#else >
                        <i class="ion ion-gear-b fa-5x"></i>
                      </#if>
                    </p>
                  </div>

                  <#if config.networkEnabled && !config.singleNetworkEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-info">
                          <a href="../search#lists?type=networks&query=dataset(in(Mica_dataset.id,${dataset.id}))">
                            <i class="ion ion-filing"></i>
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
                        <a href="../search#lists?type=variables&query=dataset(in(Mica_dataset.id,${dataset.id}))">
                          <i class="ion ion-pie-graph"></i>
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

                <p class="card-text">
                  <#if dataset.description?? && dataset.description[.lang]??>
                      ${dataset.description[.lang]!""}
                  </#if>
                </p>
              </div>
                <#if study??>
                  <div class="card-footer">
                    <@message "associated-study"/>
                    <a class="btn btn-success ml-2" href="../study/${study.id}">
                      <i class="ion ion-folder"></i> ${study.acronym[.lang]!""}
                    </a>
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
                  <h3 class="card-title">Population</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <h5>${population.name[.lang]!""}</h5>
                  <div><#if population.description??>${population.description[.lang]!""}</#if></div>
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
                  <h3 class="card-title">Data Collection Event</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <h5>${dce.name[.lang]!""}</h5>
                  <div><#if dce.description??>${dce.description[.lang]!""}</#if></div>
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
                  <h3 class="card-title">Studies Included</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <#if studyTables?? && studyTables?size != 0>
                    <h5><@message "individual-studies"/></h5>
                    <table class="table table-striped mb-3">
                      <thead>
                      <tr>
                        <th>Study</th>
                        <th>Population</th>
                        <th>DCE</th>
                      </tr>
                      </thead>
                      <tbody>
                      <#list studyTables as table>
                        <tr>
                          <td>
                            <a href="../study/${table.study.id}">
                              ${table.study.acronym[.lang]!""}
                            </a>
                          </td>
                          <td>
                            <#assign popId="${table.study.id}-${table.population.id}">
                            <@populationDialog id=popId population=table.population></@populationDialog>
                            <a href="#" data-toggle="modal" data-target="#modal-${popId}">
                              ${table.population.name[.lang]!""}
                            </a>
                          </td>
                          <td>
                            <#assign dceId="${table.study.id}-${table.population.id}-${table.dce.id}">
                            <@dceDialog id=dceId dce=table.dce></@dceDialog>
                            <a href="#" data-toggle="modal" data-target="#modal-${dceId}">
                              ${table.dce.name[.lang]!""}
                            </a>
                          </td>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </#if>
                    <#if harmonizationTables?? && harmonizationTables?size != 0>
                      <h5><@message "harmonization-studies"/></h5>
                      <table class="table table-striped">
                        <thead>
                        <tr>
                          <th>Study</th>
                          <th>Population</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list harmonizationTables as table>
                          <tr>
                            <td>
                              <a href="../study/${table.study.id}">
                                ${table.study.acronym[.lang]!""}
                              </a>
                            </td>
                            <td>
                              <#assign popId="${table.study.id}-${table.population.id}">
                              <@populationDialog id=popId population=table.population></@populationDialog>
                              <a href="#" data-toggle="modal" data-target="#modal-${popId}">
                                ${table.population.name[.lang]!""}
                              </a>
                            </td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </#if>
                </div>
              </div>
            </div>
          </#if>
        </div>

        <!-- Harmonization content -->
        <#if type != "Collected">
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title">Harmonization</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  TODO
                </div>
              </div>
            </div>
            </div>
          </div>
        </#if>

      </div><!-- /.container-fluid -->
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
