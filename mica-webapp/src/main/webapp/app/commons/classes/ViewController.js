'use strict';

mica.commons.ViewController = function ($location) {

  var self = this;
  self.sfForm = {};
  self.sfOptions = {pristine: {errors: true, success: false}};
  self.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

  self.inViewMode = function () {
    return self.getViewMode() === self.Mode.View;
  };

  self.getViewMode = function () {
    var result = /\/(revision[s\/]*|files|permissions|comments)/.exec($location.path());
    if (result && result.length > 1) {
      switch (result[1]) {
        case 'revision':
        case 'revisions':
          return self.Mode.Revision;
        case 'files':
          return self.Mode.File;
        case 'permissions':
          return self.Mode.Permission;
        case 'comments':
          return self.Mode.Comment;
      }
    }
    return self.Mode.View;
  };

  self.print = function () {
    setTimeout(function () {
      window.print();
    }, 250);
  };
};

/* exported processMemberships, STUDY_EVENTS */
function processMemberships(document) {
  document.memberships = document.memberships || [];

  return document.memberships.map(function (m) {
    if (!m.members) {
      m.members = [];
    }
    return m;
  }).reduce(function (res, m) {
    res[m.role] = m.members;
    return res;
  }, {});
}

var STUDY_EVENTS = {studyUpdated: 'event:study-updated'};
