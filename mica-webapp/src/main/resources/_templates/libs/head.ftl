<!-- Spring utils for translations -->
<#import "/spring.ftl" as spring/>
<#macro message code>
  <@spring.messageText code code/>
</#macro>

<!-- From a localized text get the value in the current language or in the default one or in the undefined one -->
<#function localize txt={} default="">
  <#if txt??>
    <#if txt[.lang]??>
      <#return txt[.lang]/>
    <#elseif txt["en"]??>
      <#return txt["en"]/>
    <#elseif txt["und"]??>
      <#return txt["und"]/>
    </#if>
  </#if>
  <#return default/>
</#function>

<!-- App settings -->
<#include "settings.ftl"/>
<#include "../models/settings.ftl"/>

<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="x-ua-compatible" content="ie=edge">

<!-- Favicon -->
<link rel="shortcut icon" href="${faviconPath}" />

<!-- Font Awesome Icons -->
<link rel="stylesheet" href="${pathPrefix!".."}${adminLTEPath}/plugins/fontawesome-free/css/all.min.css">
<!-- Theme style -->
<link rel="stylesheet" href="${pathPrefix!".."}${adminLTEPath}/dist/css/adminlte.min.css">
<!-- Google Font: Source Sans Pro -->
<link href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,400i,700" rel="stylesheet">
<!-- DataTables -->
<link rel="stylesheet" href="${pathPrefix!".."}${adminLTEPath}/plugins/datatables-bs4/css/dataTables.bootstrap4.css">
<!-- Toastr -->
<link rel="stylesheet" href="${pathPrefix!".."}${adminLTEPath}/plugins/toastr/toastr.min.css">
<!-- Bootstrap 3 to 4 -->
<link rel="stylesheet" href="${pathPrefix!".."}/assets/css/bootstrap-3-4.css">
<!-- Obiba style -->
<link rel="stylesheet" href="${pathPrefix!".."}/assets/css/obiba.css">

<!-- Current user privilegies -->
<#if user??>
  <#assign isAdministrator = user.roles?seq_contains("mica-administrator")/>
  <#assign isDAO = user.roles?seq_contains("mica-data-access-officer")/>
</#if>

<!-- Custom head -->
<#include "../models/head.ftl"/>
