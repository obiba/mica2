<!doctype html>
<!--
  ~ Copyright (c) 2018 OBiBa. All rights reserved.
  ~
  ~ This program and the accompanying materials
  ~ are made available under the terms of the GNU Public License v3.0.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<!--[if lt IE 7]>
<html class="no-js lt-ie9 lt-ie8 lt-ie7">
<![endif]-->
<!--[if IE 7]>
<html class="no-js lt-ie9 lt-ie8">
<![endif]-->
<!--[if IE 8]>
<html class="no-js lt-ie9">
<![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js">
<!--<![endif]-->
<head>
  <!-- Context path setting -->
  <#assign contextPath = "${config.contextPath}"/>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title></title>
  <meta name="description" content="">
  <meta name="viewport" content="width=device-width">
  <!-- Place favicon.ico and apple-touch-icon.png in the root directory -->
  <!-- build:css styles/main.css -->
  <link rel="stylesheet" href="${contextPath}/styles/famfamfam-flags.css">
  <link rel="stylesheet" href="${contextPath}/bower_components/font-awesome/css/font-awesome.css" />
  <!-- bower:css -->
  <link rel="stylesheet" href="${contextPath}/bower_components/angular-loading-bar/build/loading-bar.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/nvd3/build/nv.d3.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/angular-ui-select/dist/select.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/simplemde/dist/simplemde.min.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/mica-study-timeline/dist/mica-study-timeline.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/ng-obiba/dist/css/ng-obiba.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/ng-obiba-mica/dist/css/ng-obiba-mica.css" />
  <link rel="stylesheet" href="${contextPath}/bower_components/angular-xeditable/dist/css/xeditable.css" />
  <!-- endbower -->
  <!-- do not change location due to fonts dependency on ng-obiba-mica -->
  <link rel="stylesheet" href="${contextPath}/styles/mica.css">
  <!-- endbuild -->
  <link rel="stylesheet" href="${contextPath}/ws/config/style.css">
</head>
<body id="admin-page" ng-app="mica" ng-strict-di="true" ng-controller="MainController" ng-cloak>
<!--[if lt IE 10]>
<p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://browsehappy.com/">upgrade
  your browser</a> to improve your experience.</p>
<![endif]-->

<div class="navbar navbar-default navbar-fixed-top" role="navigation">
  <div class="container">
    <div class="navbar-header">
      <a href="${contextPath}/" class="navbar-brand">{{micaConfig.name}}</a>
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
    </div>
    <div class="navbar-collapse collapse" id="navbar-main" ng-switch="authenticated">
      <ul class="nav navbar-nav">
        <li ng-if="authenticated && micaConfig.isRepositoryEnabled && micaConfig.isNetworkEnabled">
          <a href="#/network">
            <span>{{micaConfig.isSingleNetworkEnabled ? 'network.label' : 'networks' | translate}}</span>
          </a>
        </li>
        <li class="dropdown clearfix" ng-if="authenticated && micaConfig.isRepositoryEnabled && micaConfig.isHarmonizedDatasetEnabled">
          <a href="" class="dropdown-toggle" data-toggle="dropdown">
            <span translate>studies</span>
            <i class="fa fa-caret-down"></i></a>
          <ul class="dropdown-menu pull-left">
            <li><a href="#/individual-study">{{micaConfig.isSingleStudyEnabled ? 'global.individual-study' : 'global.individual-studies' | translate}}</a></li>
            <li><a href="#/harmonization-study">{{'global.harmonization-studies' | translate}}</a></li>
          </ul>
        </li>
        <li ng-if="authenticated && micaConfig.isRepositoryEnabled && !micaConfig.isHarmonizedDatasetEnabled">
          <a href="#/individual-study">{{micaConfig.isSingleStudyEnabled ? 'global.individual-study' : 'global.individual-studies' | translate}}</a>
        </li>
        <li class="dropdown clearfix" ng-if="authenticated && micaConfig.isRepositoryEnabled && micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled">
          <a href="" class="dropdown-toggle" data-toggle="dropdown">
            <span translate>datasets</span>
            <i class="fa fa-caret-down"></i></a>
          <ul class="dropdown-menu pull-left">
            <li ng-if="micaConfig.isCollectedDatasetEnabled"><a href="#/collected-dataset"><span translate>collected-datasets</span></a></li>
            <li ng-if="micaConfig.isHarmonizedDatasetEnabled"><a href="#/harmonized-dataset"><span translate>harmonized-datasets</span></a></li>
          </ul>
        </li>
        <li ng-if="authenticated && micaConfig.isRepositoryEnabled && micaConfig.isCollectedDatasetEnabled && !micaConfig.isHarmonizedDatasetEnabled">
          <a href="#/collected-dataset">
            <span>{{'collected-datasets' | translate}}</span>
          </a>
        </li>
        <li ng-if="authenticated && micaConfig.isRepositoryEnabled && !micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled">
          <a href="#/harmonized-dataset">
            <span>{{'harmonized-datasets' | translate}}</span>
          </a>
        </li>
        <li class="dropdown clearfix" ng-if="authenticated && (micaConfig.isDataAccessEnabled || micaConfig.isProjectEnabled)">
          <a href="" class="dropdown-toggle" data-toggle="dropdown">
            <span translate>research</span>
            <i class="fa fa-caret-down"></i></a>
          <ul class="dropdown-menu pull-left">
            <li ng-if="micaConfig.isProjectEnabled">
              <a href="#/project">
                <span translate>research-projects</span>
              </a>
            </li>
            <li ng-if="micaConfig.isDataAccessEnabled && hasRole(['mica-administrator', 'mica-data-access-officer', 'mica-user'])">
              <a href="#/data-access-requests">
                <span translate>data-access-requests</span>
              </a>
            </li>
          </ul>
        </li>
        <li ng-if="authenticated">
          <a href="#/files"><span>{{'files' | translate}}</span></a>
        </li>
        <li ng-if="authenticated && hasRole(['mica-administrator'])">
          <a href="#/persons"><span>{{'persons.title' | translate}}</span></a>
        </li>
      </ul>

      <ul class="nav navbar-nav navbar-right" ng-hide="!authenticated">
        <li ng-if="hasRole(['mica-administrator'])">
          <a href="#/admin">
            <span translate>global.menu.admin</span>
          </a>
        </li>
        <li>
          <a href="http://micadoc.obiba.org" target="_blank">
            <span translate>help</span>
          </a>
        </li>
        <li class="dropdown">
          <a href="" class="dropdown-toggle" data-toggle="dropdown">
            <i class="fa fa-user"></i>
            {{UserProfileService.getFullName(subject.profile) || subject.login}}
            <i class="fa fa-caret-down"></i></a>
          <ul class="dropdown-menu pull-right" ng-controller="LanguageController">
            <!--<li><a href="#/profile"><i class="fa fa-cog"></i> <span translate>global.menu.myProfile</span></a></li>-->
            <li ng-repeat="lang in languages">
              <a href ng-click="changeLanguage(lang)">
                <span>{{'language.' + lang | translate}}</span> <i class="fa fa-check" aria-hidden="true" ng-show="getCurrentLanguage() === lang"></i></a>
            </li>
            <li class="divider"></li>
            <li><a href="#/logout"><i class="fa fa-sign-out"></i> <span translate>global.menu.logout</span></a></li>
          </ul>
        </li>
      </ul>

    </div>
  </div>
</div>

<div class="container">
  <div class="page-header" ng-switch="authenticated">
  </div>

  <div class="container alert-fixed-position">
    <obiba-alert id="MainController"></obiba-alert>
  </div>

  <div class="alert-growl-container">
    <obiba-alert id="MainControllerGrowl"></obiba-alert>
  </div>

  <div ng-controller="NotificationController"></div>

  <div ng-view=""></div>

  <footer class="hidden-print" ng-hide="!authenticated">
    <div class="row">
      <div class="col-lg-12">
        <ul class="list-unstyled list-inline">
          <li class="pull-right">{{micaConfig.version}}</li>
          <li>© 2021</li>
          <li><a href="http://obiba.org" target="_blank"> OBiBa </a></li>
          <li><a href="http://micadoc.obiba.org" target="_blank" translate>
              global.documentation</a></li>
          <li><a href="https://github.com/obiba/mica2" target="_blank" translate>global.sources
            </a></li>
        </ul>
      </div>
    </div>
  </footer>
</div>

<!-- Global js variables -->
<script>
  const contextPath = '${contextPath}';
</script>

<!-- build:js scripts/scripts.js -->
<!-- bower:js -->
<script src="${contextPath}/bower_components/modernizr/modernizr.js"></script>
<script src="${contextPath}/bower_components/jquery/dist/jquery.js"></script>
<script src="${contextPath}/bower_components/angular/angular.js"></script>
<script src="${contextPath}/bower_components/angular-animate/angular-animate.js"></script>
<script src="${contextPath}/bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>
<script src="${contextPath}/bower_components/chosen/chosen.jquery.js"></script>
<script src="${contextPath}/bower_components/angular-chosen-localytics/dist/angular-chosen.js"></script>
<script src="${contextPath}/bower_components/angular-cookies/angular-cookies.js"></script>
<script src="${contextPath}/bower_components/angular-dynamic-locale/src/tmhDynamicLocale.js"></script>
<script src="${contextPath}/bower_components/angular-loading-bar/build/loading-bar.js"></script>
<script src="${contextPath}/bower_components/d3/d3.js"></script>
<script src="${contextPath}/bower_components/nvd3/build/nv.d3.js"></script>
<script src="${contextPath}/bower_components/angular-nvd3/dist/angular-nvd3.js"></script>
<script src="${contextPath}/bower_components/angular-resource/angular-resource.js"></script>
<script src="${contextPath}/bower_components/angular-route/angular-route.js"></script>
<script src="${contextPath}/bower_components/angular-sanitize/angular-sanitize.js"></script>
<script src="${contextPath}/bower_components/angular-translate/angular-translate.js"></script>
<script src="${contextPath}/bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.js"></script>
<script src="${contextPath}/bower_components/angular-translate-storage-cookie/angular-translate-storage-cookie.js"></script>
<script src="${contextPath}/bower_components/angular-ui-select/dist/select.js"></script>
<script src="${contextPath}/bower_components/jquery-ui/jquery-ui.js"></script>
<script src="${contextPath}/bower_components/angular-ui-sortable/sortable.js"></script>
<script src="${contextPath}/bower_components/bootstrap/dist/js/bootstrap.min.js"></script>
<script src="${contextPath}/bower_components/json3/lib/json3.js"></script>
<script src="${contextPath}/bower_components/mica-study-timeline/dist/mica-study-timeline.js"></script>
<script src="${contextPath}/bower_components/ng-file-upload/ng-file-upload.js"></script>
<script src="${contextPath}/bower_components/marked/lib/marked.js"></script>
<script src="${contextPath}/bower_components/simplemde/dist/simplemde.min.js"></script>
<script src="${contextPath}/bower_components/angular-marked/dist/angular-marked.js"></script>
<script src="${contextPath}/bower_components/moment/moment.js"></script>
<script src="${contextPath}/bower_components/moment/min/locales.min.js"></script>
<script src="${contextPath}/bower_components/angular-moment/angular-moment.js"></script>
<script src="${contextPath}/bower_components/ng-obiba/dist/ng-obiba.js"></script>
<script src="${contextPath}/bower_components/clipboard/dist/clipboard.js"></script>
<script src="${contextPath}/bower_components/ngclipboard/dist/ngclipboard.js"></script>
<script src="${contextPath}/bower_components/filesize/lib/filesize.min.js"></script>
<script src="${contextPath}/bower_components/rql-nodojo/js/rql.js"></script>
<script src="${contextPath}/bower_components/angular-local-storage/dist/angular-local-storage.js"></script>
<script src="${contextPath}/bower_components/ng-obiba-mica/dist/ng-obiba-mica.js"></script>
<script src="${contextPath}/bower_components/angular-utils-pagination/dirPagination.js"></script>
<script src="${contextPath}/bower_components/tv4/tv4.js"></script>
<script src="${contextPath}/bower_components/objectpath/lib/ObjectPath.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form/dist/schema-form.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form/dist/bootstrap-decorator.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-bootstrap/bootstrap-decorator.min.js"></script>
<script src="${contextPath}/bower_components/ace-builds/src-noconflict/ace.js"></script>
<script src="${contextPath}/bower_components/angular-ui-ace/ui-ace.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-ui-ace/bootstrap-ui-ace.min.js"></script>
<script src="${contextPath}/bower_components/angular-strap/dist/angular-strap.js"></script>
<script src="${contextPath}/bower_components/angular-strap/dist/angular-strap.tpl.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-datetimepicker/schema-form-date-time-picker.min.js"></script>
<script src="${contextPath}/bower_components/angular-xeditable/dist/js/xeditable.js"></script>
<script src="${contextPath}/bower_components/angular-media-queries/match-media.js"></script>
<script src="${contextPath}/bower_components/sf-localized-string/dist/sf-localized-string.min.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-file-upload/dist/sf-obiba-file-upload.min.js"></script>
<script src="${contextPath}/bower_components/sf-checkboxgroup/dist/sf-checkboxgroup.min.js"></script>
<script src="${contextPath}/bower_components/sf-typeahead/dist/sf-typeahead.min.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-simple-mde/dist/sf-obiba-simple-mde.js"></script>
<script src="${contextPath}/bower_components/obiba-country-codes/dist/all.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-countries-ui-select/dist/sf-obiba-countries-ui-select.js"></script>
<script src="${contextPath}/bower_components/sf-radio-group-collection/dist/sf-radio-group-collection.js"></script>
<script src="${contextPath}/bower_components/es6-shim/es6-shim.js"></script>
<script src="${contextPath}/bower_components/obiba-shims/dist/obiba-shims.min.js"></script>
<!-- endbower -->
<script src="${contextPath}/app/http-auth-interceptor.js"></script>
<script src="${contextPath}/app/app.js"></script>
<script src="${contextPath}/app/controllers.js"></script>
<script src="${contextPath}/app/services.js"></script>
<script src="${contextPath}/app/admin/admin.js"></script>
<script src="${contextPath}/app/admin/admin-router.js"></script>
<script src="${contextPath}/app/admin/admin-controller.js"></script>
<script src="${contextPath}/app/sets/sets.js"></script>
<script src="${contextPath}/app/search/search.js"></script>
<script src="${contextPath}/app/analysis/analysis.js"></script>
<script src="${contextPath}/app/config/config.js"></script>
<script src="${contextPath}/app/config/config-router.js"></script>
<script src="${contextPath}/app/config/config-controller.js"></script>
<script src="${contextPath}/app/config/config-service.js"></script>
<script src="${contextPath}/app/comment/comment.js"></script>
<script src="${contextPath}/app/comment/comment-service.js"></script>
<script src="${contextPath}/app/comment/comment-directive.js"></script>
<script src="${contextPath}/app/comment/comment-controllers.js"></script>
<script src="${contextPath}/app/commons/commons.js"></script>
<script src="${contextPath}/app/commons/classes/EditController.js"></script>
<script src="${contextPath}/app/commons/classes/ListController.js"></script>
<script src="${contextPath}/app/commons/classes/PermissionsController.js"></script>
<script src="${contextPath}/app/commons/classes/ViewController.js"></script>
<script src="${contextPath}/app/commons/commons-service.js"></script>
<script src="${contextPath}/app/commons/components/key-list/component.js"></script>
<script src="${contextPath}/app/commons/components/pagination/size-selector/component.js"></script>
<script src="${contextPath}/app/commons/components/entity-state-filter/component.js"></script>
<script src="${contextPath}/app/commons/components/search-field-selector/component.js"></script>
<script src="${contextPath}/app/permission/permission.js"></script>
<script src="${contextPath}/app/permission/permission-directives.js"></script>
<script src="${contextPath}/app/contact/contact.js"></script>
<script src="${contextPath}/app/contact/contact-controller.js"></script>
<script src="${contextPath}/app/contact/contact-service.js"></script>
<script src="${contextPath}/app/contact/contact-directive.js"></script>
<script src="${contextPath}/app/contact/contact-schemaform.js"></script>
<script src="${contextPath}/app/share-resource/share-resource.js"></script>
<script src="${contextPath}/app/publish/publish.js"></script>
<script src="${contextPath}/app/publish/publish-directives.js"></script>
<script src="${contextPath}/app/status/status.js"></script>
<script src="${contextPath}/app/status/status-directives.js"></script>
<script src="${contextPath}/app/entity-revisions/entity-revisions.js"></script>
<script src="${contextPath}/app/entity-revisions/entity-revisions-directive.js"></script>
<script src="${contextPath}/app/entity-revisions/entity-revisions-controller.js"></script>
<script src="${contextPath}/app/study/study.js"></script>
<script src="${contextPath}/app/study/classes/EditController.js"></script>
<script src="${contextPath}/app/study/classes/ViewController.js"></script>
<script src="${contextPath}/app/study/classes/ImportController.js"></script>
<script src="${contextPath}/app/study/study-directives.js"></script>
<script src="${contextPath}/app/study/study-router.js"></script>
<script src="${contextPath}/app/study/study-controller.js"></script>
<script src="${contextPath}/app/study/study-service.js"></script>
<script src="${contextPath}/app/dataset/dataset.js"></script>
<script src="${contextPath}/app/dataset/dataset-router.js"></script>
<script src="${contextPath}/app/dataset/dataset-directive.js"></script>
<script src="${contextPath}/app/dataset/dataset-controller.js"></script>
<script src="${contextPath}/app/dataset/dataset-service.js"></script>
<script src="${contextPath}/app/dataset/dataset-opal-table-schemaform.js"></script>
<script src="${contextPath}/app/network/network.js"></script>
<script src="${contextPath}/app/network/network-router.js"></script>
<script src="${contextPath}/app/network/network-controller.js"></script>
<script src="${contextPath}/app/network/network-service.js"></script>
<script src="${contextPath}/app/entity-sf-config/entity-sf-config.js"></script>
<script src="${contextPath}/app/entity-sf-config/entity-sf-config-directive.js"></script>
<script src="${contextPath}/app/entity-sf-config/entity-sf-config-service.js"></script>
<script src="${contextPath}/app/entity-sf-config/entity-sf-config-controller.js"></script>
<script src="${contextPath}/app/access-config/data-access-config.js"></script>
<script src="${contextPath}/app/access-config/data-access-config-router.js"></script>
<script src="${contextPath}/app/access-config/data-access-config-service.js"></script>
<script src="${contextPath}/app/access-config/data-access-config-controller.js"></script>
<script src="${contextPath}/app/project-config/project-config.js"></script>
<script src="${contextPath}/app/project-config/project-config-router.js"></script>
<script src="${contextPath}/app/project-config/project-config-service.js"></script>
<script src="${contextPath}/app/project-config/project-config-controller.js"></script>
<script src="${contextPath}/app/entity-config/entity-config.js"></script>
<script src="${contextPath}/app/entity-config/entity-config-router.js"></script>
<script src="${contextPath}/app/entity-config/entity-config-service.js"></script>
<script src="${contextPath}/app/entity-config/entity-config-controller.js"></script>
<script src="${contextPath}/app/entity-taxonomy-config/entity-taxonomy-config.js"></script>
<script src="${contextPath}/app/entity-taxonomy-config/entity-taxonomy-config-controller.js"></script>
<script src="${contextPath}/app/entity-taxonomy-config/entity-taxonomy-config-service.js"></script>
<script src="${contextPath}/app/entity-taxonomy-config/entity-taxonomy-config-directive.js"></script>
<script src="${contextPath}/app/access/data-access-request.js"></script>
<script src="${contextPath}/app/file-system/file-system.js"></script>
<script src="${contextPath}/app/file-system/file-system-router.js"></script>
<script src="${contextPath}/app/file-system/file-system-controller.js"></script>
<script src="${contextPath}/app/file-system/file-system-directive.js"></script>
<script src="${contextPath}/app/file-system/file-system-service.js"></script>
<script src="${contextPath}/app/project/project.js"></script>
<script src="${contextPath}/app/project/project-router.js"></script>
<script src="${contextPath}/app/project/project-controller.js"></script>
<script src="${contextPath}/app/project/project-service.js"></script>
<script src="${contextPath}/app/persons/persons.js"></script>
<script src="${contextPath}/app/persons/persons-list.js"></script>
<script src="${contextPath}/app/persons/person-view.js"></script>
<script src="${contextPath}/app/persons/person-memberships.js"></script>
<script src="${contextPath}/app/persons/entity-list.js"></script>
<script src="${contextPath}/app/persons/membership-roles.js"></script>
<script src="${contextPath}/app/entity-statistics-summary/entity-statistics-summary.js"></script>
<script src="${contextPath}/app/entity-statistics-summary/entity-statistics-summary-view.js"></script>
<script src="${contextPath}/app/entity-statistics-summary/entity-statistics-summary-item.js"></script>

<!-- endbuild -->
</body>
</html>
