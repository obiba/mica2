<!-- Spring utils for translations -->
<#import "/spring.ftl" as spring/>
<#macro message code>
  <@spring.messageText code code/>
</#macro>
<#macro messageArgs code args>
  <@spring.messageArgsText code args code/>
</#macro>

<!-- Context path setting -->
<#assign contextPath = "${config.contextPath}"/>

<!-- From a localized text get the value in the current language or in the default one or in the undefined one -->
<#function localize txt={} default="">
  <#if txt?? && txt?keys??>
    <#if txt[.lang]??>
      <!-- page language -->
      <#return txt[.lang]/>
    <#elseif txt[defaultLang]??>
      <!-- default language setting -->
      <#return txt[defaultLang]/>
    <#elseif txt["und"]??>
      <!-- undefined lang, can happen in variables -->
      <#return txt["und"]/>
    <#elseif txt?values[0]??>
      <!-- first non null value -->
      <#return txt?values[0]/>
    </#if>
  </#if>
  <#return default/>
</#function>

<!-- Current user built-in roles -->
<#assign isAdministrator = (user?? && user.roles?? && user.roles?seq_contains("mica-administrator"))/>
<#assign isReviewer = (user?? && user.roles?? && user.roles?seq_contains("mica-reviewer"))/>
<#assign isEditor = (user?? && user.roles?? && user.roles?seq_contains("mica-editor"))/>
<#assign isDAO = (user?? && user.roles?? && user.roles?seq_contains("mica-data-access-officer"))/>

<!-- App settings -->
<#include "settings.ftl"/>
<#include "../models/settings.ftl"/>

<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="x-ua-compatible" content="ie=edge">

<!-- Favicon -->
<link rel="shortcut icon" href="${faviconPath}" />

<!-- Font Awesome Icons -->
<link rel="stylesheet" href="${adminLTEPath}/plugins/fontawesome-free/css/all.min.css">
<!-- Theme style -->
<link rel="stylesheet" href="${adminLTEPath}/dist/css/adminlte.min.css">
<!-- Google Font: Source Sans Pro -->
<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,400i,700" rel="stylesheet">
<!-- DataTables -->
<link rel="stylesheet" href="${adminLTEPath}/plugins/datatables-bs4/css/dataTables.bootstrap4.css">
<!-- Toastr -->
<link rel="stylesheet" href="${adminLTEPath}/plugins/toastr/toastr.min.css">
<!-- Bootstrap 3 to 4 -->
<link rel="stylesheet" href="${assetsPath}/css/bootstrap-3-4.css">
<!-- Obiba style -->
<link rel="stylesheet" href="${assetsPath}/css/obiba.css">
<!-- Select2 -->
<link rel="stylesheet" href="${adminLTEPath}/plugins/select2/css/select2.css">
<link rel="stylesheet" href="${adminLTEPath}/plugins/select2-bootstrap4-theme/select2-bootstrap4.css">

<!-- Custom head -->
<#include "../models/head.ftl"/>
