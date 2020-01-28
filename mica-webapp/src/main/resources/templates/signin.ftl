<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>Example | Sign in</title>
  <!-- Tell the browser to be responsive to screen width -->
  <meta name="viewport" content="width=device-width, initial-scale=1">

  <#include "libs/head.ftl">
  <!-- icheck bootstrap -->
  <link rel="stylesheet" href="../bower_components/admin-lte/plugins/icheck-bootstrap/icheck-bootstrap.min.css">
</head>
<body class="hold-transition login-page">
<div class="login-box">
  <div class="login-logo">
    <a href="../home"><b>Example</b></a>
  </div>
  <!-- /.login-logo -->
  <div class="card">
    <div class="card-body login-card-body">
      <p class="login-box-msg">Sign in to start your session</p>

      <div id="alertFailure" class="alert alert-danger d-none">
        <small>Authentication failed. Please verify credentials.</small>
      </div>

      <div id="alertBanned" class="alert alert-warning d-none">
        <small>Too many failures. Please try again later.</small>
      </div>

      <form id="form" method="post">
        <div class="input-group mb-3">
          <input name="username" type="text" class="form-control" placeholder="Username or Email">
          <div class="input-group-append">
            <div class="input-group-text">
              <span class="fas fa-envelope"></span>
            </div>
          </div>
        </div>
        <div class="input-group mb-3">
          <input name="password" type="password" class="form-control" placeholder="Password">
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
            <button type="submit" class="btn btn-primary btn-block">Sign In</button>
          </div>
          <!-- /.col -->
        </div>
      </form>

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
        <a href="forgot-password">I forgot my password</a>
      </p>
      <p class="mb-0">
        <a href="signup" class="text-center">Register a new membership</a>
      </p>
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
