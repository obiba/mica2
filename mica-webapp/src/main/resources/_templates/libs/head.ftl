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


<#function arrayNotEmpty array=[]>
    <#assign notEmpty = true>
    <#if array?? && array?size gt 0>
        <#list array as element>
            <#assign notEmpty = notEmpty && element?? && element?has_content>
        </#list>
    <#else>
        <#return false>
    </#if>
    <#return notEmpty>
</#function>

<#function localizedStringNotEmpty txt={}>
    <#assign notEmpty = true>
    <#if txt?? && txt?keys??>
        <#assign notEmpty = txt[.lang]?? && txt[.lang]?has_content && txt[.lang]?trim?has_content>
    <#else>
        <#return false>
    </#if>
    <#return notEmpty>
</#function>

<#function arrayLocalizedStringNotEmpty array=[]>
    <#assign notEmpty = true>
    <#if array?? && array?size gt 0>
        <#list array as element>
            <#assign notEmpty = notEmpty && localizedStringNotEmpty(element)>
        </#list>
    <#else>
        <#return false>
    </#if>
    <#return notEmpty>
</#function>

<!-- Current user built-in roles -->
<#assign isAdministrator = (user?? && user.roles?? && user.roles?seq_contains("mica-administrator"))/>
<#assign isReviewer = (user?? && user.roles?? && user.roles?seq_contains("mica-reviewer"))/>
<#assign isEditor = (user?? && user.roles?? && user.roles?seq_contains("mica-editor"))/>
<#assign isDAO = (user?? && user.roles?? && user.roles?seq_contains("mica-data-access-officer"))/>

<!-- Current user can view at least one draft document -->
<#assign hasPermissionOnAnyDraftDocument = (user?? && user.hasPermissionOnAnyDraftDocument)/>

<!-- App settings -->
<#include "settings.ftl"/>
<#include "../models/settings.ftl"/>

<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="x-ua-compatible" content="ie=edge">

<!-- Favicon -->
<link rel="shortcut icon" href="${faviconPath}" />

<!-- Font Awesome Icons -->
<link rel="stylesheet" href="${fontAwesomePath}/css/all.min.css">
<!-- Mica Theme (includes Bootstrap 5 + AdminLTE 4 + custom overrides) -->
<!-- Font: Source Sans Pro -->
<style type="text/css">
  @font-face {
    font-family: 'Source Sans Pro';
    font-style: normal;
    font-weight: 300;
    src: url("${assetsPath}/fonts/Source_Sans_Pro/SourceSansPro-Light.ttf") format("truetype"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-light-webfont.woff2") format("woff2"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-light-webfont.woff") format("woff");
  }
  @font-face {
    font-family: 'Source Sans Pro';
    font-style: normal;
    font-weight: 400;
    src: url("${assetsPath}/fonts/Source_Sans_Pro/SourceSansPro-Regular.ttf") format("truetype"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-regular-webfont.woff2") format("woff2"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-regular-webfont.woff") format("woff");
  }
  @font-face {
    font-family: 'Source Sans Pro';
    font-style: normal;
    font-weight: 700;
    src: url("${assetsPath}/fonts/Source_Sans_Pro/SourceSansPro-Bold.ttf") format("truetype"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-bold-webfont.woff2") format("woff2"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-bold-webfont.woff") format("woff");
  }
  @font-face {
    font-family: 'Source Sans Pro';
    font-style: italic;
    font-weight: 400;
    src: url("${assetsPath}/fonts/Source_Sans_Pro/SourceSansPro-Italic.ttf") format("truetype"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-italic-webfont.woff2") format("woff2"),
    url("${assetsPath}/fonts/Source_Sans_Pro/sourcesanspro-italic-webfont.woff") format("woff");
  }
</style>
<!-- DataTables -->
<link rel="stylesheet" href="${datatablesBS5Path}/css/dataTables.bootstrap5.min.css">
<link rel="stylesheet" href="${datatablesFixedHeaderBS5Path}/css/fixedHeader.bootstrap5.min.css">
<!-- Toastr -->
<link rel="stylesheet" href="${toastrPath}/build/toastr.min.css">
<!-- Bootstrap 3 to 4 -->
<link rel="stylesheet" href="${assetsPath}/css/bootstrap-3-4.css">
<!-- Obiba style -->
<link rel="stylesheet" href="${assetsPath}/css/obiba.css">
<!-- Select2 -->
<link rel="stylesheet" href="${select2Path}/dist/css/select2.min.css">
<link rel="stylesheet" href="${select2BS5ThemePath}/dist/select2-bootstrap-5-theme.min.css">
<link rel="stylesheet" href="${assetsPath}/css/mica.min.css">

<!-- Custom head -->
<#include "../models/head.ftl"/>
