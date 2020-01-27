<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Studies</title>
  <#include "libs/head.ftl">
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Template variables -->
  <#if !type??>
    <#assign title = "Studies">
    <#assign callout = "The Studies section includes an inventory of epidemiological studies and of harmonization projects.
            This section can help identify studies with relevant designs that collected information useful in answering specific research questions.
            This section can also learn about current and past harmonization projects. Through access to lists of harmonized variables, users can also learn about the harmonization potential across studies and the data processing applied to generate harmonized data.">
    <#assign showTypeColumn = true>
  <#elseif type == "Harmonization">
    <#assign title = "Harmonization Studies">
    <#assign callout = "The Harmonization Studies section includes an inventory of harmonization projects.
            This section can also learn about current and past harmonization projects. Through access to lists of harmonized variables, users can also learn about the harmonization potential across studies and the data processing applied to generate harmonized data.">
    <#assign showTypeColumn = false>
  <#else>
    <#assign title = "Individual Studies">
    <#assign callout = "The Individual Studies section includes an inventory of epidemiological studies.
            This section can help identify studies with relevant designs that collected information useful in answering specific research questions.">
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
              <li class="breadcrumb-item"><a class="text-white-50" href="#">Home</a></li>
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
            <#if studies?? && studies?size != 0>
              <div class="card card-info card-outline">

                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3">Studies</h3>
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
                      <table id="studies" class="table table-bordered table-striped">
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
                        <#list studies as std>
                          <tr>
                            <td><a href="../study/${std.id}">${std.acronym.en}</a></td>
                            <td><small>${std.name.en}</small></td>
                            <td><small>${std.objectives.en?trim?truncate_w(100, "...")}</small></td>
                            <#if showTypeColumn>
                              <td>
                                <#if std.class.simpleName == "HarmonizationStudy">
                                  Harmonization
                                <#else>
                                  Individual
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
                        <#list studies as std>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${std.acronym.en}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${std.name.en}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                      <#if std.logo??>
                                        <img class="img-fluid" style="max-height: 200px" alt="${std.acronym.en} logo" src="../ws/study/${std.id}/file/${std.logo.id}/_download"/>
                                      <#else >
                                        <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                          <i class="ion ion-folder fa-4x"></i>
                                        </p>
                                      </#if>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../study/${std.id}" class="btn btn-sm btn-primary">
                                    <i class="fas fa-eye"></i> View ${std.acronym.en}
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
        $("#studies").DataTable(dataTablesDefaultOpts);
    });
</script>
</body>
</html>
