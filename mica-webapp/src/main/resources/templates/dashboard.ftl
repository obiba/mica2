<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Dashboard</title>
    <#include "libs/head.ftl">
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
            <h1 class="m-0">Dashboard</h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <div class="row">
          <div class="col-lg-3 col-6">
            <!-- small box -->
            <div class="small-box bg-info">
              <div class="inner">
                <h3 id="network-hits">-</h3>
                <p>Networks</p>
              </div>
              <div class="icon">
                <i class="ion ion-filing"></i>
              </div>
              <a href="../catalog/#/search?type=networks" class="small-box-footer">More info <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
          <!-- ./col -->
          <div class="col-lg-3 col-6">
            <!-- small box -->
            <div class="small-box bg-success">
              <div class="inner">
                <h3 id="study-hits">-</h3>
                <p>Studies</p>
              </div>
              <div class="icon">
                <i class="ion ion-folder"></i>
              </div>
              <a href="../catalog/#/search?type=studies" class="small-box-footer">More info <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
          <!-- ./col -->
          <div class="col-lg-3 col-6">
            <!-- small box -->
            <div class="small-box bg-warning">
              <div class="inner">
                <h3 id="dataset-hits">-</h3>
                <p>Datasets</p>
              </div>
              <div class="icon">
                <i class="ion ion-grid"></i>
              </div>
              <a href="../catalog/#/search?type=datasets" class="small-box-footer">More info <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
          <!-- ./col -->
          <div class="col-lg-3 col-6">
            <!-- small box -->
            <div class="small-box bg-danger">
              <div class="inner">
                <h3 id="variable-hits">-</h3>
                <p>Variables</p>
              </div>
              <div class="icon">
                <i class="ion ion-pie-graph"></i>
              </div>
              <a href="../catalog/#/search?type=variables" class="small-box-footer">More info <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
          <!-- ./col -->
        </div>

        <div class="callout callout-info">
          <div class="row mb-2">
            <div class="col-sm-10">
              <p class="text-justify">
                Search tool to query networks, studies, and variables and to explore harmonization potential across studies.
              </p>
            </div><!-- /.col -->
            <div class="col-sm-2">
              <div class="text-right">
                <button type="button"  onclick="location.href='../catalog';" class="btn btn-success btn-lg">
                  <i class="fas fa-search"></i> Search
                </button>
              </div>
            </div><!-- /.col -->
          </div><!-- /.row -->

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
    micajs.stats('networks', {}, function(stats) {
        $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
        $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
        $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    }, micajs.redirectError);
</script>

</body>
</html>
