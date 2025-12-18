<!-- Main Footer -->
<footer class="main-footer d-print-none">
  <!-- To the right -->
  <#if config??>
    <div class="float-right d-none d-sm-inline border-left ps-2 ms-2">
      <strong><@message "copyright"/> &copy; 2025 <a href="${portalLink}">${config.name!""}</a>.</strong> <@message "all-rights-reserved"/>
    </div>
  </#if>
  <a href="${contextPath}/administration" title="<@message "administration"/>" class="float-right"><i class="fa-solid fa-lock"></i></a>
  <!-- Default to the left -->
  <small><@message "powered-by"/> <a href="https://www.obiba.org">OBiBa Mica</a></small>
</footer>
