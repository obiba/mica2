<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "networks"/></title>
</head>
<body id="networks-page" class="hold-transition layout-top-nav">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title="networks" breadcrumb=[["${contextPath}/", "home"], ["networks"]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div id="networks-callout" class="callout callout-info">
          <p><@message "networks-callout"/></p>
        </div>

        <#if networks?size != 0>
          <div id="networks-card" class="card card-info card-outline">

            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "networks"/></h3>
              <#if networkListDisplays?size gt 1>
                <ul class="nav nav-pills ml-auto p-2">
                  <#list networkListDisplays as display>
                    <#if display == "table">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "table">active</#if>" href="#table" data-toggle="tab">
                          <i class="fas fa-table"></i></a>
                      </li>
                    </#if>
                    <#if display == "lines">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "lines">active</#if>" href="#lines" data-toggle="tab">
                          <i class="fas fa-grip-lines"></i></a>
                      </li>
                    </#if>
                    <#if display == "cards">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "cards">active</#if>" href="#cards" data-toggle="tab">
                          <i class="fas fa-grip-horizontal"></i></a>
                      </li>
                    </#if>
                  </#list>
                </ul>
              </#if>
            </div><!-- /.card-header -->

            <div class="card-body">
              <div class="tab-content">
                <#if networkListDisplays?seq_contains("table")>
                  <div class="tab-pane <#if networkListDefaultDisplay == "table">active</#if>" id="table">
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
                          <td><a href="${contextPath}/network/${ntwk.id}">${localize(ntwk.acronym)}</a></td>
                          <td><small>${localize(ntwk.name)}</small></td>
                          <td class="marked"><small>${localize(ntwk.description)?trim?truncate_w(100, "...")}</small></td>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                </#if>

                <#if networkListDisplays?seq_contains("lines")>
                  <div class="tab-pane <#if networkListDefaultDisplay == "lines">active</#if>" id="lines">
                    <#list networks as ntwk>
                      <div class="border-bottom mb-3 pb-3" style="min-height: 150px;">
                        <div class="row">
                          <div class="col-lg-3 col-sm-12">
                            <#if ntwk.logo??>
                              <img class="img-fluid" style="max-height: 150px" alt="${localize(ntwk.acronym)} logo" src="${contextPath}/ws/network/${ntwk.id}/file/${ntwk.logo.id}/_download"/>
                            <#else >
                              <div class="text-black-50 text-center mt-5">
                                <i class="${networkIcon} fa-3x"></i>
                              </div>
                            </#if>
                          </div>
                          <div class="col-lg-9 col-sm-12">
                            <h2 class="lead"><b>${localize(ntwk.acronym)}</b></h2>
                            <p class="text-muted text-sm">${localize(ntwk.name)}</p>
                            <div class="marked">
                              ${localize(ntwk.description)?trim?truncate_w(200, "...")}
                            </div>
                            <div class="mt-2">
                              <a href="${contextPath}/network/${ntwk.id}" class="btn btn-sm btn-outline-info">
                                <@message "global.read-more"/>
                              </a>
                            </div>
                          </div>
                        </div>
                      </div>
                    </#list>
                  </div>
                </#if>

                <#if networkListDisplays?seq_contains("cards")>
                  <div class="tab-pane <#if networkListDefaultDisplay == "cards">active</#if>" id="cards">
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
                                      <img class="img-fluid" style="max-height: 200px" alt="${localize(ntwk.acronym)} logo" src="${contextPath}/ws/network/${ntwk.id}/file/${ntwk.logo.id}/_download"/>
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
                                <a href="${contextPath}/network/${ntwk.id}" class="btn btn-sm btn-outline-info">
                                  <i class="fas fa-eye"></i> <@message "global.read-more"/>
                                </a>
                              </div>
                            </div>
                          </div>
                        </div>
                      </#list>
                    </div>
                  </div>
                </#if>

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
    });
</script>
</body>
</html>
