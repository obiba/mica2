<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<script>
  <#if !authenticated>
    MicaService.redirect('${contextPath}');
  <#elseif postLogoutRedirectUri??>
    MicaService.redirect('${postLogoutRedirectUri}');
  <#else>
    UserService.signout('${postLogoutRedirectUri!"${contextPath}"}');
  </#if>
</script>
</#outputformat>
