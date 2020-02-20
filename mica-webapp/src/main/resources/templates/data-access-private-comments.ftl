<!-- Macros -->
<#include "models/data-access-comments.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
    <#include "libs/data-access-comments-head.ftl">
  <title>${config.name!""} | Data Access Comments ${dar.id}</title>
</head>
<body class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
    <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Sidebar -->
    <#include "libs/data-access-sidebar.ftl">
  <!-- /.sidebar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50">Data Access Private Comments /</span> ${dar.id}
            </h1>
          </div>
          <div class="col-sm-6">
              <#include "libs/data-access-breadcrumb.ftl">
          </div>
        </div>
      </div><!-- /.container-fluid -->
    </section>

    <!-- Main content -->
    <section class="content">

      <div class="row">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              These are the private comments (i.e. visible only by data access officers and administrators) related to
              the data access request.
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <!-- Timelime of comments  -->
      <div class="row">
        <div class="col-sm-12 col-lg-6">
            <@commentsTimeline isPrivate="true"/>
        </div>
        <!-- /.col -->
      </div>

      <div class="row">
        <div class="col-sm-12 col-lg-6">
          <div class="card timeline-block">
            <div class="card-body p-0">
              <textarea id="comment-add-write-text"></textarea>
            </div><!-- /.card-body -->
            <div class="card-footer">
              <button id="send-comment" type="button" class="btn btn-secondary float-right">
                <i class="fa fa-paper-plane"></i> <@message "send-comment"/>
              </button>
            </div><!-- /.card-footer -->
          </div>

        </div>
      </div>

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/data-access-comments-scripts.ftl">
<@commentScripts isPrivate="true"/>
<script>
    $(function () {
        $('#private-comments-menu').addClass('active').attr('href', '#');
    });
</script>

</body>
</html>
