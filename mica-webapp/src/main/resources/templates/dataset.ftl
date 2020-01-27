<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | ${dataset.acronym.en}</title>
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
            <h1 class="m-0">Dataset</h1>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="#">Home</a></li>
              <li class="breadcrumb-item"><a class="text-white-50" href="../datasets">Datasets</a></li>
              <li class="breadcrumb-item active text-light">${dataset.acronym.en}</li>
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
                    <h3 class="mb-4">${dataset.name.en}</h3>
                  </div>
                </div>
                <div class="row mb-4">
                  <div class="col-md-3 col-sm-6 col-12">
                    <p class="text-light text-center">
                      <i class="ion ion-grid fa-6x"></i>
                    </p>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-info">
                        <a href="../catalog/#search?type=networks&query=dataset(in(Mica_dataset.id,${dataset.id}))">
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
                      <span class="info-box-icon bg-success">
                        <a href="../catalog/#search?type=studies&query=dataset(in(Mica_dataset.id,${dataset.id}))">
                          <i class="ion ion-folder"></i>
                        </a>
                      </span>
                      <div class="info-box-content">
                        <span class="info-box-text">Studies</span>
                        <span class="info-box-number" id="study-hits">-</span>
                      </div>
                      <div>
                      </div>

                      <!-- /.info-box-content -->
                    </div>
                  </div>
                  <div class="col-md-3 col-sm-6 col-12">
                    <div class="info-box">
                      <span class="info-box-icon bg-danger">
                        <a href="../catalog/#search?type=variables&query=dataset(in(Mica_dataset.id,${dataset.id}))">
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
                  <#if dataset.description?? && dataset.description.en??>
                      ${dataset.description.en}
                  </#if>
                </p>
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
<script>
    micajs.stats('datasets', { query: "dataset(in(Mica_dataset.id,${dataset.id}))" }, function(stats) {
        $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
        $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    }, micajs.redirectError);
</script>
</body>
</html>
