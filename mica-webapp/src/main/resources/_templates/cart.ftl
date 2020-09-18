<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "sets.cart.title"/></title>
</head>
<body id="cart-page" class="hold-transition layout-top-nav layout-navbar-fixed">
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
            <h1 class="m-0"><@message "sets.cart.title"/></h1>
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
          <p><@message "sets.cart.help"/></p>
        </div>

        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "variables"/></h3>
          </div>
          <div class="card-body">
            <#if user.variablesCart?? && user.variablesCart.count gt 0>
              <a class="btn btn-info" href="${contextPath}/search#lists?type=variables&query=variable(in(Mica_variable.sets,${user.variablesCart.id}))">
                <i class="fas fa-search"></i>
              </a>
            <#else>
              <div class="text-muted"><@message "sets.cart.no-variables"/></div>
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
