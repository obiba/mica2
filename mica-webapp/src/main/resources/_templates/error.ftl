<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title><@message "error"/></title>
</head>
<body id="error-page" class="hold-transition layout-top-nav layout-navbar-fixed">
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
            <h1 class="m-0"><@message "error"/></h1>
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
        <div class="error-page">
          <h2 class="headline text-warning"> ${status!"404"}</h2>

          <div class="error-content">
            <h3><i class="fas fa-exclamation-triangle text-warning"></i>
              <#if msg??>
                  ${msg}
              <#else >
                  "Not found"
              </#if>
            </h3>

            <p>
              We could not access the page you were looking for.
              Meanwhile, you may <a href="..">return to home</a> or try to <a href="../search">search the repository</a>.
            </p>
          </div>
          <!-- /.error-content -->
        </div><!-- /.container-fluid -->
      </div>
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
