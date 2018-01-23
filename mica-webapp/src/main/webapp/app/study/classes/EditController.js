/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/**
 * Basic study edit controller class
 * Must be overridden before use
 *
 * @param $scope
 * @param $routeParams
 * @param $location
 * @param $log
 * @param FormServerValidation
 * @param FormDirtyStateObserver
 * @param StudyUpdateWarningService
 * @constructor
 */
mica.study.BaseEditController = function (
  $scope,
  $routeParams,
  $location,
  $log,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService) {

  mica.commons.EditController.call(this);

  var self = this;

  self.save = function () { };

  self.type = '';

  self.cancel = function () {
    $location.path('/' + self.type + '-study' + ($routeParams.id ? '/' + $routeParams.id : '')).replace();
  };

  self.updateStudy = function () {
    $log.debug('Updating study', $scope.study);
    $scope.study.$save({comment: $scope.revision.comment}, function () {
      FormDirtyStateObserver.unobserve();
      $location.url('/' + self.type + '-study/' + $routeParams.id);
    }, self.saveErrorHandler);
  };

  self.saveErrorHandler = function (response) {
    if (response.status === 409) {
      StudyUpdateWarningService.popup(response.data, 'study.delete-conflict', self.getConflictMessageKey());
    } else {
      FormServerValidation.error(response, $scope.form, $scope.languages);
    }
  };

  self.initializeForm = function () { };

};

mica.study.BaseEditController.prototype = Object.create(mica.commons.EditController.prototype);
mica.study.BaseEditController.prototype.constructor = mica.study.BaseEditController;
mica.study.BaseEditController.prototype.getConflictMessageKey = function () {
  return 'study.delete-conflict-message';
};

mica.study.EditController = function (
  $scope,
  $rootScope,
  $routeParams,
  $location,
  $filter,
  $translate,
  $q,
  $log,
  MicaConfigResource,
  SfOptionsService,
  EntityFormResource,
  DraftStudiesResource,
  DraftStudyResource,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService) {

  mica.study.BaseEditController.call(this, $scope, $routeParams, $location, $log, FormServerValidation, FormDirtyStateObserver, StudyUpdateWarningService);

  var self = this;
  self.study = {model: {}};
  self.files = [];

  self.save = function () {
    $scope.study.logo = $scope.files.length > 0 ? $scope.files[0] : null;
    if (!$scope.study.logo) { delete $scope.study.logo; }

    $scope.$broadcast('schemaFormValidate');

    if (!$scope.form.$valid) { $scope.form.saveAttempted = true; }
    else {
      if ($routeParams.id) {
        self.updateStudy();
      } else {
        createStudy();
      }
    }
  };

  self.type = 'individual';

  self.updateStudy = function () {
    $log.debug('Updating study', $scope.study);
    $scope.study.$save({comment: $scope.revision.comment}, function (response) {
      FormDirtyStateObserver.unobserve();
      $location.path('/individual-study/' + response.study.id).replace();

      if (response.potentialConflicts) {
        StudyUpdateWarningService.popup(response.potentialConflicts, 'study.potential-conflicts', 'study.potential-conflicts-message');
      }
    }, self.saveErrorHandler);
  };

  self.saveErrorHandler = function (response) {
    FormServerValidation.error(response, $scope.form, $scope.languages);
  };

  self.initializeForm = function() {
    $q.all([
      MicaConfigResource.get().$promise,
      SfOptionsService.transform(),
      EntityFormResource.get({target: 'individual-study', locale: $translate.use()}).$promise,
      $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}).$promise : null
    ]).then(function (data) {
      var micaConfig = data[0];
      var sfLanguages = {};

      micaConfig.languages.forEach(function (lang) {
        sfLanguages[lang] = $filter('translate')('language.' + lang);
      });

      self.languages = micaConfig.languages;
      self.sfOptions = data[1];
      self.sfOptions.formDefaults = {languages: sfLanguages};
      self.sfForm = data[2];

      if (data[3]) {
        self.study = data[3];
        self.files = self.study.logo ? [self.study.logo] : [];
      }

      angular.extend($scope, self);
      FormDirtyStateObserver.observe($scope);
    });
  };

  function createStudy() {
    $log.debug('Creating new study', $scope.study);
    $scope.study['obiba.mica.CollectionStudyDto.type'] = {};
    DraftStudiesResource.save($scope.study, function (resource, getResponseHeaders) {
      FormDirtyStateObserver.unobserve();
      var parts = getResponseHeaders().location.split('/');
      $location.path('/individual-study/' + parts[parts.length - 1]).replace();
    }, self.saveErrorHandler);
  }

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();
};

