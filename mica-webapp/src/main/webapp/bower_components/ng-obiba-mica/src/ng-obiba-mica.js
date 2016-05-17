'use strict';

function NgObibaMicaUrlProvider() {
  var registry = {
    'DataAccessFormConfigResource': 'ws/config/data-access-form',
    'DataAccessRequestsResource': 'ws/data-access-requests',
    'DataAccessRequestResource': 'ws/data-access-request/:id',
    'DataAccessRequestAttachmentDownloadResource': '/ws/data-access-request/:id/attachments/:attachmentId/_download',
    'DataAccessRequestDownloadPdfResource': '/ws/data-access-request/:id/_pdf',
    'DataAccessRequestCommentsResource': 'ws/data-access-request/:id/comments',
    'DataAccessRequestCommentResource': 'ws/data-access-request/:id/comment/:commentId',
    'DataAccessRequestStatusResource': 'ws/data-access-request/:id/_status?to=:status',
    'TempFileUploadResource': 'ws/files/temp',
    'TempFileResource': 'ws/files/temp/:id',
    'TaxonomiesSearchResource': 'ws/taxonomies/_search',
    'TaxonomiesResource': 'ws/taxonomies/_filter',
    'TaxonomyResource': 'ws/taxonomy/:taxonomy/_filter',
    'VocabularyResource': 'ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter',
    'JoinQuerySearchResource': 'ws/:type/_rql?query=:query',
    'JoinQueryCoverageResource': 'ws/variables/_coverage?query=:query',
    'JoinQueryCoverageDownloadResource': 'ws/variables/_coverage_download?query=:query',
    'VariablePage': '',
    'NetworkPage': '#/network/:network',
    'StudyPage': '#/study/:study',
    'StudyPopulationsPage': '#/study/:study',
    'DatasetPage': '#/:type/:dataset',
    'BaseUrl': '/',
    'FileBrowserFileResource': 'ws/file/:path/',
    'FileBrowserSearchResource': 'ws/files-search/:path',
    'FileBrowserDownloadUrl': 'ws/draft/file-dl/:path?inline=:inline',
    'GraphicsSearchRootUrl': '#/search'
  };

  function UrlProvider(registry) {
    var urlRegistry = registry;

    this.getUrl = function (resource) {
      if (resource in urlRegistry) {
        return urlRegistry[resource];
      }

      return null;
    };
  }

  this.setUrl = function (key, url) {
    if (key in registry) {
      registry[key] = url;
    }
  };

  this.$get = function () {
    return new UrlProvider(registry);
  };
}

/* exported NgObibaMicaTemplateUrlFactory */
function NgObibaMicaTemplateUrlFactory() {
  var factory = {registry: null};

  function TemplateUrlProvider(registry) {
    var urlRegistry = registry;

    this.getHeaderUrl = function (key) {
      if (key in urlRegistry) {
        return urlRegistry[key].header;
      }

      return null;
    };

    this.getFooterUrl = function (key) {
      if (key in urlRegistry) {
        return urlRegistry[key].footer;
      }

      return null;
    };
  }

  factory.setHeaderUrl = function (key, url) {
    if (key in this.registry) {
      this.registry[key].header = url;
    }
  };

  factory.setFooterUrl = function (key, url) {
    if (key in this.registry) {
      this.registry[key].footer = url;
    }
  };

  factory.$get = function () {
    return new TemplateUrlProvider(this.registry);
  };

  this.create = function (inputRegistry) {
    factory.registry = inputRegistry;
    return factory;
  };
}

angular.module('ngObibaMica', [
    'schemaForm',
    'ngCookies',
    'obiba.mica.utils',
    'obiba.mica.file',
    'obiba.mica.attachment',
    'obiba.mica.access',
    'obiba.mica.search',
    'obiba.mica.graphics',
    'obiba.mica.localized',
    'obiba.mica.fileBrowser',
    'angularUtils.directives.dirPagination',
  ])
  .constant('USER_ROLES', {
    all: '*',
    admin: 'mica-administrator',
    reviewer: 'mica-reviewer',
    editor: 'mica-editor',
    user: 'mica-user',
    dao: 'mica-data-access-officer'
  })
  .config(['$provide', 'paginationTemplateProvider', function ($provide, paginationTemplateProvider) {
    $provide.provider('ngObibaMicaUrl', NgObibaMicaUrlProvider);
    paginationTemplateProvider.setPath('views/pagination-template.html');
  }]);

