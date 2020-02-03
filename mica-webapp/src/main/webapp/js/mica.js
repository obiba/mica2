/* exported micajs */
'use strict';

var micajs = (function() {

  // get the stats
  var micaStats = function(type, params, onSuccess, onFailure) {
    var url = '/ws/' + type + '/_rql';
    if (params && Object.keys(params).length>0) {
      var query = Object.keys(params).map(key => key + '=' + params[key]).join('&');
      url = url + '?' + query;
    }
    $.ajax({
      url: url,
      type: 'GET',
      dataType : 'json',
    })
      .done(function(json) {
        console.log('The request has succeeded');
        console.log(json);
        if (onSuccess) {
          onSuccess(json);
        }
      })
      .fail(function(xhr, status, errorThrown) {
        console.log('The request has failed');
        console.log('  Error: ' + errorThrown);
        console.log('  Status: ' + status + ' ' + xhr.status);
        console.dir(xhr);
        if (onFailure) {
          onFailure(xhr, errorThrown);
        }
      });
  };

  var micaRedirectError = function(xhr, errorThrown) {
    $.redirect('/error', {
      'status': xhr.status,
      'message': errorThrown
    }, 'POST');
  };

  var micaSignin = function(formId, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      var form = $(this);
      var url = '../ws/auth/sessions';
      var data = form.serialize(); // serializes the form's elements.

      $.ajax({
        type: 'POST',
        url: url,
        data: data})
        .done(function() {
          var redirect = 'home';
          var q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          }
          $.redirect(redirect, {}, 'GET');
        })
        .fail(function(xhr, status, errorThrown) {
          console.log('The request has failed');
          console.log('  Error: ' + errorThrown);
          console.log('  Status: ' + status + ' ' + xhr.status);
          console.dir(xhr);
          if (onFailure) {
            var banned = xhr.responseJSON && xhr.responseJSON.message === 'User is banned';
            onFailure(banned, xhr.responseJSON);
          }
        });
    });
  };

  var micaSignout = function(pathPrefix) {
    $.ajax({
      type: 'DELETE',
      url: pathPrefix + '/ws/auth/session/_current'
    })
    .always(function() {
      var redirect = pathPrefix + '/home';
      $.redirect(redirect, {}, 'GET');
    });
  };

  var micaChangeLanguage = function(lang) {
    let key = 'language';
    let value = encodeURI(lang);
    var kvp = window.location.search.substr(1).split('&');
    var i=kvp.length;
    var x;

    while(i--) {
      x = kvp[i].split('=');
      if (x[0] === key) {
        x[1] = value;
        kvp[i] = x.join('=');
        break;
      }
    }

    if (i<0) {
      kvp[kvp.length] = [key,value].join('=');
    }

    //this will reload the page, it's likely better to store this until finished
    window.location.search = kvp.join('&');
  };

  return {
    'stats': micaStats,
    'redirectError': micaRedirectError,
    'signin': micaSignin,
    'signout': micaSignout,
    'changeLanguage': micaChangeLanguage
  };

}());
