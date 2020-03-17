<#include "navbar-menus.ftl">
<nav class="main-header navbar navbar-expand-md navbar-light navbar-white">
  <div class="container">
    <#if config??>
    <a href="${config.portalUrl!".."}" class="navbar-brand">
      <img src="${brandImageSrc}" alt="Logo" class="brand-image ${brandImageClass}"
           style="opacity: .8">
      <span class="brand-text ${brandTextClass}">${config.name!"Mica"}</span>
    </a>
    <#else>
      <img src="${brandImageSrc}" alt="Logo" class="brand-image ${brandImageClass}"
           style="opacity: .8">
      <span class="brand-text ${brandTextClass}"></span>
    </#if>

    <button class="navbar-toggler order-1" type="button" data-toggle="collapse" data-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse order-3" id="navbarCollapse">
      <!-- Left navbar links -->
      <ul class="navbar-nav">
          <@leftmenus></@leftmenus>
      </ul>

      <!-- Right navbar links -->
      <ul class="order-1 order-md-3 navbar-nav navbar-no-expand ml-auto">
        <@rightmenus></@rightmenus>
      </ul>
    </div>

  </div>
</nav>
