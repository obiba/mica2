<script>
  <#if !authenticated>
    MicaService.redirect('..');
  <#elseif postLogoutRedirectUri??>
    MicaService.redirect('${postLogoutRedirectUri}');
  <#else>
    UserService.signout('${postLogoutRedirectUri!".."}');
  </#if>
</script>
