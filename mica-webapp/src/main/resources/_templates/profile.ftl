<!-- Macros -->
<#include "models/profile.ftl"/>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "profile"/></title>
</head>
<body id="profile-page" class="hold-transition layout-top-nav layout-navbar-fixed">
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
            <h1 class="m-0">${user.fullName}</h1>
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
        <div class="row">
          <div class="col-sm-12 col-lg-6">
            <div class="card card-primary card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "attributes"/></h3>
              </div>
              <div class="card-body">
                <@userProfile profile=user/>
              </div>
              <#if user?? && user.realm == "obiba-realm">
                <div class="card-footer">
                  <div class="float-right">
                    <a href="${authConfig.userAccountUrl}" class="btn btn-primary" target="_blank"><i class="fas fa-pen"></i> <@message "personal-information"/></a>
                  </div>
                </div>
              </#if>
            </div>
          </div>
          <div class="col-sm-12 col-lg-6">
            <div class="card card-primary card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "credentials"/></h3>
              </div>
              <div class="card-body">
                <#if user.attributes?? && user.attributes["realm"]??>
                  <#if user.attributes["realm"] == "agate-user-realm">
                    <p><@message "credentials-info"/></p>
                    <#if user.otpEnabled?? && !user.otpEnabled>
                      <div class="alert alert-info">
                        <h6><i class="icon fas fa-lock"></i> <@message "security-info"/></h6>
                        <@message "2fa-info"/>
                      </div>
                    </#if>
                    <a href="${authConfig.userAccountUrl}" class="btn btn-primary" target="_blank"><i class="fas fa-pen"></i> <@message "user-account"/></a>
                  <#else>
                    <#assign isOidc = false/>
                    <#if oidcProviders??>
                      <#list oidcProviders as oidc>
                        <#if oidc.name == user.attributes["realm"]>
                          <#assign isOidc = true/>
                          <p><@message "credentials-info"/></p>
                          <@message "user-account-at"/>
                          <a href="${oidc.providerUrl}" class="btn btn-primary ml-2" target="_blank"><i class="fas fa-user"></i> ${oidc.title}</a>
                        </#if>
                      </#list>
                    </#if>
                    <#if !isOidc>
                      <@message "contact-system-administrator-to-change-password"/> [${user.attributes["realm"]}]
                    </#if>
                  </#if>
                <#else>
                  <@message "contact-system-administrator-to-change-password"/>
                </#if>
              </div>
            </div>
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

</body>
</html>
