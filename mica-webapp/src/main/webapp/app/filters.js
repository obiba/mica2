'use strict';

mica.filter('fromNow', ['moment', function(moment) {
  return function(dateString) {
    return moment(dateString).fromNow();
  };
}]);
