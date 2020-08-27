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

mica.revisions
  .directive('entityRevisions', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        title: '@',
        id: '@',
        state: '=',
        onFetchRevisions: '&',
        onViewRevision: '&',
        onRestoreRevision: '&',
        onViewDiff: '&'
      },
      templateUrl: 'app/entity-revisions/entity-revisions-template.html',
      controller: 'RevisionsController'
    };
  }])
  .filter('diffHighlight', function() {
    function escape(str) {
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function removeTrailingSpace(str) {
      return str.replace(/\s+$/, '');
    }

    function toWordArray(str) {
      return str || str.length > 0 ? str.split(/\s+/) : [];
    }

    function getSpaces(str) {
      let spaces = str.match(/\s+/g);

      if (spaces === null) {
        spaces = [''];
      } else {
        spaces.push('');
      }

      return spaces;
    }

    function updateWordRepository(wordRepository, wordArray) {
      for (let index = 0; index < wordArray.length; index++) {
        if (!wordRepository[wordArray[index]]) {
          wordRepository[wordArray[index]] = { rows: [] };
        }

        wordRepository[wordArray[index]].rows.push(index);
      }
    }

    function diff(sourceWordArray, otherWordArray) {
      const sourceWordRepository = {};
      const otherWordRepository = {};

      updateWordRepository(sourceWordRepository, sourceWordArray);
      updateWordRepository(otherWordRepository, otherWordArray);

      for (const key in sourceWordRepository) {
        if (sourceWordRepository.hasOwnProperty(key) && sourceWordRepository[key].rows.length === 1 && otherWordRepository.hasOwnProperty(key) && otherWordRepository[key].rows.length === 1) {
          sourceWordArray[sourceWordRepository[key].rows[0]] = {
            text: sourceWordArray[sourceWordRepository[key].rows[0]],
            row: otherWordRepository[key].rows[0],
          };

          otherWordArray[otherWordRepository[key].rows[0]] = {
            text: otherWordArray[otherWordRepository[key].rows[0]],
            row: sourceWordRepository[key].rows[0],
          };
        }
      }

      for (let index = 0; index < sourceWordArray.length; index++) {
        if (
          sourceWordArray[index].text &&
          (sourceWordArray[index + 1] && !sourceWordArray[index + 1].text) &&
          sourceWordArray[index].row + 1 < otherWordArray.length &&
          !otherWordArray[sourceWordArray[index].row + 1].text &&
          sourceWordArray[index + 1] === otherWordArray[sourceWordArray[index].row + 1]
        ) {
          sourceWordArray[index + 1] = {
            text: sourceWordArray[index + 1],
            row: sourceWordArray[index].row + 1,
          };

          otherWordArray[sourceWordArray[index].row + 1] = {
            text: otherWordArray[sourceWordArray[index].row + 1],
            row: index + 1,
          };
        }
      }

      for (let index = sourceWordArray.length - 1; index > 0; index--) {
        if (
          sourceWordArray[index].text &&
          !sourceWordArray[index - 1].text &&
          sourceWordArray[index].row > 0 &&
          !otherWordArray[sourceWordArray[index].row - 1].text &&
          sourceWordArray[index - 1] === otherWordArray[sourceWordArray[index].row - 1]
        ) {
          sourceWordArray[index - 1] = {
            text: sourceWordArray[index - 1],
            row: sourceWordArray[index].row - 1,
          };

          otherWordArray[sourceWordArray[index].row - 1] = {
            text: otherWordArray[sourceWordArray[index].row - 1],
            row: index - 1,
          };
        }
      }

      return { source: sourceWordArray, other: otherWordArray };
    }

    function highlight(source, other) {
      let sourceWithoutTrailingSpace = removeTrailingSpace(source);
      let otherWithoutTrailingSpace = removeTrailingSpace(other);

      let diffOutput = diff(toWordArray(sourceWithoutTrailingSpace), toWordArray(otherWithoutTrailingSpace));
      let result = '';

      const sourceSpaces = getSpaces(sourceWithoutTrailingSpace);
      const otherSpaces = getSpaces(otherWithoutTrailingSpace);

      if (diffOutput.other.length === 0) {
        for (let index = 0; index < diffOutput.other.length && !diffOutput.other[index].text; index++) {
          result += '<ins>' + escape(diffOutput.other[index]) + otherSpaces[index] + '</ins>';
        }
      } else {
        if (!diffOutput.source[0].text) {
          for (let index = 0; index < diffOutput.other.length && !diffOutput.other[index].text; index++) {
            result += '<ins>' + escape(diffOutput.other[index]) + otherSpaces[index] + '</ins>';
          }
        }

        for (let index = 0; index < diffOutput.source.length; index++) {
          if (!diffOutput.source[index].text) {
            result += '<del>' + escape(diffOutput.source[index]) + sourceSpaces[index] + '</del>';
          } else {
            let pre = '';
            for (let i = diffOutput.source[index].row + 1; i < diffOutput.other.length && !diffOutput.other[i].text; i++) {
              pre += '<ins>' + escape(diffOutput.other[i]) + otherSpaces[i] + '</ins>';
            }

            result += ' ' + diffOutput.source[index].text + sourceSpaces[index] + pre;
          }
        }
      }

      return result;
    }

    return highlight;
  });
