<!-- Macros -->
<#include "models/project.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "approved-projects"/></title>
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
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

        <div class="row">
          <div class="col-lg-12">
            <#if projects?? && projects?size != 0>
              <div class="card card-info card-outline">
                <div class="card-header d-flex p-0">
                  <h3 class="card-title p-3"><@message "projects"/></h3>
                </div><!-- /.card-header -->
                <div class="card-body">
                  <#list projects as project>
                    <div class="border-bottom mb-3">
                      <p>
                        <strong><i class="far fa-file-alt"></i> ${project.title[.lang]!""}</strong>
                      </p>
                      <p class="text-muted">${project.summary[.lang]!""}</p>
                      <p>
                        <@projectModel project=project/>
                      </p>
                    </div>
                  </#list>
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

</body>
</html>
