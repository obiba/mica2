<!-- Macros -->
<#include "models/project.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "approved-projects"/></title>
</head>
<body id="projects-page" class="hold-transition layout-top-nav layout-navbar-fixed">
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
            <h1 class="m-0"><@message "approved-projects"/></h1>
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
        <div class="callout callout-info">
          <p><@message "projects-callout"/></p>
        </div>

        <#if projects?? && projects?size != 0>
          <div id="projects-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "projects"/></h3>
            </div><!-- /.card-header -->
            <div class="card-body">
              <#list projects as project>
                <div class="border-bottom mb-3">
                  <p>
                    <strong><i class="${projectIcon}"></i> ${localize(project.title)}</strong>
                  </p>
                  <p class="text-muted marked">${localize(project.summary)?trim?truncate_w(300, "...")}</p>
                  <p>
                    <@projectModelSummary project=project/>
                  </p>
                  <div class="mt-2 mb-3">
                    <a href="${contextPath}/project/${project.id}" class="btn btn-sm btn-outline-info">
                      <@message "global.read-more"/>
                    </a>
                  </div>
                </div>
              </#list>
            </div>
          </div>
        <#else>
          <div id="projects-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "projects"/></h3>
            </div><!-- /.card-header -->
            <div class="card-body">
              <#if config.openAccess || user??>
                <p class="text-muted"><@message "no-approved-projects"/></p>
              <#else>
                <p class="text-muted"><@message "sign-in-approved-projects"/></p>
                <button type="button" onclick="location.href='${contextPath}/signin?redirect=${contextPath}/projects';" class="btn btn-success btn-lg">
                  <i class="fas fa-sign-in-alt"></i> <@message "sign-in"/>
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

</body>
</html>
