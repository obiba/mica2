<#include "navbar-menus.ftl">
<nav class="main-header navbar navbar-expand navbar-white navbar-light">
  <!-- Left navbar links -->
  <ul class="navbar-nav">
    <li class="nav-item">
      <a class="nav-link" data-widget="pushmenu" href="#"><i class="fas fa-bars"></i></a>
    </li>
    <@leftmenus></@leftmenus>
  </ul>


  <!-- Right navbar links -->
  <ul class="navbar-nav ml-auto">
      <@rightmenus></@rightmenus>
  </ul>
</nav>
