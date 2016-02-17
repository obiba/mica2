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

  .service('StringUtils', ['LocalizedValues',function(LocalizedValues){

    this.localize = function (values, lang) {
      return LocalizedValues.forLocale(values, lang);
    };

    this.truncate = function (text, size) {
      var max = size || 30;
      return text.length > max ? text.substring(0, max) + '...' : text;
    };

    return this;
  }])
  .service('GraphicChartsConfigurations', function(){
  this.getClientConfig = function(){
    return true;
  };
    this.setClientConfig = function(){
      return true;
    };
});

