<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/index.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <title>${config.name!""}</title>
  <#include "libs/head.ftl">
</head>
<body id="index-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->


  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">

    <div class="jumbotron jumbotron-fluid">
      <div class="container">
        <#if config.repositoryEnabled && config.dataAccessEnabled>
          <h1 class="display-4"><@message "data-portal-title"/></h1>
          <p class="lead"><@message "data-portal-text"/></p>
        <#elseif config.repositoryEnabled>
          <h1 class="display-4"><@message "data-repository-portal-title"/></h1>
          <p class="lead"><@message "data-repository-portal-text"/></p>
        <#elseif config.dataAccessEnabled>
          <h1 class="display-4"><@message "data-access-portal-title"/></h1>
          <p class="lead"><@message "data-access-portal-text"/></p>
        </#if>
      </div>
    </div>

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <@homeModel/>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/index-scripts.ftl">

</body>
</html>
