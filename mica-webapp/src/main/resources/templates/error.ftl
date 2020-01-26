<#include "libs/members.ftl">
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Error</title>
    <#include "libs/head.ftl">
  <!-- Ionicons -->
  <link rel="stylesheet" href="https://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css">
</head>
<body class="hold-transition layout-top-nav">
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
            <h1 class="m-0">Error</h1>
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
            <h3><i class="fas fa-exclamation-triangle text-warning"></i> ${message!"Oops! Page not found."}</h3>

            <p>
              We could not find the page you were looking for.
              Meanwhile, you may <a href="../dashboard">return to dashboard</a> or try to <a href="../catalog">search the catalog</a>.
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
