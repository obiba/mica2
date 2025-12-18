<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "administration"/></title>
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
            <h1 class="m-0"><@message "administration"/></h1>
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


        <div class="row">

          <div class="col-sm-12 col-lg-6">
            <div class="small-box bg-info">
              <div class="inner">
                <h3><@message "content-administration"/></h3>
                <p><@message "admin-content"/></p>
              </div>
              <div class="icon">
                <i class="fa-solid fa-cogs"></i>
              </div>
              <a href="${contextPath}/admin" class="small-box-footer">
                <@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i>
              </a>
            </div>
          </div>
          <div class="col-sm-12 col-lg-6">
            <#if isAdministrator || isReviewer || isEditor>
              <div class="small-box bg-warning">
                <div class="inner">
                  <h3><@message "users-administration"/></h3>
                  <p><@message "admin-users"/></p>
                </div>
                <div class="icon">
                  <i class="fa-solid fa-users"></i>
                </div>
                <a href="${agateUrl}/admin" class="small-box-footer">
                  <@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i>
                </a>
              </div>
            </#if>
          </div>
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
