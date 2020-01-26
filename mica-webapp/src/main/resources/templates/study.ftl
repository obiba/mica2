<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Study</title>
    <#include "libs/head.ftl">
  <!-- Ionicons -->
  <link rel="stylesheet" href="https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css">
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
            <h1 class="m-0">${study.acronym.en}</h1>
            <small>${study.name.en}</small>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="#">Home</a></li>
              <li class="breadcrumb-item"><a class="text-white-50" href="../studies">Studies</a></li>
              <li class="breadcrumb-item active text-light">Study</li>
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
                    <h3 class="mb-4">${study.name.en}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <#if study.logo??>
                      <img class="img-fluid" style="max-height: 200px" alt="${study.acronym.en} logo" src="../ws/study/${study.id}/file/${study.logo.id}/_download"/>
                    <#else >
                      <p class="text-light text-center">
                        <i class="ion ion-folder fa-6x"></i>
                      </p>
                    </#if>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-info"><i class="ion ion-folder"></i></span>
                      <div class="info-box-content">
                        <span class="info-box-text">Networks</span>
                        <span class="info-box-number" id="network-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-warning"><i class="ion ion-grid"></i></span>
                      <div class="info-box-content">
                        <span class="info-box-text">Datasets</span>
                        <span class="info-box-number" id="dataset-hits">-</span>
                      </div>
                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-danger"><i class="ion ion-pie-graph"></i></span>
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
                    ${study.objectives.en}
                  </#if>
                </p>
                  <#if study.model.website??>
                    <blockquote>
                      Visit <a href="${study.model.website}" target="_blank" class="card-link">${study.acronym.en}</a>
                    </blockquote>
                  </#if>
              </div>
            </div>
          </div>
        </div>

          <#if study.memberships?? && study.memberships?keys?size!=0>
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
                          <#if study.memberships.investigator??>
                              <@memberlist members=study.memberships.investigator role="investigator"/>
                          </#if>
                      </div>
                      <div class="tab-pane fade" id="custom-tabs-three-profile" role="tabpanel" aria-labelledby="custom-tabs-three-profile-tab">
                          <#if study.memberships.contact??>
                              <@memberlist members=study.memberships.contact role="contact"/>
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

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<script>
    micajs.stats('studies', { query: "study(in(Mica_study.id,${study.id}))" }, function(stats) {
        $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
        $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    }, micajs.redirectError);
</script>
</body>
</html>
