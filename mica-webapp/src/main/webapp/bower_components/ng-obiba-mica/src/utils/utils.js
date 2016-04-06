'use strict';

angular.module('obiba.mica.utils', [])

  .factory('UserProfileService',
    function () {

      var getAttributeValue = function(attributes, key) {
        var result = attributes.filter(function (attribute) {
          return attribute.key === key;
        });

        return result && result.length > 0 ? result[0].value : null;
      };

      return {

        'getAttribute': function (attributes, key) {
          return getAttributeValue(attributes, key);
        },

        'getFullName': function (profile) {
          if (profile) {
            if (profile.attributes) {
              return getAttributeValue(profile.attributes, 'firstName') + ' ' + getAttributeValue(profile.attributes, 'lastName');
            }
            return profile.username;
          }
          return null;
        }
      };
    })

  .service('GraphicChartsConfigurations', function(){

    this.getClientConfig = function(){
      return true;
    };

    this.setClientConfig = function(){
      return true;
    };
  })
  
  .directive('fixedHeader', ['$timeout','$window', function ($timeout, $window) {
    return {
      restrict: 'A',
      scope: {
        tableMaxHeight: '@'
      },
      link: function ($scope, $elem) {
        var elem = $elem[0];
        function isVisible(el) {
          var style = $window.getComputedStyle(el);
          return (style.display !== 'none' && el.offsetWidth !==0 );
        }
        function isTableReady() {
          return isVisible(elem.querySelector('tbody')) && elem.querySelector('tbody tr:first-child') !== null;
        }
        // wait for content to load into table and to have at least one row, tdElems could be empty at the time of execution if td are created asynchronously (eg ng-repeat with promise)
        var unbindWatch = $scope.$watch(isTableReady,
          function (newValue) {
            if (newValue === true) {
              // reset display styles so column widths are correct when measured below
              angular.element(elem.querySelectorAll('thead, tbody, tfoot')).css('display', '');

              // wrap in $timeout to give table a chance to finish rendering
              $timeout(function () {
                // set widths of columns
                angular.forEach(elem.querySelectorAll('tr:first-child th'), function (thElem, i) {

                  var tdElems = elem.querySelector('tbody tr:first-child td:nth-child(' + (i + 1) + ')');
                  var tfElems = elem.querySelector('tfoot tr:first-child td:nth-child(' + (i + 1) + ')');


                  var columnWidth = tdElems ? tdElems.offsetWidth : thElem.offsetWidth;
                  if(tdElems) {
                    tdElems.style.width = columnWidth + 'px';
                  }
                  if(thElem) {
                    thElem.style.width = columnWidth + 'px';
                  }
                  if (tfElems) {
                    tfElems.style.width = columnWidth + 'px';
                  }
                });
                // set css styles on thead and tbody
                angular.element(elem.querySelectorAll('thead, tfoot')).css('display', 'block');

                angular.element(elem.querySelectorAll('tbody')).css({
                  'display': 'block',
                  'max-height': $scope.tableMaxHeight || 'inherit',
                  'overflow': 'auto'
                });


                // reduce width of last column by width of scrollbar
                var tbody = elem.querySelector('tbody');
                var scrollBarWidth = tbody.offsetWidth - tbody.clientWidth;
                if (scrollBarWidth > 0) {
                  // for some reason trimming the width by 2px lines everything up better
                  scrollBarWidth -= 2;
                  var lastColumn = elem.querySelector('tbody tr:first-child td:last-child');
                  lastColumn.style.width = (lastColumn.offsetWidth - scrollBarWidth) + 'px';
                }
              });

              //we only need to watch once
              unbindWatch();
            }
          });
      }
    };
  }]);

