<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>${config.name!""} | Sign up</title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <#include "libs/head.ftl">
  <script type="text/javascript">
    var onloadCallback = function() {
      grecaptcha.render('html_element', {
        'sitekey' : '6LdANggTAAAAAEBWCuEL-YkFd3ehtzlg4CzQpn3e'
      });
    };
  </script>
</head>
<body class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href="../home"><b>${config.name!""}</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div class="card-body login-card-body">
      <p class="login-box-msg">Register a new membership</p>

      <form action="../home" method="post">
        <div class="input-group mb-3">
          <input type="text" class="form-control" placeholder="Full name">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-user"></span>
            </div>
          </div>
        </div>
        <div class="input-group mb-3">
          <input type="email" class="form-control" placeholder="Email">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-envelope"></span>
            </div>
          </div>
        </div>
        <div class="input-group mb-3">
          <input type="password" class="form-control" placeholder="Password">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-lock"></span>
            </div>
          </div>
        </div>
        <div id="html_element" class="mb-3"></div>
        <div class="row">
          <div class="col-8">
          </div>
          <!-- /.col -->
          <div class="col-4">
            <button type="submit" class="btn btn-primary btn-block">Sign Up</button>
          </div>
          <!-- /.col -->
        </div>
      </form>
      <script src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit"
              async defer>
      </script>

      <#if oauthProviders?? && oauthProviders?size != 0>
        <div class="social-auth-links text-center mb-3">
          <p>- OR -</p>
            <#list oauthProviders as oaut>
              <a href="#" class="btn btn-block btn-primary">
                Sign in using ${oauth.name}
              </a>
            </#list>
        </div>
        <!-- /.social-auth-links -->
      </#if>

      <p class="mb-1">
        <a href="signin" class="text-center">I already have a membership</a>
      </p>
    </div>
    <!-- /.login-card-body -->
  </div>
</div>
<!-- /.login-box -->

<#include "libs/scripts.ftl">

</body>
</html>
