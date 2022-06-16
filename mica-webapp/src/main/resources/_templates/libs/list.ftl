<!-- List page macros and utils -->

<!-- Function to normalize list name -->
<#function listName set>
  <#if set.name?starts_with("dar:")>
    <#local name = set.name?replace("dar:", "")/>
  <#else>
    <#local name = set.name/>
  </#if>
  <#return name/>
</#function>

<!-- Data Access Request creation -->
<#assign canCreateDAR = config?? && config.dataAccessEnabled && accessConfig?? && accessConfig.variablesEnabled/>