mica.study.EditController.prototype = Object.create(mica.study.BaseEditController.prototype);
mica.study.EditController.prototype.constructor = mica.study.EditController;

mica.study.HarmonizationStudyEditController = function (
  $scope,
  $rootScope,
  $routeParams,
  $location,
  $filter,
  $translate,
  $q,
  $log,
  MicaConfigResource,
  SfOptionsService,
  EntityFormResource,
  DraftStudiesResource,
  DraftStudyResource,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService) {

  mica.study.BaseEditController.call(this, $scope, $routeParams, $location, $log, FormServerValidation, FormDirtyStateObserver, StudyUpdateWarningService);

  var self= this;

  self.study = {model: {}};
  self.files = [];

  self.save = function () {
    $scope.study.logo = $scope.files.length > 0 ? $scope.files[0] : null;
    if (!$scope.study.logo) { delete $scope.study.logo; }

    $scope.$broadcast('schemaFormValidate');

    if (!$scope.form.$valid) { $scope.form.saveAttempted = true; }
    else {
      if ($routeParams.id) {
        self.updateStudy();
      } else {
        createStudy();
      }
    }
  };

  self.type = 'harmonization';

  self.updateStudy = function () {
    $log.debug('Updating study', $scope.study);
    $scope.study.$save({comment: $scope.revision.comment}, function (response) {
      FormDirtyStateObserver.unobserve();
      $location.path('/harmonization-study/' + response.study.id).replace();

      if (response.potentialConflicts) {
        StudyUpdateWarningService.popup(response.potentialConflicts, 'study.potential-conflicts', 'study.potential-conflicts-message');
      }
    }, self.saveErrorHandler);
  };

  self.saveErrorHandler = function (response) {
    FormServerValidation.error(response, $scope.form, $scope.languages);
  };

  self.initializeForm = function() {
    $q.all([
      MicaConfigResource.get().$promise,
      SfOptionsService.transform(),
      EntityFormResource.get({target: 'harmonization-study', locale: $translate.use()}).$promise,
      $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}).$promise : null
    ]).then(function (data) {
      var micaConfig = data[0];
      var sfLanguages = {};

      micaConfig.languages.forEach(function (lang) {
        sfLanguages[lang] = $filter('translate')('language.' + lang);
      });

      self.languages = micaConfig.languages;
      self.sfOptions = data[1];
      self.sfOptions.formDefaults = {languages: sfLanguages};
      self.sfForm = data[2];

      if (data[3]) {
        self.study = data[3];
        self.files = self.study.logo ? [self.study.logo] : [];
      }

      angular.extend($scope, self);
      FormDirtyStateObserver.observe($scope);
    });
  };

  function createStudy() {
    $log.debug('Creating new study', $scope.study);
    $scope.study['obiba.mica.HarmonizationStudyDto.type'] = {};
    DraftStudiesResource.save($scope.study, function (resource, getResponseHeaders) {
      FormDirtyStateObserver.unobserve();
      var parts = getResponseHeaders().location.split('/');
      $location.path('/harmonization-study/' + parts[parts.length - 1]).replace();
    }, self.saveErrorHandler);
  }

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();
};

mica.study.HarmonizationStudyEditController.prototype = Object.create(mica.study.BaseEditController.prototype);
mica.study.HarmonizationStudyEditController.prototype.constructor = mica.study.HarmonizationStudyEditController;

