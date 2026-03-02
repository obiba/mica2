<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/studies.ftl">

<!-- Template variables -->
<#if !type??>
  <#assign title = "studies">
  <#assign showTypeColumn = true>
<#elseif type == "Harmonization">
  <#assign title = "harmonization-studies">
  <#assign showTypeColumn = false>
<#else>
  <#assign title = "individual-studies">
  <#assign showTypeColumn = false>
</#if>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message title/></title>
</head>
<body id="${title}-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <@header title=title breadcrumb=[["${contextPath}/", "home"], [title]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div id="${title}-callout" class="callout callout-info">
          <p><@message (title + "-callout")/></p>
        </div>

        <#if studies?? && studies?size != 0>
          <div id="${title}-card" class="card card-info card-outline">

            <div class="card-header">
              <h3 class="card-title <#if studyListDisplays?size gt 1>pt-2</#if>"><span>${studies?size} <@message title/></span></h3>
              <#if studyListDisplays?size gt 1>
                <ul class="nav nav-pills ms-auto float-end">
                  <#list studyListDisplays as display>
                    <#if display == "table">
                      <li class="nav-item"><a class="nav-link <#if studyListDefaultDisplay == "table">active</#if>" href="#table" data-bs-toggle="tab">
                          <i class="fa-solid fa-table"></i></a>
                      </li>
                    </#if>
                    <#if display == "lines">
                      <li class="nav-item"><a class="nav-link <#if studyListDefaultDisplay == "lines">active</#if>" href="#lines" data-bs-toggle="tab">
                          <i class="fa-solid fa-grip-lines"></i></a>
                      </li>
                    </#if>
                    <#if display == "cards">
                      <li class="nav-item"><a class="nav-link <#if studyListDefaultDisplay == "cards">active</#if>" href="#cards" data-bs-toggle="tab">
                          <i class="fa-solid fa-grip-horizontal"></i></a>
                      </li>
                    </#if>
                  </#list>
                </ul>
              </#if>
            </div><!-- /.card-header -->

            <div class="card-body">
              <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
                <div id="studyFilter"  class="mb-4 d-none">
                  <div role="group" class="btn-group">
                    <button onclick="window.location.href='${contextPath}/studies';" type="button" class="btn btn-sm btn-info <#if !type??>active</#if>"><@message "all"/></button>
                    <button onclick="window.location.href='${contextPath}/individual-studies';" type="button" class="btn btn-sm btn-info <#if type?? && type == "Individual">active</#if>"><@message "individual"/></button>
                    <button onclick="window.location.href='${contextPath}/harmonization-studies';" type="button" class="btn btn-sm btn-info <#if type?? && type == "Harmonization">active</#if>"><@message "harmonization"/></button>
                  </div>
                </div>
              </#if>

              <div class="tab-content">
                <#if studyListDisplays?seq_contains("table")>
                  <div class="tab-pane <#if studyListDefaultDisplay == "table">active</#if>" id="table">
                    <div class="table-responsive">
                      <table id="${title}" class="table table-bordered table-striped">
                        <thead>
                        <@studyTableHeadModel/>
                        </thead>
                        <tbody>
                        <#list studies as std>
                          <@studyTableRowModel study=std/>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </#if>

                <#if studyListDisplays?seq_contains("lines")>
                  <div class="tab-pane <#if studyListDefaultDisplay == "lines">active</#if>" id="lines">
                    <#list studies as std>
                      <div class="border-bottom mb-3 pb-3" style="min-height: 150px;">
                        <div class="row">
                          <@studyLineModel study=std/>
                        </div>
                      </div>
                    </#list>
                  </div>
                </#if>

                <#if studyListDisplays?seq_contains("cards")>
                  <div class="tab-pane <#if studyListDefaultDisplay == "cards">active</#if>" id="cards" v-cloak>
                    <@studyCardModel/>
                  </div>
                </#if>

              </div>
            </div>
          </div>
        <#else>
          <div id="${title}-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "studies"/></h3>
            </div><!-- /.card-header -->
            <div class="card-body">
              <#if config.openAccess || user??>
                <p class="text-muted"><@message "no-studies"/></p>
              <#else>
                <p class="text-muted"><@message "sign-in-studies"/></p>
                <button type="button" onclick="location.href='${contextPath}/signin?redirect=${contextPath}/<#if type??>${type?lower_case}-</#if>studies';" class="btn btn-success btn-lg">
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
<#include "libs/studies-scripts.ftl">
<script>
  if (!window.location.href.match('(individual|harmonization)-')) {
    document.querySelector('#studyFilter').classList.remove('d-none');
  }
</script>
</body>
</html>
