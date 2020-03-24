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
                    <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "table">active</#if>" href="#table" data-toggle="tab">
                        <i class="fas fa-grip-lines"></i></a>
                    </li>
                    <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "cards">active</#if>" href="#cards" data-toggle="tab">
                        <i class="fas fa-grip-horizontal"></i></a>
                    </li>
                  </ul>
                </div><!-- /.card-header -->


                <div class="card-body">
                  <div class="tab-content">
                    <div class="tab-pane <#if datasetListDefaultDisplay == "table">active</#if>" id="table">
                      <table id="networks" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th><@message "acronym"/></th>
                          <th><@message "name"/></th>
                          <th><@message "description"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list networks as ntwk>
                          <tr>
                            <td><a href="../network/${ntwk.id}">${localize(ntwk.acronym)}</a></td>
                            <td><small>${localize(ntwk.name)}</small></td>
                            <td><small>${localize(ntwk.description)?trim?truncate_w(100, "...")}</small></td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>

                    <div class="tab-pane <#if datasetListDefaultDisplay == "cards">active</#if>" id="cards">
                      <div class="row d-flex align-items-stretch">
                        <#list networks as ntwk>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light w-100">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${localize(ntwk.acronym)}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${localize(ntwk.name)}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                      <#if ntwk.logo??>
                                        <img class="img-fluid" style="max-height: 200px" alt="${localize(ntwk.acronym)} logo" src="../ws/network/${ntwk.id}/file/${ntwk.logo.id}/_download"/>
                                      <#else >
                                        <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                          <i class="${networkIcon} fa-3x"></i>
                                        </p>
                                      </#if>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../network/${ntwk.id}" class="btn btn-sm btn-outline-info">
                                    <i class="fas fa-eye"></i> ${localize(ntwk.acronym)}
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
