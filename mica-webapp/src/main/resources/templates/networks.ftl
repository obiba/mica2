<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "networks"/></title>
</head>
<body class="hold-transition layout-top-nav">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title="networks" breadcrumb=[["..", "home"], ["networks"]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p><@message "networks-callout"/></p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <#if networks?size != 0>
              <div class="card card-info card-outline">

                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3"><@message "networks"/></h3>
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
                      <table id="networks" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th>Acronym</th>
                          <th>Name</th>
                          <th>Description</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list networks as ntwk>
                          <tr>
                            <td><a href="../network/${ntwk.id}">${ntwk.acronym[.lang]!""}</a></td>
                            <td><small>${ntwk.name[.lang]!""}</small></td>
                            <td><small><#if ntwk.description?? && ntwk.description[.lang]??>${ntwk.description[.lang]?trim?truncate_w(100, "...")}</#if></small></td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>

                    <div class="tab-pane active" id="cards">
                      <div class="row d-flex align-items-stretch">
                        <#list networks as ntwk>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${ntwk.acronym[.lang]!""}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${ntwk.name[.lang]!""}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                      <#if ntwk.logo??>
                                        <img class="img-fluid" style="max-height: 200px" alt="${ntwk.acronym[.lang]!""} logo" src="../ws/network/${ntwk.id}/file/${ntwk.logo.id}/_download"/>
                                      <#else >
                                        <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                          <i class="ion ion-filing fa-4x"></i>
                                        </p>
                                      </#if>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../network/${ntwk.id}" class="btn btn-sm btn-primary">
                                    <i class="fas fa-eye"></i> View ${ntwk.acronym[.lang]!""}
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
<!-- page script -->
<script>
    $(function () {
        $("#networks").DataTable(dataTablesDefaultOpts);
    });
</script>
</body>
</html>
