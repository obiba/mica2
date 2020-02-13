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

  var micaRedirect = function(path) {
    $.redirect(path, null, 'GET');
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

  var micaSuccess = function(text) {
    toastr.success(text);
  };
  var micaWarning = function(text) {
    toastr.warning(text);
  };
  var micaError = function(text) {
    toastr.error(text);
  };

  //
  // Data access
  //

  var micaCreateDataAccess = function(id) {
    var url = '../ws/data-access-requests/_empty';
    if (id) {
      url = '../ws/data-access-request/' + id + '/amendments/_empty';
    }
    axios.post(url)
      .then(response => {
        console.dir(response);
        if (response.status === 201) {
          const tokens = response.headers.location.split('/');
          const createdId = tokens[tokens.length - 1];
          var redirect = '../data-access/' + createdId;
          if (id) {
            redirect = '../data-access-amendment-form/' + id + '?id=' + createdId;
          }
          micaRedirect(redirect);
        }
      })
      .catch(response => {
        console.dir(response);
        micaError('Creation failed.');
      });
  };

  var micaDeleteDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id;
    var redirect = '../../data-accesses';
    if (aId) {
      url = url + '/amendment/' + aId;
      redirect = '../../data-access/' + id;
    }
    axios.delete(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Deletion failed.');
      });
  };

  var micaSubmitDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id + '/_status?to=SUBMITTED';
    var redirect = '../data-access-form/' + id;
    if (aId) {
      url = '../../ws/data-access-request/' + id + '/amendment/' + aId + '/_status?to=SUBMITTED';
      redirect = '../data-access-amendment-form/' + id + '?id=' + aId;
    }
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Submission failed.');
      });
  };

  var micaReviewDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id + '/_status?to=REVIEWED';
    var redirect = '../data-access-form/' + id;
    if (aId) {
      url = '../../ws/data-access-request/' + id + '/amendment/' + aId + '/_status?to=REVIEWED';
      redirect = '../data-access-amendment-form/' + id + '?id=' + aId;
    }
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Review failed.');
      });
  };

  var micaApproveDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id + '/_status?to=APPROVED';
    var redirect = '../data-access-form/' + id;
    if (aId) {
      url = '../../ws/data-access-request/' + id + '/amendment/' + aId + '/_status?to=APPROVED';
      redirect = '../data-access-amendment-form/' + id + '?id=' + aId;
    }
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Approval failed.');
      });
  };

  var micaConditionallyApproveDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id + '/_status?to=CONDITIONALLY_APPROVED';
    var redirect = '../data-access-form/' + id;
    if (aId) {
      url = '../../ws/data-access-request/' + id + '/amendment/' + aId + '/_status?to=CONDITIONALLY_APPROVED';
      redirect = '../data-access-amendment-form/' + id + '?id=' + aId;
    }
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Conditional approval failed.');
      });
  };

  var micaRejectDataAccess = function(id, aId) {
    var url = '../../ws/data-access-request/' + id + '/_status?to=REJECTED';
    var redirect = '../data-access-form/' + id;
    if (aId) {
      url = '../../ws/data-access-request/' + id + '/amendment/' + aId + '/_status?to=REJECTED';
      redirect = '../data-access-amendment-form/' + id + '?id=' + aId;
    }
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Rejection failed.');
      });
  };

  var micaSendComment = function(id, message, isPrivate) {
    var url = '../../ws/data-access-request/' + id + '/comments';
    var redirect = '../data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '../data-access-private-comments/' + id;
    }
    axios({
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      url: url,
      data: message
    })
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Sending comment failed.');
      });
  };

  var micaDeleteComment = function(id, cid, isPrivate) {
    var url = '../../ws/data-access-request/' + id + '/comment/' + cid;
    var redirect = '../data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '../data-access-private-comments/' + id;
    }
    axios.delete(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Deleting comment failed.');
      });
  };

  return {
    'stats': micaStats,
    'redirectError': micaRedirectError,
    'redirect': micaRedirect,
    'signin': micaSignin,
    'signout': micaSignout,
    'changeLanguage': micaChangeLanguage,
    'success': micaSuccess,
    'warning': micaWarning,
    'error': micaError,
    'dataAccess': {
      'create': micaCreateDataAccess,
      'delete': micaDeleteDataAccess,
      'submit': micaSubmitDataAccess,
      'review': micaReviewDataAccess,
      'approve': micaApproveDataAccess,
      'conditionallyApprove': micaConditionallyApproveDataAccess,
      'reject': micaRejectDataAccess,
      'sendComment': micaSendComment,
      'deleteComment': micaDeleteComment
    }
  };

}());
