<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/network.ftl">
<#include "models/member.ftl">
<#include "models/files.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(network.acronym)}</title>
</head>
<body id="network-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix="network" title=localize(network.acronym) subtitle=localize(network.name) breadcrumb=[["${contextPath}/", "home"], ["${contextPath}/networks", "networks"], [localize(network.acronym)]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if draft>
          <div class="alert alert-warning" role="alert">
            <i class="icon fas fa-exclamation-triangle"></i> <@messageArgs code="viewing-draft-version" args=["/network/${network.id}"]/>
          </div>
        </#if>

        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-body">
                <div class="row">
                  <div class="col-lg-12">
                    <h3 class="mb-4">${localize(network.name)}</h3>
                  </div>
                </div>
                <div class="row">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if network.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${localize(network.acronym)} logo" src="${contextPath}/ws/network/${network.id}/file/${network.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="${networkIcon} fa-4x"></i>
                      </p>
                    </#if>
                  </div>

                  <#if !config.singleStudyEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-success">
                          <a href="${contextPath}/search#lists?type=studies&query=network(in(Mica_network.id,${network.id}))">
                            <i class="${studyIcon}"></i>
                          </a></span>
                        <div class="info-box-content">
                          <span class="info-box-text"><@message "studies"/></span>
                          <span class="info-box-number" id="study-hits">${individualStudies?size + harmonizationStudies?size}</span>
                        </div>
                        <!-- /.info-box-content -->
                      </div>
                    </div>
                  </#if>

                  <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-warning">
                          <a href="${contextPath}/search#lists?type=datasets&query=network(in(Mica_network.id,${network.id}))">
                            <i class="${datasetIcon}"></i>
                          </a>
                        </span>
                        <div class="info-box-content">
                          <span class="info-box-text"><@message "datasets"/></span>
                          <span class="info-box-number" id="dataset-hits">-</span>
                        </div>
                        <!-- /.info-box-content -->
                      </div>
                    </div>
                    <div class="col-md-3 col-sm-6 col-12">
                      <div class="info-box">
                        <span class="info-box-icon bg-danger">
                          <a href="${contextPath}/search#lists?type=variables&query=network(in(Mica_network.id,${network.id}))">
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
                  <template>${localize(network.description)}</template>
                </div>
                <#if network.model.website??>
                  <blockquote>
                    <@message "visit"/> <a href="${network.model.website}" target="_blank" class="card-link">${localize(network.acronym)}</a>
                  </blockquote>
                </#if>
              </div>
            </div>
          </div>
        </div>

        <!-- Member list -->
        <#if network.memberships?? && network.memberships?keys?size!=0>
          <div class="row">
            <div class="col-12">
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "members"/></h3>
                  <div class="card-tools float-right">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>

                  <#if affiliatedMembersQuery??>
                    <button type="button" class="btn btn-primary float-right mr-2" data-toggle="modal" data-target="#affiliatedMembersModal">
                      <@message "network.associated-people"/>
                    </button>
                  </#if>
                </div>
                <div class="card-body">
                  <div class="table-responsive">
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
                          <#if network.memberships.investigator??>
                              <@memberList members=network.memberships.investigator role="investigator"/>
                          </#if>
                        </td>
                        <td>
                          <#if network.memberships.contact??>
                              <@memberList members=network.memberships.contact role="contact"/>
                          </#if>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
            <!-- /.col-12 -->
          </div>
          <!-- /.row -->
        </#if>

        <#--  Summary Statistics  -->
        <div class="row" id="summary-statistics-container">
          <div class="col-12">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "client.label.network.summary-stats"/></h3>
              </div>

              <div class="card-body">
                <div id="summary-statistics">
                  <div class="mt-3 text-muted" v-show="!hasGraphicsResult"><@message "no-graphics-result"/></div>

                  <template v-show="hasGraphicsResult">
                    <ul class="nav nav-tabs">
                      <li class="nav-item" v-for="(option, index) in chartOptions" v-bind:key="option.id"><a href="" class="nav-link" @click.prevent="onTabClick(index)">{{option.title}}</a></li>
                    </ul>

                    <div class="card card-info card-outline" v-for="chartOption in rawChartOptions">
                      <div class="card-body">
                        <p class="text-muted">{{chartOption.text | translate}}</p>

                        <div class="row">
                          <div class="col my-auto">
                            <search-graph :aggregation-name="chartOption.agg" :default-graph-type="chartOption.plotlyType" :graph-colors="chartOption.borderColor" :study-result="graphResult" :study-taxonomy="studyTaxonomy"></search-graph>
                          </div>

                          <div class="col overflow-auto" style="max-height: 24em">
                            <search-graph-table :study-result="graphResult" :chart-options="chartOption"></search-graph-table>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Network model -->
        <@networkModel network=network/>

        <!-- Network list -->
        <#if networks?size != 0>
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "networks"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <div class="table-responsive">
                    <table id="networks" class="table table-bordered table-striped">
                      <thead>
                      <tr>
                        <th><@message "acronym"/></th>
                        <th><@message "name"/></th>
                        <@networkTHs/>
                      </tr>
                      </thead>
                      <tbody>
                      <#list networks as netwk>
                        <tr>
                          <td><a href="${contextPath}/network/${netwk.id}">${localize(netwk.acronym)}</a></td>
                          <td><small>${localize(netwk.name)}</small></td>
                          <@networkTDs network=netwk/>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </#if>

        <!-- Individual study list -->
        <#if individualStudies?size != 0>
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "individual-studies"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <div class="table-responsive">
                    <table id="individual-studies" class="table table-bordered table-striped">
                      <thead>
                      <tr>
                        <th><@message "acronym"/></th>
                        <th><@message "name"/></th>
                        <@individualStudyTHs/>
                      </tr>
                      </thead>
                      <tbody>
                      <#list individualStudies as study>
                        <tr>
                          <td><a href="${contextPath}/study/${study.id}">${localize(study.acronym)}</a></td>
                          <td><small>${localize(study.name)}</small></td>
                          <@individualStudyTDs study=study/>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </#if>

        <!-- Harmonization study list -->
        <#if harmonizationStudies?size != 0>
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "harmonization-studies"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="<@message "collapse"/>">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <div class="table-responsive">
                    <table id="harmonization-studies" class="table table-bordered table-striped">
                      <thead>
                      <tr>
                        <th><@message "acronym"/></th>
                        <th><@message "name"/></th>
                        <@harmonizationStudyTHs/>
                      </tr>
                      </thead>
                      <tbody>
                      <#list harmonizationStudies as study>
                        <tr>
                          <td><a href="${contextPath}/study/${study.id}">${localize(study.acronym)}</a></td>
                          <td><small>${localize(study.name)}</small></td>
                          <@harmonizationStudyTDs study=study/>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </#if>

        <!-- Files -->
        <#if showNetworkFiles>
          <@networkFilesBrowser network=network/>
        </#if>

        <!-- Variables classifications -->
        <#if networkVariablesClassificationsTaxonomies?? && (networkVariablesClassificationsTaxonomies?size gt 0) && studyAcronyms?? && (studyAcronyms?size gt 0)>
          <@variablesClassifications network=network studyAcronyms=studyAcronyms/>
        </#if>

        <!-- Affiliated Members Modal -->
        <div class="modal fade" id="affiliatedMembersModal" data-keyboard="false" tabindex="-1">
          <div class="modal-dialog modal-xl">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><@message "network.associated-people"/></h5>
                <button type="button" class="close" data-dismiss="modal">
                  <span>&times;</span>
                </button>
              </div>

              <div class="modal-body">
                <div class="container-fluid">
                  <div class="row">
                    <a href="${contextPath}/ws/persons/_search/_download?limit=1000&query=${affiliatedMembersQuery?url('utf-8')}" class="btn btn-primary mb-2">
                      <i class="fas fa-download"></i> <@message "download"/>
                    </a>
                  </div>

                  <div class="row">
                    <div class="table-responsive">
                      <table id="affiliatedMembersTable" class="table" style="width: 100%"></table>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/network-scripts.ftl">

</body>
</html>
