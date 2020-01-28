<nav class="main-header navbar navbar-expand-md navbar-light navbar-white">
  <div class="container">
    <a href="../bower_components/admin-lte/index3.html" class="navbar-brand">
      <img src="../bower_components/admin-lte/dist/img/AdminLTELogo.png" alt="AdminLTE Logo" class="brand-image img-circle elevation-3"
           style="opacity: .8">
      <span class="brand-text font-weight-light">Example</span>
    </a>

    <button class="navbar-toggler order-1" type="button" data-toggle="collapse" data-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse order-3" id="navbarCollapse">
      <!-- Left navbar links -->
      <ul class="navbar-nav">
        <li class="nav-item">
          <a href="../home" class="nav-link">Home</a>
        </li>
        <li class="nav-item">
          <a href="../dashboard" class="nav-link">Dashboard</a>
        </li>
        <li class="nav-item">
          <a href="../networks" class="nav-link">Networks</a>
        </li>
        <li class="nav-item">
          <a href="../studies" class="nav-link">Studies</a>
        </li>
        <li class="nav-item">
          <a href="../datasets" class="nav-link">Datasets</a>
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
      </ul>
    </div>

    <!-- Right navbar links -->
    <ul class="order-1 order-md-3 navbar-nav navbar-no-expand ml-auto">
      <#--<li class="nav-item">
        <a href="../cart" class="nav-link">
          <i class="fas fa-shopping-basket"></i>
          <span class="badge badge-danger navbar-badge">3</span>
        </a>
      </li>-->
      <#if user??>
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
    </ul>
  </div>
</nav>
