<!-- Main Footer -->
<footer class="main-footer d-print-none">
  <!-- To the right -->
  <#if config??>
    <div class="float-right d-none d-sm-inline">
      <strong>Copyright &copy; 2020 <a href="${config.portalUrl!"#"}">${config.name!""}</a>.</strong> All rights reserved.
    </div>
  </#if>
  <!-- Default to the left -->
  Powered by <a href="https://www.obiba.org">OBiBa Mica</a>.
</footer>
