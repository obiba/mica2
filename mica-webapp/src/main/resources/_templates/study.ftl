<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/study.ftl">
<#include "models/member.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">
<#include "models/files.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(study.acronym)}</title>
  <link rel="stylesheet" href="${contextPath}/bower_components/mica-study-timeline/dist/mica-study-timeline.css" />
</head>
<body id="${type?lower_case}-study-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-study") title=localize(study.acronym) subtitle=localize(study.name) breadcrumb=[["..", "home"], ["${contextPath}/studies", "studies"], [localize(study.acronym)]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if draft>
          <div class="alert alert-warning" role="alert">
            <i class="icon fas fa-exclamation-triangle"></i> <@messageArgs code="viewing-draft-version" args=["/study/${study.id}"]/>
          </div>
        </#if>

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
                      <img class="img-fluid" style="max-height: 200px" alt="${localize(study.acronym)} logo" src="${contextPath}/ws/study/${study.id}/file/${study.logo.id}/_download"/>
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
                          <a href="${contextPath}/search#lists?type=networks&query=study(in(Mica_study.id,${study.id}))">
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
                          <a href="${contextPath}/search#lists?type=datasets&query=study(in(Mica_study.id,${study.id}))">
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
                          <a href="${contextPath}/search#lists?type=variables&query=study(in(Mica_study.id,${study.id}))">
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

                <div class="card-text marked mt-3">
                  ${localize(study.objectives)}
                </div>

                <!-- Study definition model -->
                <@studyDefinition study=study type=type/>

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
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>
                  <a href="${contextPath}/ws/persons/_search/_download?limit=1000&query=studyMemberships.parentId:(${study.id})" class="btn btn-primary float-right mr-2">
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
                      <#list study.populationsSorted as pop>
                        <li class="nav-item"><a class="nav-link <#if pop?index == 0>active</#if>" href="#population-${pop.id}" data-toggle="tab">
                          ${localize(pop.name)}</a>
                        </li>
                      </#list>
                    </ul>
                  </#if>
                  <div class="tab-content">
                    <#list study.populationsSorted as pop>
                      <div class="tab-pane <#if pop?index == 0>active</#if>" id="population-${pop.id}">
                        <div class="mb-3 marked">
                          ${localize(pop.description)}
                        </div>
                        <@populationModel population=pop/>
                        <@dceList population=pop/>
                      </div>
                    </#list>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </#if>

        <!-- Files -->
        <#if showStudyFiles>
          <@studyFilesBrowser study=study/>
        </#if>

        <!-- Variables classifications -->
        <#if studyVariablesClassificationsTaxonomies?? && studyVariablesClassificationsTaxonomies?size gt 0>
          <@variablesClassifications study=study/>
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
