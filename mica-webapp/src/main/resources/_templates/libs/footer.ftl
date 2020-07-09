<!-- Main Footer -->
<footer class="main-footer d-print-none">
  <!-- To the right -->
  <#if config??>
    <div class="float-right d-none d-sm-inline border-left pl-2 ml-2">
      <strong><@message "copyright"/> &copy; 2020 <a href="${config.portalUrl!"#"}">${config.name!""}</a>.</strong> <@message "all-rights-reserved"/>
    </div>
  </#if>
  <a href="${contextPath}/admin" title="<@message "administration"/>" class="float-right"><i class="fas fa-lock"></i></a>
  <!-- Default to the left -->
  <small><@message "powered-by"/> <a href="https://www.obiba.org">OBiBa Mica</a></small>
</footer>
