<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Datasets</title>
  <#include "libs/head.ftl">
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Template variables -->
  <#if !type??>
    <#assign title = "Datasets">
    <#assign callout = "">
    <#assign showTypeColumn = true>
  <#elseif type == "Harmonized">
    <#assign title = "Harmonized Datasets">
    <#assign callout = "">
    <#assign showTypeColumn = false>
  <#else>
    <#assign title = "Collected Datasets">
    <#assign callout = "">
    <#assign showTypeColumn = false>
  </#if>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0">${title}</h1>
          </div><!-- /.col -->
          <div class="col-sm-6">
            <ol class="breadcrumb float-sm-right">
              <li class="breadcrumb-item"><a class="text-white-50" href="../index">Home</a></li>
              <li class="breadcrumb-item active text-light">${title}</li>
            </ol>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <div class="callout callout-info">
          <p>
            ${callout}
          </p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <#if datasets?? && datasets?size != 0>
              <div class="card card-info card-outline">

                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3">Datasets</h3>
                  <ul class="nav nav-pills ml-auto p-2">
                    <li class="nav-item"><a class="nav-link" href="#list" data-toggle="tab">
                      <i class="fas fa-grip-lines"></i></a>
                    </li>
                    <li class="nav-item"><a class="nav-link active" href="#cards" data-toggle="tab">
                      <i class="fas fa-grip-horizontal"></i></a>
                    </li>
                  </ul>
                </div><!-- /.card-header -->


                <div class="card-body">
                  <div class="tab-content">
                    <div class="tab-pane" id="list">
                      <table id="datasets" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th>Acronym</th>
                          <th>Name</th>
                          <th>Description</th>
                          <#if showTypeColumn>
                            <th>Type</th>
                          </#if>
                        </tr>
                        </thead>
                        <tbody>
                        <#list datasets as ds>
                          <tr>
                            <td><a href="../dataset/${ds.id}">${ds.acronym.en}</a></td>
                            <td><small>${ds.name.en}</small></td>
                            <td><small><#if ds.description?? && ds.description.en??>${ds.description.en?trim?truncate_w(100, "...")}</#if></small></td>
                            <#if showTypeColumn>
                              <td>
                                <#if ds.class.simpleName == "HarmonizationDataset">
                                  Harmonized
                                <#else>
                                  Collected
                                </#if>
                              </td>
                            </#if>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>

                    <div class="tab-pane active" id="cards">
                      <div class="row d-flex align-items-stretch">
                        <#list datasets as ds>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${ds.acronym.en}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${ds.name.en}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                    <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                      <#if ds.class.simpleName == "HarmonizationDataset">
                                        <i class="ion ion-gear-b fa-4x"></i>
                                      <#else>
                                        <i class="ion ion-grid fa-4x"></i>
                                      </#if>
                                    </p>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../dataset/${ds.id}" class="btn btn-sm btn-primary">
                                    <i class="fas fa-eye"></i> View ${ds.acronym.en}
                                  </a>
                                </div>
                              </div>
                            </div>
                          </div>
                        </#list>
                      </div>
                    </div>
                  </div>

                </div>
              </div>
            </#if>
          </div>
        </div>
        <!-- /.row -->

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
    $(function () {
        $("#datasets").DataTable(dataTablesDefaultOpts);
    });
</script>
</body>
</html>
