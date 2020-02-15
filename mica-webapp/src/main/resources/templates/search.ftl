<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <title>${config.name!""} | Search</title>
  <#include "libs/head.ftl">
</head>
<body class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="../bower_components/admin-lte/index3.html" class="brand-link bg-white">
      <img src="../bower_components/admin-lte/dist/img/AdminLTELogo.png"
           alt="Logo"
           class="brand-image img-circle elevation-3"
           style="opacity: .8">
      <span class="brand-text font-weight-light">${config.name!""}</span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
      <!-- Sidebar user (optional) -->
      <div class="user-panel mt-3 pb-3 mb-3 d-flex">
        <div class="info">
          <a href="#" class="d-block">Search Criteria</a>
        </div>
      </div>

      <!-- Sidebar Menu -->
      <nav class="mt-2">

        <ul data-widget="treeview" role="menu" data-accordion="false" class="nav nav-pills nav-sidebar flex-column">

        </ul>

        <div id="search-criteria">
          <ul v-for="name in criteriaMenu.order"
              class="nav nav-pills nav-sidebar flex-column" data-widget="treeview"
              role="menu" data-accordion="false">
            <li class="nav-item has-treeview">
              <a href="#" class="nav-link">
                <i class="nav-icon" v-bind:class="criteriaMenu.items[name].icon"></i>
                <p>
                  {{criteriaMenu.items[name].title}}
                  <i class="right fas fa-angle-left"></i>
                </p>
              </a>
              <ul class="nav nav-treeview">
                <taxonomy-menu v-for="menu in criteriaMenu.items[name].menus"
                               v-bind:key="menu.name"
                               v-bind:taxonomy="menu"
                               v-on:taxonomy-selection="onTaxonomySelection($event)">
                </taxonomy-menu>
              </ul>
            </li>
          </ul>
        </div>
      </nav>
      <!-- /.sidebar-menu -->
    </div>
    <!-- /.sidebar -->
  </aside>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1>Search</h1>
          </div>
          <div class="col-sm-6">

          </div>
        </div>
      </div><!-- /.container-fluid -->
    </section>

    <!-- Main content -->
    <section class="content">

      <!-- Query box -->
      <div class="card card-info card-outline">
        <div class="card-header">
          <h3 class="card-title">Query</h3>

          <div class="card-tools">
            <button type="button" class="btn btn-tool" data-card-widget="collapse" data-toggle="tooltip"
                    title="Collapse">
              <i class="fas fa-minus"></i></button>
          </div>
        </div>
        <div class="card-body">
          <div id="query-builder">
            <div class="alert alert-info" v-if="message != ''">
              <i class="icon fas fa-info"></i>
              {{ message }}
            </div>
            <button class="btn btn-success" @click.prevent="onExecuteQuery()"><i class="fas fa-sync"></i> Refresh</button> <span class="badge badge-danger">{{ queryType }}</span>
          </div>
        </div>
        <!-- /.card-body -->
      </div>
      <!-- /.card -->

      <!-- Results box -->
      <div class="row">
        <div class="col-12">
          <!-- Custom Tabs -->
          <div class="card">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3">Results</h3>
              <ul class="nav nav-pills ml-auto p-2">
                <li class="nav-item"><a id="lists-tab" class="nav-link active" href="#tab_lists" data-toggle="tab">Lists</a></li>
                <li class="nav-item"><a id="coverage-tab" class="nav-link" href="#tab_coverage" data-toggle="tab">Coverage</a></li>
                <li class="nav-item"><a id="graphics-tab" class="nav-link" href="#tab_graphics" data-toggle="tab">Graphics</a></li>
              </ul>
            </div><!-- /.card-header -->
            <div class="card-body">
              <div class="tab-content">
                <div class="tab-pane active" id="tab_lists">
                  <p class="text-muted">
                    A wonderful serenity has taken possession of my entire soul,
                    like these sweet mornings of spring which I enjoy with my whole heart.
                    I am alone, and feel the charm of existence in this spot,
                    which was created for the bliss of souls like mine.
                  </p>

                  <div class="mt-3">
                    <ul class="nav nav-pills" id="results-tab" role="tablist">
                      <li class="nav-item">
                        <a class="nav-link active" id="variables-tab" data-toggle="pill" href="#variables" role="tab"
                           aria-controls="variables" aria-selected="true">Variables</a>
                      </li>
                      <li class="nav-item">
                        <a class="nav-link" id="datasets-tab" data-toggle="pill" href="#datasets" role="tab"
                           aria-controls="datasets" aria-selected="false">Datasets</a>
                      </li>
                      <li class="nav-item">
                        <a class="nav-link" id="studies-tab" data-toggle="pill" href="#studies" role="tab"
                           aria-controls="studies" aria-selected="false">Studies</a>
                      </li>
                      <li class="nav-item">
                        <a class="nav-link" id="networks-tab" data-toggle="pill" href="#networks" role="tab"
                           aria-controls="networks" aria-selected="false">Networks</a>
                      </li>
                    </ul>
                  </div>

                  <div class="mt-3">
                    <div class="tab-content" id="results-tabContent">
                      <div class="tab-pane fade show active" id="variables" role="tabpanel"
                           aria-labelledby="variables-tab">
                        List of variables
                        <div id="list-variables">
                          {{ result }}
			  <variables-result></variables-result>
                        </div>
                      </div>
                      <div class="tab-pane fade" id="datasets" role="tabpanel" aria-labelledby="datasets-tab">
                        List of datasets
                        <div id="list-datasets">
                          {{ result }}
                        </div>
                      </div>
                      <div class="tab-pane fade" id="studies" role="tabpanel" aria-labelledby="studies-tab">
                        List of studies
                        <div id="list-studies">
                          {{ result }}
                        </div>
                      </div>
                      <div class="tab-pane fade" id="networks" role="tabpanel" aria-labelledby="networks-tab">
                        List of networks
                        <div id="list-networks">
                          {{ result }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- /.tab-pane -->
                <div class="tab-pane" id="tab_coverage">
                  <p class="text-muted">
                    The European languages are members of the same family. Their separate existence is a myth.
                    For science, music, sport, etc, Europe uses the same vocabulary. The languages only differ
                    in their grammar, their pronunciation and their most common words.
                  </p>
                  <div id="coverage">
                    {{ result }}
                  </div>
                </div>
                <!-- /.tab-pane -->
                <div class="tab-pane" id="tab_graphics">
                  <p class="text-muted">
                    Lorem Ipsum is simply dummy text of the printing and typesetting industry.
                    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
                    when an unknown printer took a galley of type and scrambled it to make a type specimen book.
                  </p>
                  <div id="graphics">
                    {{ result }}
                  </div>
                </div>
                <!-- /.tab-pane -->
              </div>
              <!-- /.tab-content -->
            </div><!-- /.card-body -->
          </div>
          <!-- ./card -->
        </div>
        <!-- /.col -->
      </div>

      <div class="row">
        <div class="col-12">
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/search-scripts.ftl">

</body>
</html>
