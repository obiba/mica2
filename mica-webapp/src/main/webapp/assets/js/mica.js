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

      axios.post(url, data)
        .then(response => {
          console.dir(response);
          var redirect = '/';
          var q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          }
          $.redirect(redirect, {}, 'GET');
        })
        .catch(handle => {
          console.dir(handle);
          if (onFailure) {
            var banned = handle.response.data && handle.response.data.message === 'User is banned';
            onFailure(banned, handle.response.data);
          }
        });
    });
  };

  var micaSignup = function(formId, requiredFields, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      var form = $(this);
      var url = '../ws/users';
      var data = form.serialize(); // serializes the form's elements.

      var formData = form.serializeArray();

      if (requiredFields) {
        var missingFields = [];
        requiredFields.forEach(function(item) {
          var found = formData.filter(function(field) {
            return field.name === item.name && field.value;
          }).length;
          if (found === 0) {
             missingFields.push(item.title);
          }
        });
        if (missingFields.length>0) {
          if (onFailure) {
            onFailure(missingFields);
          } else {
            console.dir(missingFields);
          }
          return;
        }
      }

      if (onFailure) {
        var password = formData.filter(function(field) {
          return field.name === 'password';
        }).map(function(field) {
          return field.value;
        })[0];
        console.log(password);
        if (password.length < 8) {
          onFailure('server.error.password.too-short');
          return;
        }
      }

      axios.post(url, data)
        .then(response => {
          console.dir(response);
        })
        .catch(response => {
          console.dir(response);
          if (onFailure) {
            onFailure('Submission failed: ' + response.statusText + ' (' + response.status + ')');
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
      var redirect = pathPrefix + '/';
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
            redirect = '../data-access-amendment-form/' + createdId;
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
      redirect = '../data-access-amendment-form/' + aId;
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
      redirect = '../data-access-amendment-form/' + aId;
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
      redirect = '../data-access-amendment-form/' + aId;
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
      redirect = '../data-access-amendment-form/' + aId;
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
      redirect = '../data-access-amendment-form/' + aId;
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

  var micaAddAction = function(id, action) {
    console.dir(action);
    var url = '../../ws/data-access-request/' + id + '/_log-actions';
    var redirect = '../data-access-history/' + id;
    axios.post(url, action)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Adding action failed.');
      });
  };

  var micaStartDate = function(id, startDate) {
    console.log(startDate);
    var url = '../../ws/data-access-request/' + id + '/_start-date?date=' + startDate;
    var redirect = '../data-access/' + id;
    axios.put(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('Setting start date failed.');
      });
  };

  var micaDeleteAttachment = function(id, fileId) {
    var url = '../../ws/data-access-request/' + id + '/attachments/' + fileId;
    var redirect = '../data-access-documents/' + id;
    axios.delete(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('File deletion failed.');
      });
  };

  var micaUploadTempFile = function(file, onsuccess, onprogress) {
    var data = new FormData();
    data.append('file', file);
    var config = {
      onUploadProgress: function(progressEvent) {
        var percentCompleted = Math.round( (progressEvent.loaded * 100) / progressEvent.total );
        onprogress(percentCompleted);
      }
    };
    axios.post('../../ws/files/temp', data, config)
      .then(response => {
        console.dir(response);
        var fileId = response.headers.location.split('/').pop();
        onsuccess(fileId);
      })
      .catch(response => {
        console.dir(response);
        micaError('File upload failed.');
      });
  };

  var micaAttachFile = function(id, fileId) {
    var url = '../../ws/data-access-request/' + id + '/attachments/' + fileId;
    var redirect = '../data-access-documents/' + id;
    axios.post(url)
      .then(response => {
        console.dir(response);
        micaRedirect(redirect);
      })
      .catch(response => {
        console.dir(response);
        micaError('File attachment failed.');
      });
  };

  //
  // Variable
  //

  var micaVariableSummary = function(id, onsuccess, onfailure) {
    var url = '../ws/variable/' + id + '/summary';
    axios.get(url)
      .then(response => {
        console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  };

  return {
    'stats': micaStats,
    'redirectError': micaRedirectError,
    'redirect': micaRedirect,
    'signin': micaSignin,
    'signup': micaSignup,
    'signout': micaSignout,
    'changeLanguage': micaChangeLanguage,
    'success': micaSuccess,
    'warning': micaWarning,
    'error': micaError,
    'dataAccess': {
      'create': micaCreateDataAccess,
      'createAmendment': micaCreateDataAccess,
      'delete': micaDeleteDataAccess,
      'submit': micaSubmitDataAccess,
      'review': micaReviewDataAccess,
      'approve': micaApproveDataAccess,
      'conditionallyApprove': micaConditionallyApproveDataAccess,
      'reject': micaRejectDataAccess,
      'sendComment': micaSendComment,
      'deleteComment': micaDeleteComment,
      'addAction': micaAddAction,
      'startDate': micaStartDate,
      'deleteAttachment': micaDeleteAttachment,
      'attachFile': micaAttachFile
    },
    'uploadTempFile': micaUploadTempFile,
    'variable': {
      'summary': micaVariableSummary
    }
  };

}());
