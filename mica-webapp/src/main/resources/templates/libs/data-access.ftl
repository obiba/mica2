<!-- Function to get the color corresponding to a status -->
<#function statusColor status>
  <#if status == "OPENED">
    <#local txtColor = "primary"/>
  <#elseif status == "APPROVED">
    <#local txtColor = "success"/>
  <#elseif status == "REJECTED">
    <#local txtColor = "danger"/>
  <#elseif status == "SUBMITTED">
    <#local txtColor = "info"/>
  <#elseif status == "REVIEWED">
    <#local txtColor = "info"/>
  <#elseif status == "CONDITIONALLY_APPROVED">
    <#local txtColor = "warning"/>
  <#else>
    <#local txtColor = "info"/>
  </#if>
  <#return txtColor/>
</#function>

<!-- Current user privilegies -->
<#assign isAdministrator = user.roles?seq_contains("mica-administrator")/>
<#assign isDAO = user.roles?seq_contains("mica-data-access-officer")/>
