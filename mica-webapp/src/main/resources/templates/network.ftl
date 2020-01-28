<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | ${network.acronym.en}</title>
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
            <h1 class="m-0">${network.acronym.en}</h1>
            <small>${network.name.en}</small>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="../home">Home</a></li>
              <li class="breadcrumb-item"><a class="text-white-50" href="../networks">Networks</a></li>
              <li class="breadcrumb-item active text-light">${network.acronym.en}</li>
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
                    <h3 class="mb-4">${network.name.en}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if network.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${network.acronym.en} logo" src="../ws/network/${network.id}/file/${network.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="ion ion-filing fa-5x"></i>
                      </p>
                    </#if>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-success">
                        <a href="../catalog/#search?type=studies&query=network(in(Mica_network.id,${network.id}))">
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
                        <a href="../catalog/#search?type=datasets&query=network(in(Mica_network.id,${network.id}))">
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
                        <a href="../catalog/#search?type=datasets&query=network(in(Mica_network.id,${network.id}))">
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
                    ${network.description.en}
                  </#if>
                </p>
                  <#if network.model.website??>
                    <blockquote>
                      Visit <a href="${network.model.website}" target="_blank" class="card-link">${network.acronym.en}</a>
                    </blockquote>
                  </#if>
              </div>
            </div>
          </div>
        </div>

        <#if network.memberships?? && network.memberships?keys?size!=0>
          <div class="row">
            <div class="col-lg-6">
              <div class="card card-primary card-outline card-outline-tabs">
                <div class="card-header p-0 border-bottom-0">
                  <ul class="nav nav-tabs" id="custom-tabs-three-tab" role="tablist">
                    <li class="nav-item">
                      <a class="nav-link active" id="custom-tabs-three-home-tab" data-toggle="pill" href="#custom-tabs-three-home" role="tab" aria-controls="custom-tabs-three-home" aria-selected="true">Investigators</a>
                    </li>
                    <li class="nav-item">
                      <a class="nav-link" id="custom-tabs-three-profile-tab" data-toggle="pill" href="#custom-tabs-three-profile" role="tab" aria-controls="custom-tabs-three-profile" aria-selected="false">Contacts</a>
                    </li>
                  </ul>
                </div>

                <div class="card-body">
                  <div class="tab-content" id="custom-tabs-three-tabContent">
                    <div class="tab-pane fade show active" id="custom-tabs-three-home" role="tabpanel" aria-labelledby="custom-tabs-three-home-tab">
                        <#if network.memberships.investigator??>
                            <@memberlist members=network.memberships.investigator role="investigator"/>
                        </#if>
                    </div>
                    <div class="tab-pane fade" id="custom-tabs-three-profile" role="tabpanel" aria-labelledby="custom-tabs-three-profile-tab">
                        <#if network.memberships.contact??>
                            <@memberlist members=network.memberships.contact role="contact"/>
                        </#if>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- /.col-md-6 -->
            <div class="col-lg-6">
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title">Affiliated Members</h3>
                </div>
                <div class="card-body">
                  TODO
                </div>
              </div>
            </div>
            <!-- /.col-md-6 -->
          </div>
          <!-- /.row -->
        </#if>

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
                    </tr>
                    </thead>
                    <tbody>
                    <#list networks as netwk>
                      <tr>
                        <td><a href="../network/${netwk.id}">${netwk.acronym.en}</a></td>
                        <td><small>${netwk.name.en}</small></td>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </#if>

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
                      <th>Study Design</th>
                      <th>Participants</th>
                      <th>Countries</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list individualStudies as study>
                      <tr>
                        <td><a href="../study/${study.id}">${study.acronym.en}</a></td>
                        <td><small>${study.name.en}</small></td>
                        <td>${study.model.methods.design}</td>
                        <td>${study.model.numberOfParticipants.participant.number}</td>
                        <td></td>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </#if>

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
                    </tr>
                    </thead>
                    <tbody>
                    <#list harmonizationStudies as study>
                      <tr>
                        <td><a href="../study/${study.id}">${study.acronym.en}</a></td>
                        <td><small>${study.name.en}</small></td>
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
