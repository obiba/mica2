<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>${config.name!""} | <@message "sign-up"/></title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <script type="text/javascript">
    var onloadCallback = function() {
      grecaptcha.render('html_element', {
        'sitekey' : '${authConfig.reCaptchaKey}'
      });
    };
  </script>
</head>
<body id="signup-page" class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href="${contextPath}/"><b>${config.name!""}</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div class="card-body login-card-body">
      <p class="login-box-msg"><@message "sign-up-caption"/></p>

      <div id="alertFailure" class="alert alert-danger d-none">
        <small><@message "sign-up-auth-failed"/></small>
      </div>

      <form id="form" method="post">

        <#if authConfig.joinWithUsername>
          <div class="input-group mb-3">
            <input name="username" type="text" class="form-control" placeholder="<@message "username"/>">
            <div class="input-group-append">
              <div class="input-group-text">
                <span class="fas fa-user"></span>
              </div>
            </div>
          </div>
        </#if>
        <div class="input-group mb-3">
          <input name="email" type="email" class="form-control" placeholder="<@message "email"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-envelope"></span>
            </div>
          </div>
        </div>
        <#if config.signupWithPassword>
          <div class="input-group mb-3">
            <input name="password" type="password" class="form-control" placeholder="<@message "password"/>">
            <div class="input-group-append">
              <div class="input-group-text">
                <span class="fas fa-lock"></span>
              </div>
            </div>
          </div>
          <p class="text-sm"><@message "password-helper"/></p>
        </#if>

        <div class="text-center">
          <p>- <@message "personal-information"/> -</p>
        </div>

        <div class="input-group mb-3">
          <input name="firstname" type="text" class="form-control" placeholder="<@message "firstname"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-user"></span>
            </div>
          </div>
        </div>
        <div class="input-group mb-3">
          <input name="lastname" type="text" class="form-control" placeholder="<@message "lastname"/>">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-user"></span>
            </div>
          </div>
        </div>

        <#if authConfig.languages?size gt 1>
          <div class="form-group mb-3">
            <label><@message "preferred-language"/></label>
            <select class="form-control" name="locale">
              <#list authConfig.languages as language>
                <option value="${language}"><@message language/></option>
              </#list>
            </select>
          </div>
        <#else>
          <input type="hidden" name="locale" value="${authConfig.languages[0]!"en"}"/>
        </#if>

        <#list authConfig.userAttributes as attribute>
          <div class="form-group mb-3">
            <#if attribute.inputType == "checkbox">
              <div class="form-check">
                <input name="${attribute.name}" type="checkbox" value="true" class="form-check-input" id="${attribute.name}">
                <label class="form-check-label" for="${attribute.name}"><@message attribute.name/></label>
              </div>
            <#elseif attribute.values?size != 0>
              <label><@message attribute.name/></label>
              <select class="form-control" name="${attribute.name}">
                <#list attribute.values as value>
                  <option value="${value}"><@message value/></option>
                </#list>
              </select>
            <#else>
              <input name="${attribute.name}" type="${attribute.inputType}" class="form-control" placeholder="<@message attribute.name/>">
            </#if>
          </div>
        </#list>

        <div id="html_element" class="mb-3"></div>
        <div class="row">
          <div class="col-6">
          </div>
          <!-- /.col -->
          <div class="col-6">
            <button type="submit" class="btn btn-primary btn-block">
              <i class="spinner-border spinner-border-sm" style="display: none;"></i> <@message "sign-up-submit"/>
            </button>
          </div>
          <!-- /.col -->
        </div>
      </form>

      <script src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit"
              async defer>
      </script>

      <#if oidcProviders?? && oidcProviders?size != 0>
        <div class="social-auth-links text-center mb-3">
          <p>- <@message "sign-up-or"/> -</p>
          <#list oidcProviders as oidc>
            <a href="${oidc.url}" class="btn btn-block btn-primary">
              <@message "sign-up-with"/> ${oidc.title}
            </a>
          </#list>
        </div>
        <!-- /.social-auth-links -->
      </#if>

      <p class="mb-1">
        <a href="signin" class="text-center"><@message "already-have-a-membership"/></a>
      </p>
    </div>
    <!-- /.login-card-body -->
  </div>
</div>
<!-- /.login-box -->

<#include "libs/scripts.ftl">
<script>
  const requiredFields = [
    { name: "email", title: "<@message "email"/>" },
    <#if config.signupWithPassword>
      { name: "password", title: "<@message "password"/>" },
    </#if>
    { name: "firstname", title: "<@message "firstname"/>" },
    { name: "lastname", title: "<@message "lastname"/>" },
    <#if authConfig.joinWithUsername>
      { name: "username", title: "<@message "username"/>" },
    </#if>
    <#list authConfig.userAttributes as attribute>
      <#if attribute.required>
        { name: "${attribute.name}", title: "<@message attribute.name/>" },
      </#if>
    </#list>
    { name: "g-recaptcha-response", title: "<@message "captcha"/>" }
  ];
</script>
<#include "libs/signup-scripts.ftl">

</body>
</html>
