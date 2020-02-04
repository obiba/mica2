<div class="user-panel mt-3 pb-3 mb-3 d-flex">
  <div class="info">
    <a href="#" class="d-block">${dar.applicant} - <@message dar.status.toString()/></a>
  </div>
</div>

<nav class="mt-2">
  <ul data-widget="treeview" role="menu" data-accordion="false" class="nav nav-pills nav-sidebar flex-column">
    <li class="nav-item">
      <a id="dashboard-menu" href="../data-access/${dar.id}" class="nav-link">
        <i class="fas fa-tachometer-alt nav-icon"></i>
        <p>Dashboard</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="feasibility-menu" href="../data-access-feasibility/${dar.id}" class="nav-link">
        <i class="far fa-question-circle nav-icon"></i>
        <p>Feasibility Inquiry Form</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="form-menu" href="../data-access-form/${dar.id}" class="nav-link">
        <i class="fas fa-book nav-icon"></i>
        <p>Form</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="documents-menu" href="../data-access-documents/${dar.id}" class="nav-link">
        <i class="fas fa-copy nav-icon"></i>
        <p>Documents</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="amendments-menu" href="../data-access-amendments/${dar.id}" href="#" class="nav-link">
        <i class="far fa-plus-square nav-icon"></i>
        <p>Amendments</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="comments-menu" href="../data-access-comments/${dar.id}" class="nav-link">
        <i class="fas fa-envelope nav-icon"></i>
        <p>Comments</p>
      </a>
    </li>
    <li class="nav-item">
      <a id="history-menu" href="../data-access-history/${dar.id}" class="nav-link">
        <i class="fas fa-calendar-alt nav-icon"></i>
        <p>History</p>
      </a>
    </li>
  </ul>
</nav>
