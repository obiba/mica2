<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/networks.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "networks"/></title>
</head>
<body id="networks-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
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

            <div class="card-header">
              <h3 class="card-title <#if networkListDisplays?size gt 1>pt-2</#if>"><span>${networks?size} <@message "networks"/></span></h3>
              <#if networkListDisplays?size gt 1>
                <ul class="nav nav-pills ms-auto float-end">
                  <#list networkListDisplays as display>
                    <#if display == "table">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "table">active</#if>" href="#table" data-bs-toggle="tab">
                          <i class="fa-solid fa-table"></i></a>
                      </li>
                    </#if>
                    <#if display == "lines">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "lines">active</#if>" href="#lines" data-bs-toggle="tab">
                          <i class="fa-solid fa-grip-lines"></i></a>
                      </li>
                    </#if>
                    <#if display == "cards">
                      <li class="nav-item"><a class="nav-link <#if networkListDefaultDisplay == "cards">active</#if>" href="#cards" data-bs-toggle="tab">
                          <i class="fa-solid fa-grip-horizontal"></i></a>
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
                    <div class="table-responsive">
                      <table id="networks" class="table table-bordered table-striped">
                        <thead>
                        <@networkTableHeadModel/>
                        </thead>
                        <tbody>
                        <#list networks as ntwk>
                          <@networkTableRowModel network=ntwk/>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </#if>

                <#if networkListDisplays?seq_contains("lines")>
                  <div class="tab-pane <#if networkListDefaultDisplay == "lines">active</#if>" id="lines">
                    <#list networks as ntwk>
                      <div class="border-bottom mb-3 pb-3" style="min-height: 150px;">
                        <div class="row">
                          <@networkLineModel network=ntwk/>
                        </div>
                      </div>
                    </#list>
                  </div>
                </#if>

                <#if networkListDisplays?seq_contains("cards")>
                  <div class="tab-pane <#if networkListDefaultDisplay == "cards">active</#if>" id="cards" v-cloak>
                    <@networkCardModel/>
                  </div>
                </#if>

              </div>
            </div>
          </div>
        <#else>
          <div id="networks-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "networks"/></h3>
            </div><!-- /.card-header -->
            <div class="card-body">
              <#if config.openAccess || user??>
                <p class="text-muted"><@message "no-networks"/></p>
              <#else>
                <p class="text-muted"><@message "sign-in-networks"/></p>
                <button type="button" onclick="location.href='${contextPath}/signin?redirect=${contextPath}/networks';" class="btn btn-success btn-lg">
                  <i class="fa-solid fa-sign-in-alt"></i> <@message "sign-in"/>
                </button>
              </#if>
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
<#include "libs/networks-scripts.ftl">

</body>
</html>
