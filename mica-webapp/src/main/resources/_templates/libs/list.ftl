<#ftl output_format="HTML">
<!-- List page macros and utils -->

<!-- Function to normalize list name; the name can be stored HTML-encoded, the result is decoded: print it with listName(set), escaped when printed -->
<#function listName set>
  <#if set.name?starts_with("dar:")>
    <#local name = set.name?replace("dar:", "")/>
  <#else>
    <#local name = set.name/>
  </#if>
  <#return htmlDecode(name)/>
</#function>

<!-- Data Access Request creation -->
<#assign canCreateDAR = config?? && config.dataAccessEnabled && accessConfig?? && accessConfig.variablesEnabled/>
