<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>${config.name!""} | <@message "sign-in"/></title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body id="forgot-password-page" class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href="${contextPath}/"><b>${config.name!""}</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div class="card-body login-card-body">
      <p class="login-box-msg"><@message "reset-password"/></p>

      <div id="alertFailure" class="alert alert-danger d-none">
        <small><@message "reset-password-failed"/></small>
      </div>

      <form id="form" method="post">
        <div class="input-group mb-3">
          <input name="username" type="text" class="form-control" placeholder="<@message "sign-in-username"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fa-solid fa-envelope"></span>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-6">
          </div>
          <!-- /.col -->
          <div class="col-6">
            <button type="submit" class="btn btn-primary btn-block"><@message "reset"/></button>
          </div>
          <!-- /.col -->
        </div>
      </form>

      <#if config?? && config.signupEnabled>
        <p class="mb-0">
          <a href="signup" class="text-center"><@message "register-new-membership"/></a>
        </p>
      </#if>
    </div>
    <!-- /.login-card-body -->
  </div>
</div>
<!-- /.login-box -->

<#include "libs/scripts.ftl">

<script>
  UserService.forgotPassword("#form", function() {
    var alertId = "#alertFailure";
    $(alertId).removeClass("d-none");
    setTimeout(function() {
      $(alertId).addClass("d-none");
    }, 5000);
  });
</script>

</body>
</html>
