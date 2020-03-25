<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/study.ftl">
<#include "models/member.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(study.acronym)}</title>
  <link rel="stylesheet" href="../bower_components/mica-study-timeline/dist/mica-study-timeline.css" />
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-study") title=localize(study.acronym) subtitle=localize(study.name) breadcrumb=[["..", "home"], ["../studies", "studies"], [localize(study.acronym)]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-body">
                <div class="row">
                  <div class="col-lg-12">
                    <h3 class="mb-4">${localize(study.name)}</h3>
                  </div>
                </div>
                <div class="row">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if study.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${localize(study.acronym)} logo" src="../ws/study/${study.id}/file/${study.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="${studyIcon} fa-4x"></i>
                      </p>
                    </#if>
                  </div>

                  <#if config.networkEnabled && !config.singleNetworkEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-info">
                          <a href="../search#lists?type=networks&query=study(in(Mica_study.id,${study.id}))">
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

                  <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-warning">
                          <a href="../search#lists?type=datasets&query=study(in(Mica_study.id,${study.id}))">
                            <i class="${datasetIcon}"></i>
                          </a>
                        </span>
                        <div class="info-box-content">
                          <span class="info-box-text"><@message "datasets"/></span>
                          <span class="info-box-number" id="dataset-hits">-</span>
                        </div>
                        <div>
                        </div>

                        <!-- /.info-box-content -->
                      </div>
                    </div>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-danger">
                          <a href="../search#lists?type=variables&query=study(in(Mica_study.id,${study.id}))">
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
                  </#if>

                </div>

                <div class="card-text marked">
                  ${localize(study.objectives)}
                </div>
                  <#if study.model.website??>
                    <blockquote>
                      <@message "visit"/> <a href="${study.model.website}" target="_blank">${localize(study.acronym)}</a>
                    </blockquote>
                  </#if>
              </div>
            </div>
          </div>
        </div>

        <!-- Member list -->
        <#if study.memberships?? && study.memberships?keys?size!=0>
          <div class="row">
            <div class="col-12">
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "members"/></h3>
                  <div class="card-tools float-right">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                  <a href="../ws/persons/_search/_download?limit=1000&query=studyMemberships.parentId:(${study.id})" class="btn btn-primary float-right mr-2">
                    <i class="fas fa-download"></i> <@message "download"/>
                  </a>
                </div>
                <div class="card-body">
                  <table class="table">
                    <thead>
                    <tr>
                      <th><@message "investigators"/></th>
                      <th><@message "contacts"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                      <td>
                        <#if study.memberships.investigator??>
                          <@memberList members=study.memberships.investigator role="investigator"/>
                        </#if>
                      </td>
                      <td>
                        <#if study.memberships.contact??>
                          <@memberList members=study.memberships.contact role="contact"/>
                        </#if>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
            <!-- /.col-12 -->
          </div>
          <!-- /.row -->
        </#if>

        <!-- Study model -->
        <@studyModel study=study type=type/>

        <#if study.populations?? && study.populations?size != 0>
          <!-- Timeline -->
          <#if type == "Individual">
            <div class="row">
              <div class="col-lg-12">
                <div class="card card-info card-outline">
                  <div class="card-header">
                    <h3 class="card-title"><@message "timeline"/></h3>
                  </div>
                  <div class="card-body">
                    <div id="timeline"></div>
                  </div>
                </div>
              </div>
            </div>
          </#if>

          <!-- Populations -->
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title">
                    <#if study.populations?size == 1>
                      <@message "population"/>
                    <#else>
                      <@message "populations"/>
                    </#if>
                  </h3>
                </div>
                <div class="card-body">
                  <#if study.populations?size == 1>
                  <#else>
                    <ul class="nav nav-pills mb-3">
                      <#list study.populations as pop>
                        <li class="nav-item"><a class="nav-link <#if pop?index == 0>active</#if>" href="#population-${pop.id}" data-toggle="tab">
                          ${localize(pop.name)}</a>
                        </li>
                      </#list>
                    </ul>
                  </#if>
                  <div class="tab-content">
                    <#list study.populations as pop>
                      <div class="tab-pane <#if pop?index == 0>active</#if>" id="population-${pop.id}">
                        <div class="mb-3 marked">
                          ${localize(pop.description)}
                        </div>
                        <@populationModel population=pop/>

                        <!-- DCE list -->
                        <#if pop.dataCollectionEvents?? && pop.dataCollectionEvents?size != 0>
                          <h5><@message "study.data-collection-events"/></h5>
                          <table id="population-${pop.id}-dces" class="table table-bordered table-striped">
                            <thead>
                            <tr>
                              <th>#</th>
                              <th><@message "name"/></th>
                              <th><@message "description"/></th>
                              <th><@message "study.start"/></th>
                              <th><@message "study.end"/></th>
                            </tr>
                            </thead>
                            <tbody>
                              <#list pop.dataCollectionEvents as dce>
                                <tr>
                                  <td>${dce.weight}</td>
                                  <td>
                                    <#assign dceId="${pop.id}-${dce.id}">
                                    <a href="#" data-toggle="modal" data-target="#modal-${dceId}">
                                      ${localize(dce.name)}
                                    </a>
                                    <@dceDialog id=dceId dce=dce></@dceDialog>
                                  </td>
                                  <td><small>${localize(dce.description)?trim?truncate(200, "...")}</small></td>
                                  <td><#if dce.start?? && dce.start.yearMonth??>${dce.start.yearMonth}</#if></td>
                                  <td><#if dce.end?? && dce.end.yearMonth??>${dce.end.yearMonth}</#if></td>
                                </tr>
                              </#list>
                            </tbody>
                          </table>
                        </#if>
                      </div>
                    </#list>
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
<#include "libs/study-scripts.ftl">

</body>
</html>