mica.study.PopulationEditController = function (
  $scope,
  $rootScope,
  $routeParams,
  $location,
  $filter,
  $translate,
  $q,
  $log,
  MicaConfigResource,
  SfOptionsService,
  EntityFormResource,
  DraftStudyResource,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService,
  MicaUtil
) {

  mica.study.BaseEditController.call(this, $scope, $routeParams, $location, $log, FormServerValidation, FormDirtyStateObserver, StudyUpdateWarningService);

  var self = this;
  self.population = {model: {}};
  self.populationSfForm = {};

  self.save = function (form) {
    if (!validate(form)) { form.saveAttempted = true; }
    else { self.updateStudy(); }
  };

  self.type = 'individual';

  self.initializeForm = function() {
    $q.all([
      MicaConfigResource.get().$promise,
      SfOptionsService.transform(),
      EntityFormResource.get({target: 'population', locale: $translate.use()}).$promise,
      DraftStudyResource.get({id: $routeParams.id}).$promise
    ]).then(function (data) {
      var micaConfig = data[0];
      var sfLanguages = {};

      micaConfig.languages.forEach(function (loc) {
        sfLanguages[loc] = $filter('translate')('language.' + loc);
      });

      self.languages = micaConfig.languages;
      self.sfOptions = data[1];
      self.sfOptions.formDefaults = {languages: sfLanguages};
      self.populationSfForm = data[2];

      self.study = data[3];
      if (!self.study.populations) { self.study.populations = []; }
      if ($routeParams.pid) {
        self.population = self.study.populations.filter(function (p) { return p.model._id === $routeParams.pid; })[0] || self.population;
      } else {
        var currentPopulationIds = self.study.populations.map(function (p) { return p.model._id; });
        self.population.model._id = MicaUtil.generateNextId(currentPopulationIds);
        self.population.weight = self.study.populations.length;

        self.study.populations.push(self.population);
      }

      angular.extend($scope, self);
      FormDirtyStateObserver.observe($scope);
    });
  };

  function validate(form) {
    $scope.$broadcast('schemaForm.error._id','uniqueId',true);
    $scope.$broadcast('schemaFormValidate');

    if ($scope.study.populations.filter(function (p) {
        return p.model._id === $scope.population.model._id;
      }).length > 1) {
      $scope.$broadcast('schemaForm.error._id','uniqueId',false);
    }

    return form.$valid;
  }

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();
};

mica.study.PopulationEditController.prototype = Object.create(mica.study.BaseEditController.prototype);
mica.study.PopulationEditController.prototype.constructor = mica.study.PopulationEditController;
mica.study.PopulationEditController.prototype.getConflictMessageKey = function () {
  return 'study.population-delete-conflict-message';
};

mica.study.HarmonizationPopulationEditController = function (
  $scope,
  $rootScope,
  $routeParams,
  $location,
  $filter,
  $translate,
  $q,
  $log,
  MicaConfigResource,
  SfOptionsService,
  EntityFormResource,
  DraftStudyResource,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService,
  MicaUtil
) {

  mica.study.BaseEditController.call(this, $scope, $routeParams, $location, $log, FormServerValidation, FormDirtyStateObserver, StudyUpdateWarningService);

  var self = this;
  self.population = {model: {}};
  self.populationSfForm = {};

  self.save = function (form) {
    if (!validate(form)) { form.saveAttempted = true; }
    else { self.updateStudy(); }
  };

  self.type = 'harmonization';

  self.initializeForm = function() {
    $q.all([
      MicaConfigResource.get().$promise,
      SfOptionsService.transform(),
      EntityFormResource.get({target: 'harmonization-population', locale: $translate.use()}).$promise,
      DraftStudyResource.get({id: $routeParams.id}).$promise
    ]).then(function (data) {
      var micaConfig = data[0];
      var sfLanguages = {};

      micaConfig.languages.forEach(function (loc) {
        sfLanguages[loc] = $filter('translate')('language.' + loc);
      });

      self.languages = micaConfig.languages;
      self.sfOptions = data[1];
      self.sfOptions.formDefaults = {languages: sfLanguages};
      self.populationSfForm = data[2];

      self.study = data[3];
      if (!self.study.populations) { self.study.populations = []; }
      if ($routeParams.pid) {
        self.population = self.study.populations.filter(function (p) { return p.model._id === $routeParams.pid; })[0] || self.population;
      } else {
        var currentPopulationIds = self.study.populations.map(function (p) { return p.model._id; });
        self.population.model._id = MicaUtil.generateNextId(currentPopulationIds);

        self.study.populations.push(self.population);
      }

      angular.extend($scope, self);
      FormDirtyStateObserver.observe($scope);
    });
  };

  function validate(form) {
    $scope.$broadcast('schemaFormValidate');

    if ($scope.study.populations.filter(function (p) {
        return p.model._id === $scope.population.model._id;
      }).length > 1) {
      form.$setValidity('population_id', false);
    } else {
      form.$setValidity('population_id', true);
    }

    return form.$valid;
  }

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();
};

