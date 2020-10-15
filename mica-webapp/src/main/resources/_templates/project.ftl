<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/project.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(project.title)}</title>
</head>
<body id="project-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <h1 class="m-0"><span class="text-white-50"><@message "approved-project"/> / </span>${localize(project.title)}</h1>
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if draft>
          <div class="alert alert-warning" role="alert">
            <i class="icon fas fa-exclamation-triangle"></i> <@messageArgs code="viewing-draft-version" args=["/project/${project.id}"]/>
          </div>
        </#if>

        <div class="card card-info card-outline">
          <div class="card-header d-flex p-0">
            <h3 class="card-title p-3"><@message "overview"/></h3>
          </div><!-- /.card-header -->
          <div class="card-body">
            <p class="text-muted marked">${localize(project.summary)}</p>
          </div>
        </div>

        <@projectModel project=project/>

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
