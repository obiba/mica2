<#macro leftmenus>
  <li class="nav-item">
    <a href="../home" class="nav-link">Home</a>
  </li>
  <li class="nav-item dropdown">
    <a id="repoMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle">Repository</a>
    <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
      <li><a href="../networks" class="dropdown-item">Networks</a></li>
      <li><a href="../studies" class="dropdown-item">Studies</a></li>
      <li><a href="../datasets" class="dropdown-item">Datasets</a></li>
    </ul>
  </li>
  <li class="nav-item">
    <a href="../search" class="btn btn-success">Search <i class="fas fa-search"></i></a>
  </li>
  <li class="nav-item dropdown">
    <a id="studiesMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle">Research</a>
    <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
      <li><a href="../data-access-process" class="dropdown-item">Data Access Process</a></li>
      <li><a href="../projects" class="dropdown-item">Approved Projects</a></li>
    </ul>
  </li>
  <li class="nav-item">
    <a href="../data-accesses" class="btn btn-warning">Data Access <i class="fas fa-arrow-circle-right"></i></a>
  </li>
</#macro>

<#macro rightmenus>
    <#if user??>
      <li class="nav-item">
        <a href="../cart" class="nav-link">
          <i class="fas fa-shopping-basket"></i>
          <!--<span class="badge badge-danger navbar-badge">3</span>-->
        </a>
      </li>
      <li class="nav-item dropdown">
        <a id="userMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><i class="fas fa-user"></i> ${user.fullName}</a>
        <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
          <li><a href="../profile" class="dropdown-item">Profile</a></li>
          <li><a id="signout" href="#" class="dropdown-item">Sign out</a></li>
        </ul>
      </li>
    <#else >
      <li class="nav-item">
        <a class="nav-link" href="../signin">Sign in</a>
      </li>
      <li class="nav-item">
        <a class="btn btn-outline-primary" href="../signup">Sign up</a>
      </li>
    </#if>
</#macro>