mica.study.HarmonizationPopulationEditController.prototype = Object.create(mica.study.BaseEditController.prototype);
mica.study.HarmonizationPopulationEditController.prototype.constructor = mica.study.HarmonizationPopulationEditController;
mica.study.HarmonizationPopulationEditController.prototype.getConflictMessageKey = function () {
  return 'study.population-delete-conflict-message';
};

mica.study.DataCollectionEventEditController = function (
  $scope,
  $rootScope,
  $routeParams,
  $location,
  $filter,
  $translate,
  $q,
  $log,
  MicaConfigResource,
  SfOptionsService,
  EntityFormResource,
  DraftStudyResource,
  FormServerValidation,
  FormDirtyStateObserver,
  StudyUpdateWarningService,
  MicaUtil
) {

  mica.study.BaseEditController.call(this, $scope, $routeParams, $location, $log, FormServerValidation, FormDirtyStateObserver, StudyUpdateWarningService);

  var self = this;
  self.dce = {model: {}};
  self.dceSfForm = {};

  self.save = function (form) {
    if (!validate(form)) { form.saveAttempted = true; }
    else { self.updateStudy(); }
  };

  self.type = 'individual';

  self.initializeForm = function () {
    $q.all([
      MicaConfigResource.get().$promise,
      SfOptionsService.transform(),
      EntityFormResource.get({target: 'data-collection-event', locale: $translate.use()}).$promise,
      DraftStudyResource.get({id: $routeParams.id}).$promise
    ]).then(function (data) {
      var micaConfig = data[0];
      var sfLanguages = {};

      micaConfig.languages.forEach(function (lang) {
        sfLanguages[lang] = $filter('translate')('language.' + lang);
      });

      self.languages = micaConfig.languages;
      self.sfOptions = data[1];
      self.sfOptions.formDefaults = {languages: sfLanguages};

      self.dceSfForm = data[2];
      self.study = data[3];
      self.population = self.study.populations.filter(function (p) { return p.model._id === $routeParams.pid; })[0] || self.population;
      if (!self.population.dataCollectionEvents) { self.population.dataCollectionEvents = []; }
      if ($routeParams.dceId) {
        self.dce = findDataCollectionEvent($routeParams.dceId) || self.dce;
      } else {
        var currentDceIds = self.population.dataCollectionEvents.map(function (dce) { return dce.model._id; });

        var templateDceId = $location.search().sourceDceId;

        if (templateDceId) {
          var templateDce = findDataCollectionEvent(templateDceId) || {};
          angular.copy(templateDce, self.dce);
          delete self.dce.startYear;
          delete self.dce.startMonth;
          delete self.dce.endYear;
          delete self.dce.endMonth;
        }

        self.dce.model._id = MicaUtil.generateNextId(currentDceIds);
        self.dce.weight = self.population.dataCollectionEvents.length;
        self.population.dataCollectionEvents.push(self.dce);
      }

      angular.extend($scope, self);
      FormDirtyStateObserver.observe($scope);
    });
  };

  function findDataCollectionEvent(dataCollectionEventId) {
    return self.population && self.population.dataCollectionEvents ?
      self.population.dataCollectionEvents.filter(function (dce) { return dce.model._id === dataCollectionEventId; })[0] :
      null;
  }

  function validate(form) {
    $scope.$broadcast('schemaForm.error._id','uniqueId',true);
    $scope.$broadcast('schemaFormValidate');

    if ($scope.population.dataCollectionEvents.filter(function (d) {
        return d.model._id === $scope.dce.model._id;
      }).length > 1) {
      $scope.$broadcast('schemaForm.error._id','uniqueId',false);
    }

    return form.$valid;
  }

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();
};

mica.study.DataCollectionEventEditController.prototype = Object.create(mica.study.BaseEditController.prototype);
mica.study.DataCollectionEventEditController.prototype.constructor = mica.study.DataCollectionEventEditController;
mica.study.DataCollectionEventEditController.prototype.getConflictMessageKey = function () {
  return 'study.population-dce-delete-conflict-message';
};
