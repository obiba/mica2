<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "just-registered"/></title>
</head>
<body id="just-registered-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0"><@message "just-registered"/></h1>
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

          <#if canSignin>
            <p><@message "just-registered-signin-callout"/></p>
            <button type="button" onclick="location.href='${contextPath}/signin';" class="btn btn-success btn-lg">
              <i class="fa-solid fa-sign-in-alt"></i> <@message "sign-in"/>
            </button>
          <#else>
            <p><@message "just-registered-callout"/></p>
          </#if>
        </div>
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
