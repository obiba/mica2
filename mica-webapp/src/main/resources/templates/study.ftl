<!-- Macros -->
<#include "models/study.ftl">
<#include "models/member.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${study.acronym[.lang]!""}</title>
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
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50">${type} Study /</span> ${study.acronym[.lang]!""}
            </h1>
            <small>${study.name[.lang]!""}</small>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="../home">Home</a></li>
              <li class="breadcrumb-item"><a class="text-white-50" href="../studies">Studies</a></li>
              <li class="breadcrumb-item active text-light">${study.acronym[.lang]!""}</li>
            </ol>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
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
                    <h3 class="mb-4">${study.name[.lang]!""}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if study.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${study.acronym[.lang]!""} logo" src="../ws/study/${study.id}/file/${study.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="ion ion-folder fa-5x"></i>
                      </p>
                    </#if>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-info">
                        <a href="../search?type=networks&query=study(in(Mica_study.id,${study.id}))">
                          <i class="ion ion-filing"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text">Networks</span>
                        <span class="info-box-number" id="network-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-warning">
                        <a href="../search?type=datasets&query=study(in(Mica_study.id,${study.id}))">
                          <i class="ion ion-grid"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text">Datasets</span>
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
                        <a href="../search?type=variables&query=study(in(Mica_study.id,${study.id}))">
                          <i class="ion ion-pie-graph"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text">Variables</span>
                        <span class="info-box-number" id="variable-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                </div>

                <p class="card-text">
                  <#if study.objectives??>
                    ${study.objectives[.lang]!""}
                  </#if>
                </p>
                  <#if study.model.website??>
                    <blockquote>
                      Visit <a href="${study.model.website}" target="_blank" class="card-link">${study.acronym[.lang]!""}</a>
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
                  <h3 class="card-title">Members</h3>
                  <a href="../ws/persons/_search/_download?limit=1000&query=studyMemberships.parentId:(${study.id})" class="btn btn-primary float-right"><i class="fa fa-download"></i> Download</a>
                </div>
                <div class="card-body">
                  <table class="table">
                    <thead>
                    <tr>
                      <th>Investigators</th>
                      <th>Contacts</th>
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

        <!-- Timeline -->
        <div class="row">
          <div class="col-lg-12">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title">Timeline</h3>
                <div class="card-tools">
                  <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                    <i class="fas fa-minus"></i></button>
                </div>
              </div>
              <div class="card-body">
                <div id="timeline"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Populations -->
        <#if study.populations?? && study.populations?size != 0>
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title">
                    <#if study.populations?size == 1>
                      Population
                    <#else>
                      Populations
                    </#if>
                  </h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <#if study.populations?size == 1>
                  <#else>
                    <ul class="nav nav-pills mb-3">
                      <#list study.populations as pop>
                        <li class="nav-item"><a class="nav-link <#if pop?index == 0>active</#if>" href="#population-${pop.id}" data-toggle="tab">
                          ${pop.name[.lang]!""}</a>
                        </li>
                      </#list>
                    </ul>
                  </#if>
                  <div class="tab-content">
                    <#list study.populations as pop>
                      <div class="tab-pane <#if pop?index == 0>active</#if>" id="population-${pop.id}">
                        <div>
                          <#if pop.description??>${pop.description[.lang]!""}</#if>
                        </div>
                        <@populationModel population=pop/>

                        <!-- DCE list -->
                        <#if pop.dataCollectionEvents?? && pop.dataCollectionEvents?size != 0>
                          <h5>Data Collection Events</h5>
                          <table id="population-${pop.id}-dces" class="table table-bordered table-striped">
                            <thead>
                            <tr>
                              <th>Name</th>
                              <th>Description</th>
                              <th>Start</th>
                              <th>End</th>
                            </tr>
                            </thead>
                            <tbody>
                              <#list pop.dataCollectionEvents as dce>
                                <tr>
                                  <td>
                                    <#assign dceId="${pop.id}-${dce.id}">
                                    <a href="#" data-toggle="modal" data-target="#modal-${dceId}">
                                      ${dce.name[.lang]!""}
                                    </a>
                                    <@dceDialog id=dceId dce=dce></@dceDialog>
                                  </td>
                                  <td><small><#if dce.description?? && dce.description[.lang]??>${dce.description[.lang]?trim?truncate(200, "...")}</#if></small></td>
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
