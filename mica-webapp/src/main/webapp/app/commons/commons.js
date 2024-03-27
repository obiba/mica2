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

mica.commons = angular.module('mica.commons', []);

mica.commons.consts = {};

mica.commons.consts.PersonTypeToResourceMap = {
  'STUDY': 'individual-study',
  'INITIATIVE': 'harmonization-study',
  'NETWORK': 'network',
};
mica.commons.consts.PersonResourceToTypeMap = {
  'individual-study': 'STUDY',
  'harmonization-study': 'INITIATIVE',
  'network': 'NETWORK',
};
mica.commons.consts.PersonTypeNames = {
  'STUDY': 'STUDY',
  'INITIATIVE': 'INITIATIVE',
  'NETWORK': 'NETWORK',
};

mica.commons.EntityState = function($q, parentScope) {

  function createDirtyObservable() {
    var emitter = parentScope.$new();
    return {
      form: {$dirty: false},
      $on: angular.bind(emitter, emitter.$on),
      setDirty: function (value) {
        this.form.$dirty = value;
      },
      isDirty: function() {
        return this.form.$dirty;
      }
    };
  }

  var listeners = [];
  var dirtyObservable = createDirtyObservable();

  this.registerListener = function (listener) {
    if (listeners.indexOf(listener) < 0) {
      if (typeof listener.onSave === 'function') {
        listeners.push(listener);
      } else {
        throw new Error('EntityState - listener must define onSave() method.');
      }
    }
  };

  this.onSave = function () {
    return listeners.map(function (listener) {
      return listener.onSave().$promise;
    });
  };

  this.getDirtyObservable = function() {
    return dirtyObservable;
  };

  this.setDirty = function(value) {
    dirtyObservable.setDirty(value);
  };

  this.isDirty = function(value) {
    dirtyObservable.isDirty(value);
  };

  return this;
};
