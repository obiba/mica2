<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>${config.name!""} | <@message "sign-in"/></title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body id="signin-page" class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href="${contextPath}/"><b>${config.name!""}</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div id="signInCard" class="card-body login-card-body">
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
          <input name="password" type="password" autocomplete="off" class="form-control" placeholder="<@message "password"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-lock"></span>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-6">

          </div>
          <!-- /.col -->
          <div class="col-6">
            <button type="submit" class="btn btn-primary btn-block">
              <i class="spinner-border spinner-border-sm" style="display: none;"></i> <@message "sign-in-submit"/>
            </button>
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
        <a href="forgot-password"><@message "forgot-password"/></a>
      </p>
      <#if config?? && config.signupEnabled>
        <p class="mb-0">
          <a href="signup" class="text-center"><@message "register-new-membership"/></a>
        </p>
      </#if>
    </div>
    <!-- /.login-card-body -->
    <div id="2faCard" class="card-body 2fa-card-body" style="display: none;">
      <p class="login-box-msg"><@message "2fa-caption"/></p>

      <div>
        <div class="input-group mb-3">
          <input id="otp" name="otp" type="number" class="form-control"/>
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-mobile"></span>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-6">
            <button class="btn btn-primary btn-block" onclick="validateOtp()">
              <i class="spinner-border spinner-border-sm" style="display: none;"></i> <@message "validate"/>
            </button>
          </div>
          <!-- /.col -->
          <div class="col-6">
            <button class="btn btn-default btn-block" onclick="cancelOtp()">
                <@message "cancel"/>
            </button>
          </div>
          <!-- /.col -->
        </div>
      </div>
    </div>
    <!-- /.2fa-card-body -->
  </div>
</div>
<!-- /.login-box -->

<#include "libs/scripts.ftl">
<#include "libs/signin-scripts.ftl">

</body>
</html>
