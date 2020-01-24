<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | ${network.acronym.en}</title>
    <#include "libs/head.ftl">
  <!-- DataTables -->
  <link rel="stylesheet" href="../bower_components/admin-lte/plugins/datatables-bs4/css/dataTables.bootstrap4.css">
</head>
<body class="hold-transition layout-top-nav">
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
              <li class="breadcrumb-item"><a class="text-white-50" href="#">Home</a></li>
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
                  <div class="col-lg-8">
                      <#if network.logo??>
                        <img class="img-fluid" style="max-height: 200px" alt="${network.acronym.en} logo" src="../ws/network/${network.id}/file/${network.logo.id}/_download"/>
                      <#else >
                        <p class="text-black-50 text-center">
                          <i class="fas fa-project-diagram fa-5x"></i>
                        </p>
                      </#if>
                  </div>
                  <div class="col-lg-4">
                    <div class="info-box">
                      <span class="info-box-icon bg-info"><i class="far fa-envelope"></i></span>
                      <div class="info-box-content">
                        <span class="info-box-text">Studies</span>
                        <span class="info-box-number">${individualStudies?size + harmonizationStudies?size}</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                </div>

                <p class="card-text">
                    ${network.description.en}
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

        <div class="row">
          <div class="col-lg-6">
              <#if network.memberships??>
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
              </#if>
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

<!-- DataTables -->
<script src="../bower_components/admin-lte/plugins/datatables/jquery.dataTables.js"></script>
<script src="../bower_components/admin-lte/plugins/datatables-bs4/js/dataTables.bootstrap4.js"></script>
<!-- page script -->
<script>
    $(function () {
        var opts = {
            "paging": false,
            //"scrollY": "200px",
            "lengthChange": false,
            "searching": true,
            "ordering": true,
            "info": false,
            "autoWidth": true,
        };
        $("#networks").DataTable(opts);
        $("#individual-studies").DataTable(opts);
        $("#harmonization-studies").DataTable(opts);
    });
</script>
</body>
</html>
