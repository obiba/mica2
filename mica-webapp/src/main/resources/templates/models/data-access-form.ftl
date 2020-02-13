<!-- Data access (amendment) form page macros -->

<!-- Help section, can be adapted to data access request status -->
<#macro dataAccessFormHelp dar>
  <#if dar.status == "OPENED">
    <@message "data-access-form-opened-help"/>
  <#elseif dar.status == "CONDITIONALLY_APPROVED">
    <@message "data-access-form-conditionally-approved-help"/>
  <#elseif dar.status == "SUBMITTED">
    <@message "data-access-form-submitted-help"/>
  <#elseif dar.status == "REVIEWED">
    <@message "data-access-form-reviewed-help"/>
  <#elseif dar.status == "APPROVED">
    <@message "data-access-form-approved-help"/>
  <#elseif dar.status == "REJECTED">
    <@message "data-access-form-rejected-help"/>
  </#if>
</#macro>

<!-- Help section, can be adapted to data access request status -->
<#macro dataAccessAmendmentFormHelp amendment>
    <#if amendment.status == "OPENED">
        <@message "data-access-amendment-form-opened-help"/>
    <#elseif amendment.status == "CONDITIONALLY_APPROVED">
        <@message "data-access-amendment-form-conditionally-approved-help"/>
    <#elseif amendment.status == "SUBMITTED">
        <@message "data-access-amendment-form-submitted-help"/>
    <#elseif amendment.status == "REVIEWED">
        <@message "data-access-amendment-form-reviewed-help"/>
    <#elseif amendment.status == "APPROVED">
        <@message "data-access-amendment-form-approved-help"/>
    <#elseif amendment.status == "REJECTED">
        <@message "data-access-amendment-form-rejected-help"/>
    </#if>
</#macro>
