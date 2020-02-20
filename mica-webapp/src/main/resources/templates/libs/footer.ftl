<!-- Main Footer -->
<footer class="main-footer d-print-none">
  <!-- To the right -->
  <#if config??>
    <div class="float-right d-none d-sm-inline">
      <strong><@message "copyright"/> &copy; 2020 <a href="${config.portalUrl!"#"}">${config.name!""}</a>.</strong> <@message "all-rights-reserved"/>
    </div>
  </#if>
  <a href="${pathPrefix!""}/admin" title="<@message "administration"/>" class="float-right border-right pr-2 mr-2"><i class="fas fa-lock"></i></a>
  <!-- Default to the left -->
  <@message "powered-by"/> <a href="https://www.obiba.org">OBiBa Mica</a>.
</footer>
