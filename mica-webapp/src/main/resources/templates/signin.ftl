<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>${config.name!""} | <@message "sign-in"/></title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href=".."><b>${config.name!""}</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div class="card-body login-card-body">
      <p class="login-box-msg"><@message "sign-in-caption"/></p>

      <div id="alertFailure" class="alert alert-danger d-none">
        <small><@message "sign-in-auth-failed"/></small>
      </div>

      <div id="alertBanned" class="alert alert-warning d-none">
        <small><@message "sign-in-too-many-failures"/></small>
      </div>

      <form id="form" method="post">
        <div class="input-group mb-3">
          <input name="username" type="text" class="form-control" placeholder="<@message "sign-in-username"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-envelope"></span>
            </div>
          </div>
        </div>
        <div class="input-group mb-3">
          <input name="password" type="password" class="form-control" placeholder="<@message "password"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-lock"></span>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-8">

          </div>
          <!-- /.col -->
          <div class="col-4">
            <button type="submit" class="btn btn-primary btn-block"><@message "sign-in-submit"/></button>
          </div>
          <!-- /.col -->
        </div>
      </form>

      <#if oidcProviders?? && oidcProviders?size != 0>
        <div class="social-auth-links text-center mb-3">
          <p>- <@message "sign-in-or"/> -</p>
          <#list oidcProviders as oidc>
            <a href="${oidc.url}" class="btn btn-block btn-primary">
              <@message "sign-in-with"/> ${oidc.title}
            </a>
          </#list>
        </div>
        <!-- /.social-auth-links -->
      </#if>

      <p class="mb-1">
        <a href="reset-password"><@message "forgot-password"/></a>
      </p>
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
  micajs.signin("#form", function (banned) {
    var alertId = banned ? "#alertBanned" : "#alertFailure";
    $(alertId).removeClass("d-none");
    setTimeout(function() {
        $(alertId).addClass("d-none");
    }, 5000);
  });
</script>

</body>
</html>
