<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "contact-title"/></title>
  <script type="text/javascript">
    var onloadCallback = function() {
      grecaptcha.render('html_element', {
        'sitekey' : '${reCaptchaKey}'
      });
    };
  </script>
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
            <h1 class="m-0"><@message "contact-title"/></h1>
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
        <div id="contact-callout" class="callout callout-info">
          <p><@message "contact-callout"/></p>
        </div>
        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "contact-card-title"/></h3>
          </div>
          <div class="card-body">

            <div id="alertFailure" class="alert alert-danger d-none">
              <small><@message "contact-failed"/></small>
            </div>

            <form id="form" method="post">
              <div class="form-group">
                <label for="contact-name"><@message "contact-name"/></label>
                <input name="name" type="text" class="form-control" id="contact-name" <#if user??>value="${user.fullName!""}"</#if>>
              </div>
              <div class="form-group">
                <label for="contact-email"><@message "contact-email"/></label>
                <input name="email" type="email" class="form-control" id="contact-email" <#if user?? && user.attributes??>value="${user.attributes["email"]!""}"</#if>>
              </div>
              <div class="form-group">
                <label for="contact-subject"><@message "contact-subject"/></label>
                <input name="subject" type="text" class="form-control" id="contact-subject">
              </div>
              <div class="form-group">
                <label for="contact-message"><@message "contact-message"/></label>
                <textarea class="form-control" id="contact-message" name="message" rows="6"></textarea>
              </div>
              <div id="html_element" class="mb-3"></div>
              <button type="submit" class="btn btn-primary"><@message "contact-send"/></button>
            </form>
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
<script src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit&hl=${.lang}"
        async defer>
</script>
<#include "libs/contact-scripts.ftl">

</body>
</html>
