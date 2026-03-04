<script>
  <#if !authenticated>
    MicaService.redirect('${contextPath}');
  <#elseif postLogoutRedirectUri??>
    MicaService.redirect('${postLogoutRedirectUri}');
  <#else>
    UserService.signout('${postLogoutRedirectUri!"${contextPath}"}');
  </#if>
</script>
