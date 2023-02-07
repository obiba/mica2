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

mica.study
  .directive('numberOfParticipants', [function () {
    return {
      restrict: 'E',
      templateUrl: 'app/study/views/common/number-of-participants.html',
      scope: {
        numberOfParticipants: '=',
        lang: '='
      },
      link: function (scope) {
        scope.$watch('numberOfParticipants.sample.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.sample.number;
          }
        }, true);

        scope.$watch('numberOfParticipants.participant.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.participant.number;
          }
        }, true);
      }
    };
  }])
  .component('tagByConcept', {
    bindings: {
      taxos: '<'
    },
    templateUrl: 'app/study/views/common/tag-by-concept-component.html',
    controllerAs: '$ctrl',
    controller: ['TaxonomyFilterResource', function (TaxonomyFilterResource) {
      var self = this;

      function reset() {
        self.chosenTaxo = null;
        self.vocabChoices = [];
        self.termChoices = [];
        self.readyToTag = false;
      }      

      reset();

      TaxonomyFilterResource.query().$promise.then(function (res) {
        if (res && res.length > 0) {
          self.taxoChoices = res.filter(r => self.taxos.indexOf(r.name) > -1);
        }

        return res;
      });

      self.taxoSelected = function() {
        if (self.chosenTaxo) {
          self.vocabChoices = (self.taxoChoices.filter(t => t.name === self.chosenTaxo)[0] || {vocabularies: []}).vocabularies;
        } else {
          self.vocabChoices = [];
          self.readyToTag = false;
        }
      };

      self.vocabsSelected = function() {
        if (self.chosenVocabs && self.chosenVocabs.length === 1) {
          self.readyToTag = true;
          self.termChoices = (self.vocabChoices.filter(v => v.name === self.chosenVocabs[0])[0] || {terms: []}).terms;
        } else if (self.chosenVocabs && self.chosenVocabs.length > 0) {
          self.readyToTag = true;
          self.termChoices = [];
        } else {
          self.readyToTag = false;
          self.termChoices = [];
        }
      };

      self.addTags = function() {
        var attributes = [];

        var namespace = Array.isArray(self.chosenTaxo) ? self.chosenTaxo[0] : self.chosenTaxo;

        if (Array.isArray(self.chosenVocabs) && self.chosenVocabs.length > 1) {
          self.chosenVocabs.forEach(vocab => {
            attributes.push({namespace: namespace, name: vocab});
          });
        } else if (Array.isArray(self.chosenVocabs) && self.chosenVocabs.length === 1) {
          var name = Array.isArray(self.chosenVocabs) ? self.chosenVocabs[0] : self.chosenVocabs;

          if (Array.isArray(self.chosenTerms)) {
            self.chosenTerms.forEach(term => {
              attributes.push({namespace: namespace, name: name, values: [{lang: 'und', value: term}]});
            });
          } else {
            attributes.push({namespace: namespace, name: name});
          }
        }

        reset();
      };
    }]
  });
