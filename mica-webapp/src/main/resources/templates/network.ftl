<!-- Macros -->
<#include "models/member-macros.ftl">
<#include "models/network-macros.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <title>${config.name!""} | ${network.acronym[.lang]!""}</title>
  <#include "libs/head.ftl">
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
            <h1 class="m-0"><span class="text-white-50">Network / </span>${network.acronym[.lang]!""}</h1>
            <small>${network.name[.lang]!""}</small>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="../home">Home</a></li>
              <li class="breadcrumb-item"><a class="text-white-50" href="../networks">Networks</a></li>
              <li class="breadcrumb-item active text-light">${network.acronym[.lang]!""}</li>
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
                    <h3 class="mb-4">${network.name[.lang]!""}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if network.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${network.acronym[.lang]!""} logo" src="../ws/network/${network.id}/file/${network.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="ion ion-filing fa-5x"></i>
                      </p>
                    </#if>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-success">
                        <a href="../search?type=studies&query=network(in(Mica_network.id,${network.id}))">
                          <i class="ion ion-folder"></i>
                        </a></span>
                      <div class="info-box-content">
                        <span class="info-box-text">Studies</span>
                        <span class="info-box-number" id="study-hits">${individualStudies?size + harmonizationStudies?size}</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-warning">
                        <a href="../search?type=datasets&query=network(in(Mica_network.id,${network.id}))">
                          <i class="ion ion-grid"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text">Datasets</span>
                        <span class="info-box-number" id="dataset-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-danger">
                        <a href="../search?type=datasets&query=network(in(Mica_network.id,${network.id}))">
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
                  <#if network.description??>
                    ${network.description[.lang]!""}
                  </#if>
                </p>
                  <#if network.model.website??>
                    <blockquote>
                      Visit <a href="${network.model.website}" target="_blank" class="card-link">${network.acronym[.lang]!""}</a>
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
                  <h3 class="card-title">Members</h3>
                  <#if affiliatedMembersQuery??>
                    <a href="../ws/persons/_search/_download?limit=1000&query=${affiliatedMembersQuery?url('utf-8')}" class="btn btn-primary float-right"><i class="fa fa-download"></i> Affiliated Members</a>
                  </#if>
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
            <!-- /.col-12 -->
          </div>
          <!-- /.row -->
        </#if>

        <!-- Network model -->
        <#include "models/network.ftl">

        <!-- Network list -->
        <#if networks?size != 0>
          <div class="row">
            <div class="col-lg-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title">Networks</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <table id="networks" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th>Acronym</th>
                      <th>Name</th>
                      <@networkTHs/>
                    </tr>
                    </thead>
                    <tbody>
                    <#list networks as netwk>
                      <tr>
                        <td><a href="../network/${netwk.id}">${netwk.acronym[.lang]!""}</a></td>
                        <td><small>${netwk.name[.lang]!""}</small></td>
                        <@networkTDs network=netwk/>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
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
                  <h3 class="card-title">Individual Studies</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">

                  <table id="individual-studies" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th>Acronym</th>
                      <th>Name</th>
                      <@individualStudyTHs/>
                    </tr>
                    </thead>
                    <tbody>
                    <#list individualStudies as study>
                      <tr>
                        <td><a href="../study/${study.id}">${study.acronym[.lang]!""}</a></td>
                        <td><small>${study.name[.lang]!""}</small></td>
                        <@individualStudyTDs study=study/>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
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
                  <h3 class="card-title">Harmonization Studies</h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip" title="Collapse">
                      <i class="fas fa-minus"></i></button>
                  </div>
                </div>
                <div class="card-body">
                  <table id="harmonization-studies" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th>Acronym</th>
                      <th>Name</th>
                      <@harmonizationStudyTHs/>
                    </tr>
                    </thead>
                    <tbody>
                    <#list harmonizationStudies as study>
                      <tr>
                        <td><a href="../study/${study.id}">${study.acronym[.lang]!""}</a></td>
                        <td><small>${study.name[.lang]!""}</small></td>
                        <@harmonizationStudyTDs study=study/>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
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
<!-- page script -->
<script>
    $(function () {
        $("#networks").DataTable(dataTablesDefaultOpts);
        $("#individual-studies").DataTable(dataTablesDefaultOpts);
        $("#harmonization-studies").DataTable(dataTablesDefaultOpts);
    });
    micajs.stats('networks', { query: "network(in(Mica_network.id,${network.id}))" }, function(stats) {
        $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
        $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    }, micajs.redirectError);
</script>
</body>
</html>
