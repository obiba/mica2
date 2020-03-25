<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "datasets"/></title>
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Template variables -->
  <#if !type??>
    <#assign title = "datasets">
    <#assign callout = "datasets-callout">
    <#assign showTypeColumn = true>
  <#elseif type == "Harmonized">
    <#assign title = "harmonized-datasets">
    <#assign callout = "harmonized-datasets-callout">
    <#assign showTypeColumn = false>
  <#else>
    <#assign title = "collected-datasets">
    <#assign callout = "collected-datasets-callout">
    <#assign showTypeColumn = false>
  </#if>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title=title breadcrumb=[["..", "home"], [title]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p><@message callout/></p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <#if datasets?? && datasets?size != 0>
              <div class="card card-info card-outline">

                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3"><@message "datasets"/></h3>
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
                      <table id="datasets" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th><@message "acronym"/></th>
                          <th><@message "name"/></th>
                          <th><@message "description"/></th>
                          <#if showTypeColumn>
                            <th><@message "type"/></th>
                          </#if>
                        </tr>
                        </thead>
                        <tbody>
                        <#list datasets as ds>
                          <tr>
                            <td><a href="../dataset/${ds.id}">${localize(ds.acronym)}</a></td>
                            <td><small>${localize(ds.name)}</small></td>
                            <td><small>${localize(ds.description)?trim?truncate_w(100, "...")}</small></td>
                            <#if showTypeColumn>
                              <td>
                                <#if ds.class.simpleName == "HarmonizationDataset">
                                  <@message "harmonized"/>
                                <#else>
                                  <@message "collected"/>
                                </#if>
                              </td>
                            </#if>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>

                    <div class="tab-pane <#if datasetListDefaultDisplay == "cards">active</#if>" id="cards">
                      <div class="row d-flex align-items-stretch">
                        <#list datasets as ds>
                          <div class="col-12 col-sm-6 col-md-4 d-flex align-items-stretch">
                            <div class="card bg-light w-100">
                              <div class="card-header text-dark border-bottom-0">
                                <h2 class="lead"><b>${localize(ds.acronym)}</b></h2>
                              </div>
                              <div class="card-body pt-0">
                                <div class="row">
                                  <div class="col-7">
                                    <p class="text-muted text-sm">${localize(ds.name)}</p>
                                  </div>
                                  <div class="col-5 text-center">
                                    <p class="text-black-50 text-center mr-5 ml-5 pr-5">
                                      <#if ds.class.simpleName == "HarmonizationDataset">
                                        <i class="${harmoDatasetIcon} fa-3x"></i>
                                      <#else>
                                        <i class="${datasetIcon} fa-3x"></i>
                                      </#if>
                                    </p>
                                  </div>
                                </div>
                              </div>
                              <div class="card-footer">
                                <div class="text-right">
                                  <a href="../dataset/${ds.id}" class="btn btn-sm btn-outline-info">
                                    <i class="fas fa-eye"></i> ${localize(ds.acronym)}
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
