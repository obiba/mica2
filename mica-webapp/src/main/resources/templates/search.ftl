<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "search"/></title>
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
    <a href="${config.portalUrl!".."}" class="brand-link bg-white">
      <img src="../assets/images/logo.png"
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
          <a href="#" class="d-block"><@message "search-criteria"/></a>
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
            <h1><@message "search"/></h1>
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

            <div class="modal fade" id="taxonomy-modal">
              <div class="modal-dialog modal-xl" role="document">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title">{{ selectedTaxonomy ? selectedTaxonomy.title[0].text : "" }}</h5>
                    <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span></button>
                  </div>
                  <div class="modal-body">
                    <rql-panel v-bind:taxonomy="selectedTaxonomy" v-bind:query="queryForSelectedTaxonomy"></rql-panel>
                  </div>
                </div>
              </div>
            </div>
            <!-- /.modal -->

            <button class="btn btn-success" @click.prevent="onExecuteQuery()"><i class="fas fa-sync"></i> <@message "refresh"/></button>
            <span class="badge badge-danger">{{ queryType }}</span>

            <rql-query-builder v-for="(query, target) in queries" v-bind:target="target" v-bind:taxonomy="getTaxonomyForTarget(target)" v-bind:query="query"></rql-query-builder>
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
              <h3 class="card-title p-3"><@message "results"/></h3>
              <ul class="nav nav-pills ml-auto p-2">
                <li class="nav-item"><a id="lists-tab" class="nav-link active" href="#tab_lists" data-toggle="tab"><@message "lists"/></a></li>
                <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                  <li class="nav-item"><a id="coverage-tab" class="nav-link" href="#tab_coverage" data-toggle="tab"><@message "coverage"/></a></li>
                </#if>
                <#if config.networkEnabled && !config.singleStudyEnabled>
                  <li class="nav-item"><a id="graphics-tab" class="nav-link" href="#tab_graphics" data-toggle="tab"><@message "graphics"/></a></li>
                </#if>
              </ul>
            </div><!-- /.card-header -->
            <div class="card-body">
              <div class="tab-content">
                <div class="tab-pane active" id="tab_lists">
                  <p class="text-muted">
                    <@message "results-lists-text"/>
                  </p>

                  <div class="mt-3">
                    <ul class="nav nav-pills" id="results-tab" role="tablist">
                      <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                        <li class="nav-item">
                          <a class="nav-link active" id="variables-tab" data-toggle="pill" href="#variables" role="tab"
                             aria-controls="variables" aria-selected="true"><@message "variables"/></a>
                        </li>
                        <li class="nav-item">
                          <a class="nav-link" id="datasets-tab" data-toggle="pill" href="#datasets" role="tab"
                             aria-controls="datasets" aria-selected="false"><@message "datasets"/></a>
                        </li>
                      </#if>
                      <#if !config.singleStudyEnabled>
                        <li class="nav-item">
                          <a class="nav-link" id="studies-tab" data-toggle="pill" href="#studies" role="tab"
                             aria-controls="studies" aria-selected="false"><@message "studies"/></a>
                        </li>
                      </#if>
                      <#if config.networkEnabled && !config.singleNetworkEnabled>
                        <li class="nav-item">
                          <a class="nav-link" id="networks-tab" data-toggle="pill" href="#networks" role="tab"
                             aria-controls="networks" aria-selected="false"><@message "networks"/></a>
                        </li>
                      </#if>
                    </ul>
                  </div>

                  <div class="mt-3">
                    <div class="tab-content" id="results-tabContent">
                      <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                        <div class="tab-pane fade show active" id="variables" role="tabpanel"
                             aria-labelledby="variables-tab">
                          <p class="text-muted"><@message "results-list-of-variables-text"/></p>
                          <div id="list-variables">
                            {{ result }}
                            <variables-result></variables-result>
                          </div>
                        </div>
                        <div class="tab-pane fade" id="datasets" role="tabpanel" aria-labelledby="datasets-tab">
                          <p class="text-muted"><@message "results-list-of-datasets-text"/></p>
                          <div id="list-datasets">
                            {{ result }}
                            <datasets-result></datasets-result>
                          </div>
                        </div>
                      </#if>
                      <#if !config.singleStudyEnabled>
                        <div class="tab-pane fade" id="studies" role="tabpanel" aria-labelledby="studies-tab">
                          <p class="text-muted"><@message "results-list-of-studies-text"/></p>
                          <div id="list-studies">
                            {{ result }}
                            <studies-result></studies-result>
                          </div>
                        </div>
                      </#if>
                      <#if config.networkEnabled && !config.singleNetworkEnabled>
                        <div class="tab-pane fade" id="networks" role="tabpanel" aria-labelledby="networks-tab">
                          <p class="text-muted"><@message "results-list-of-networks-text"/></p>
                          <div id="list-networks">
                            {{ result }}
                            <networks-result></networks-result>
                          </div>
                        </div>
                      </#if>
                    </div>
                  </div>
                </div>
                <!-- /.tab-pane -->

                <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
                  <div class="tab-pane" id="tab_coverage">
                    <p class="text-muted">
                      <@message "results-coverage-text"/>
                    </p>
                    <div id="coverage">
                      {{ result }}
                    </div>
                  </div>
                </#if>
                <!-- /.tab-pane -->

                <#if config.networkEnabled && !config.singleStudyEnabled>
                  <div class="tab-pane" id="tab_graphics">
                    <p class="text-muted">
                      <@message "results-graphics-text"/>
                    </p>
                    <div id="graphics">
                      {{ result }}
                    </div>
                  </div>
                </#if>
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
