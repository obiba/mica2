/* exported micajs */
'use strict';

class LocalizedValues {
  static for(values, lang, keyLang, keyValue) {
    if (Array.isArray(values)) {
      let result = values.filter(function (item) {
        return item[keyLang] === lang;
      });

      if (result && result.length > 0) {
        return result[0][keyValue];
      } else {

        let langs = values.map(function(value) {
          return value[keyLang];
        });

        if (langs.length > 0) {
          return this.for(values, langs.length === 1 ? langs[0] : 'en', keyLang, keyValue);
        }
      }

    } else if (values) {
      return this.for(Object.keys(values).map(function(k) {
        return {lang: k, value: values[k]};
      }), lang, keyLang, keyValue);
    }

    return values || '';
  }

  static forLang(values, lang) {
    let rval = this.for(values, lang, 'lang', 'value');
    if (!rval || rval === '') {
      rval = this.for(values, 'und', 'lang', 'value');
    }
    if (!rval || rval === '') {
      rval = this.for(values, 'en', 'lang', 'value');
    }
    return rval;
  }

  static forLocale(values, lang) {
    let rval = this.for(values, lang, 'locale', 'text');
    if (!rval || rval === '') {
      rval = this.for(values, 'und', 'locale', 'text');
    }
    if (!rval || rval === '') {
      rval = this.for(values, 'en', 'locale', 'text');
    }
    return rval;
  }

  static extractLabel(attributes, lang) {
    let labelAttr = attributes ? attributes.filter(attr => attr.name === 'label').pop() : undefined;
    let label;
    if (labelAttr) {
      label = this.forLang(labelAttr.values, lang);
    }
    return label ? label : '';
  }
}

/**
 * A local storage that supports data types.
 */
class MicaLocalStorage {
  /**
   * MicaLocalStorage constructor
   */
  constructor (properties, namespace) {
    this._properties = properties ? properties : {};
    this._namespace = namespace ? namespace + '.' : '';
    this._isSupported = true;
  }

  /**
   * Namespace getter.
   *
   * @returns {string}
   */
  get namespace () {
    return this._namespace;
  }

  /**
   * Namespace setter.
   *
   * @param {string} value
   */
  set namespace (value) {
    this._namespace = value ? `${value}.` : '';
  }

  /**
   * Concatenates localStorage key with namespace prefix.
   *
   * @param {string} lsKey
   * @returns {string}
   * @private
   */
  _getLsKey (lsKey) {
    return `${this._namespace}${lsKey}`;
  }

  /**
   * Set a value to localStorage giving respect to the namespace.
   *
   * @param {string} lsKey
   * @param {*} rawValue
   * @param {*} type
   * @private
   */
  _lsSet (lsKey, rawValue, type) {
    const key = this._getLsKey(lsKey);
    const value = type && [Array, Object].includes(type)
      ? JSON.stringify(rawValue)
      : rawValue;

    window.localStorage.setItem(key, value);
  }

  /**
   * Get value from localStorage giving respect to the namespace.
   *
   * @param {string} lsKey
   * @returns {any}
   * @private
   */
  _lsGet (lsKey) {
    const key = this._getLsKey(lsKey);

    return window.localStorage[key];
  }

  /**
   * Get value from localStorage
   *
   * @param {String} lsKey
   * @param {*} defaultValue
   * @param {*} defaultType
   * @returns {*}
   */
  get (lsKey, defaultValue = null, defaultType = String) {
    if (!this._isSupported) {
      return null;
    }

    if (this._lsGet(lsKey)) {
      let type = defaultType;

      for (const key in this._properties) {
        if (key === lsKey) {
          type = this._properties[key].type;
          break;
        }
      }

      return this._process(type, this._lsGet(lsKey));
    }

    return defaultValue !== null ? defaultValue : null;
  }

  /**
   * Set localStorage value
   *
   * @param {String} lsKey
   * @param {*} value
   * @returns {*}
   */
  set (lsKey, value) {
    if (!this._isSupported) {
      return null;
    }

    for (const key in this._properties) {
      const type = this._properties[key].type;

      if ((key === lsKey)) {
        this._lsSet(lsKey, value, type);

        return value;
      }
    }

    this._lsSet(lsKey, value);

    return value;
  }

  /**
   * Remove value from localStorage
   *
   * @param {String} lsKey
   */
  remove (lsKey) {
    if (!this._isSupported) {
      return null;
    }

    return window.localStorage.removeItem(lsKey);
  }

  /**
   * Add new property to localStorage
   *
   * @param {String} key
   * @param {function} type
   * @param {*} defaultValue
   */
  addProperty (key, type, defaultValue = undefined) {
    type = type || String;

    this._properties[key] = { type };

    if (!this._lsGet(key) && defaultValue !== null) {
      this._lsSet(key, defaultValue, type);
    }
  }

  /**
   * Process the value before return it from localStorage
   *
   * @param {String} type
   * @param {*} value
   * @returns {*}
   * @private
   */
  _process (type, value) {
    switch (type) {
      case Boolean:
        return value === 'true';
      case Number:
        return parseFloat(value);
      case Array:
        try {
          const array = JSON.parse(value);

          return Array.isArray(array) ? array : [];
        } catch (e) {
          return [];
        }
      case Object:
        try {
          return JSON.parse(value);
        } catch (e) {
          return {};
        }
      default:
        return value;
    }
  }
}

/**
 * Storage of the variables selections.
 */
class MicaSetStorage extends MicaLocalStorage {
  constructor(ns) {
    super({
      //'variables': { type: Object },
      'selections': { type: Array }
    }, 'mica.' + ns);
  }

  /**
   * Get the selected IDs or empty array.
   *
   * @returns {*|*[]}
   */
  getSelections() {
    const selections = this.get('selections');
    return selections ? selections : [];
  }

  /**
   * Check if an ID is in the selection.
   *
   * @param id
   * @returns {*}
   */
  selected(id) {
    let selections = this.get('selections');
    return selections && selections.includes(id);
  }

  /**
   * Select an ID (ignore if present).
   *
   * @param id
   */
  select(id) {
    let selections = this.get('selections');
    if (!selections) {
      selections = [id];
    } else if (!selections.includes(id)) {
      selections.push(id);
    }
    this.set('selections', selections);
  }

  /**
   * Deselect an ID (ignore if not present).
   *
   * @param id
   */
  deselect(id) {
    let selections = this.get('selections');
    if (selections) {
      const idx = selections.indexOf(id);
      if (idx > -1) {
        selections.splice(idx, 1);
      }
    }
    this.set('selections', selections);
  }

  /**
   * Select all the IDs (ignore if provided IDs is empty).
   *
   * @param ids
   */
  selectAll(ids) {
    if (ids) {
      let selections = this.get('selections');
      if (!selections) {
        selections = ids;
      } else {
        ids.forEach(id => {
          if (!selections.includes(id)) {
            selections.push(id);
          }
        });
      }
      this.set('selections', selections);
    }
  }

  /**
   * Deselect all the IDs or clear the selection if none is provided.
   *
   * @param ids
   */
  deselectAll(ids) {
    if (ids) {
      let selections = this.get('selections');
      if (selections) {
        ids.forEach(id => {
          const idx = selections.indexOf(id);
          if (idx > -1) {
            selections.splice(idx, 1);
          }
        });
      }
      this.set('selections', selections);
    } else {
      this.set('selections', []);
    }
  }
}

const micajs = (function() {

  const normalizeUrl = function(url) {
    return contextPath + url;
  };

  // get the stats
  const micaStats = function(type, params, onSuccess, onFailure) {
    let url = '/ws/' + type + '/_rql';
    if (params && Object.keys(params).length>0) {
      let query = Object.keys(params).map(key => key + '=' + params[key]).join('&');
      url = url + '?' + query;
    }
    $.ajax({
      url: normalizeUrl(url),
      type: 'GET',
      dataType : 'json',
    })
      .done(function(json) {
        //console.log(json);
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

  const micaRedirectError = function(xhr, errorThrown) {
    $.redirect(normalizeUrl('/error'), {
      'status': xhr.status,
      'message': errorThrown
    }, 'POST');
  };

  const micaRedirect = function(path) {
    window.location.assign(path);
  };

  const signin = function(formId, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/auth/sessions';
      let data = form.serialize(); // serializes the form's elements.

      axios.post(normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          let redirect = normalizeUrl('/');
          const q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          }
          micaRedirect(redirect);
        })
        .catch(handle => {
          console.dir(handle);
          if (onFailure) {
            let banned = handle.response.data && handle.response.data.message === 'User is banned';
            onFailure(banned, handle.response.data);
          }
        });
    });
  };

  const signup = function(formId, requiredFields, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/users';
      let data = form.serialize(); // serializes the form's elements.

      let formData = form.serializeArray();

      const getField = function(name) {
        let fields = formData.filter(function(field) {
          return field.name === name;
        });
        return fields.length > 0 ? fields[0] : undefined;
      };

      if (requiredFields) {
        let missingFields = [];
        requiredFields.forEach(function(item) {
          let found = formData.filter(function(field) {
            return field.name === item.name && field.value;
          }).length;
          if (found === 0) {
             missingFields.push(item.title);
          }
        });
        if (missingFields.length>0) {
          onFailure(missingFields);
          return;
        }
      }
      const passwordField = getField('password');
      if (passwordField) {
        const password = passwordField.value;
        if (password.length < 8) {
          onFailure('server.error.password.too-short');
          return;
        }
      }

      const realmField = getField('realm');

      axios.post(normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          let redirect = normalizeUrl('/');
          let values = {};
          const q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          } else if (passwordField || realmField) {
            redirect = 'just-registered';
            values = { signin: true };
          } else {
            redirect = 'just-registered';
          }
          micaRedirect(redirect);
        })
        .catch(handle => {
          console.dir(handle);
          if (handle.response.data.message === 'Email already in use') {
            onFailure('server.error.email-already-assigned');
          } else if (handle.response.data.message === 'Invalid reCaptcha response') {
            onFailure('server.error.bad-captcha');
          }else if (handle.response.data.messageTemplate) {
            onFailure(handle.response.data.messageTemplate);
          } else {
            onFailure('server.error.bad-request');
          }
        });
    });
  };

  const signout = function(redirect) {
    $.ajax({
      type: 'DELETE',
      url: normalizeUrl('/ws/auth/session/_current')
    })
    .always(function() {
      micaRedirect(redirect || normalizeUrl('/'));
    });
  };

  const forgotPassword = function(formId, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/users/_forgot_password';
      let data = form.serialize(); // serializes the form's elements.

      if (decodeURI(data).trim() === 'username=') {
        return;
      }

      axios.post(normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          micaRedirect(normalizeUrl('/'));
        })
        .catch(handle => {
          console.dir(handle);
          onFailure();
        });
    });
  };

  const changeLanguage = function(lang) {
    let key = 'language';
    let value = encodeURI(lang);
    let kvp = window.location.search.substr(1).split('&');
    let i=kvp.length;
    let x;

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

  const micaInfo = function(text) {
    toastr.info(text);
  };
  const micaSuccess = function(text) {
    toastr.success(text);
  };
  const micaWarning = function(text) {
    toastr.warning(text);
  };
  const micaError = function(text) {
    toastr.error(text);
  };

  //
  // Data access
  //

  const createDataAccess = function(id, type) {
    let url = '/ws/data-access-requests/_empty';
    if (type && id) {
      if (type === 'amendment') {
        url = '/ws/data-access-request/' + id + '/amendments/_empty';
      } else {
        url = '/ws/data-access-request/' + id + '/feasibilities/_empty';
      }
    }
    axios.post(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (response.status === 201) {
          const tokens = response.headers.location.split('/');
          const createdId = tokens[tokens.length - 1];
          let redirect = '/data-access/' + createdId;
          if (type) {
            redirect = '/data-access-' + type + '-form/' + createdId;
          }
          micaRedirect(normalizeUrl(redirect));
        }
      })
      .catch(response => {
        console.dir(response);
        micaError('Creation failed.');
      });
  };

  const deleteDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id;
    let redirect = '/data-accesses';
    if (type && aId) {
      url = url + '/' + type + '/' + aId;
      redirect = '/data-access/' + id;
    }
    axios.delete(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Deletion failed.');
      });
  };

  const submitDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=SUBMITTED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=SUBMITTED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Submission failed.');
      });
  };

  const reopenDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=OPENED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=OPENED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Reopen failed.');
      });
  };

  const reviewDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=REVIEWED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=REVIEWED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Review failed.');
      });
  };

  const approveDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=APPROVED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=APPROVED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Approval failed.');
      });
  };

  const conditionallyApproveDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=CONDITIONALLY_APPROVED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=CONDITIONALLY_APPROVED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Conditional approval failed.');
      });
  };

  const rejectDataAccess = function(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=REJECTED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=REJECTED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Rejection failed.');
      });
  };

  const sendDataAccessComment = function(id, message, isPrivate) {
    let url = '/ws/data-access-request/' + id + '/comments';
    let redirect = '/data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '/data-access-private-comments/' + id;
    }
    axios({
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      url: normalizeUrl(url),
      data: message
    })
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Sending comment failed.');
      });
  };

  const deleteDataAccessComment = function(id, cid, isPrivate) {
    let url = '/ws/data-access-request/' + id + '/comment/' + cid;
    let redirect = '/data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '/data-access-private-comments/' + id;
    }
    axios.delete(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Deleting comment failed.');
      });
  };

  const addDataAccessAction = function(id, action) {
    //console.dir(action);
    let url = '/ws/data-access-request/' + id + '/_log-actions';
    let redirect = '/data-access-history/' + id;
    axios.post(normalizeUrl(url), action)
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Adding action failed.');
      });
  };

  const setDataAccessStartDate = function(id, startDate) {
    //console.log(startDate);
    let url = '/ws/data-access-request/' + id + '/_start-date?date=' + startDate;
    let redirect = '/data-access/' + id;
    axios.put(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('Setting start date failed.');
      });
  };

  const deleteDataAccessAttachment = function(id, fileId) {
    let url = '/ws/data-access-request/' + id + '/attachments/' + fileId;
    let redirect = '/data-access-documents/' + id;
    axios.delete(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('File deletion failed.');
      });
  };

  const uploadTempFile = function(file, onsuccess, onprogress) {
    let data = new FormData();
    data.append('file', file);
    let config = {
      onUploadProgress: function(progressEvent) {
        let percentCompleted = Math.round( (progressEvent.loaded * 100) / progressEvent.total );
        onprogress(percentCompleted);
      }
    };
    let url = '/ws/files/temp';
    axios.post(normalizeUrl(url), data, config)
      .then(response => {
        //console.dir(response);
        let fileId = response.headers.location.split('/').pop();
        onsuccess(fileId);
      })
      .catch(response => {
        console.dir(response);
        micaError('File upload failed.');
      });
  };

  const attachDataAccessFile = function(id, fileId) {
    let url = '/ws/data-access-request/' + id + '/attachments/' + fileId;
    let redirect = '/data-access-documents/' + id;
    axios.post(normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        micaRedirect(normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        micaError('File attachment failed.');
      });
  };

  //
  // Variable
  //

  /**
   * Get all variables sets, including the cart (set without a name).
   *
   * @param onsuccess
   * @param onfailure
   */
  const variablesSets = function(onsuccess, onfailure) {
    let url = '/ws/variables/sets';
    axios.get(normalizeUrl(url))
      .then(response => {
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

  /**
   * List the variables of a set.
   *
   * @param id
   * @param from
   * @param limit
   * @param onsuccess
   * @param onfailure
   */
  const variablesSetSearchDocuments = function(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/variables/_rql?query=variable(' +
        'in(Mica_variable.sets,' + id + '),' +
        'limit(' + from + ',' + limit + '),' +
        'fields(attributes.label.*,variableType,datasetId,datasetAcronym,attributes.Mlstr_area*),' +
        'sort(variableType,containerId,populationWeight,dataCollectionEventWeight,datasetId,index,name))';
    axios.get(normalizeUrl(url))
        .then(response => {
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

  /**
   * Delete all (no selection) or selected documents from the set.
   *
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  const variablesSetDeleteDocuments = function(id, selected, onsuccess, onfailure) {
    let url = '/ws/variables/set/' + id + '/documents';
    if (selected && selected.length>1) {
      url = url + '/_delete';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: normalizeUrl(url),
        data: selected.join('\n')
      })
          .then(response => {
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
    } else {
      axios.delete(normalizeUrl(url))
          .then(response => {
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
    }
  };

  /**
   * Get or create the variables cart.
   *
   * @param onsuccess
   * @param onfailure
   */
  const variablesCart = function(onsuccess, onfailure) {
    let url = '/ws/variables/sets/_cart';
    axios.get(normalizeUrl(url))
      .then(response => {
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

  /**
   * Add variable IDs to the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  const variablesCartAdd = function(ids, onsuccess, onfailure) {
    variablesCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_import';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: normalizeUrl(url),
        data: ids.join('\n')
      })
        .then(response => {
          //cartStorage.set('variables', response.data);
          if (onsuccess) {
            onsuccess(response.data, cart);
          }
        })
        .catch(response => {
          console.dir(response);
          if (onfailure) {
            onfailure(response);
          }
        });
    }, onfailure);
  };

  const variablesCartAddQuery = function(query, onsuccess, onfailure) {
    variablesCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_rql';
      axios({
        method: 'POST',
        headers: {
          'content-type': 'application/x-www-form-urlencoded'
        },
        url: normalizeUrl(url),
        data: 'query=' + query
      })
        .then(response => {
          //cartStorage.set('variables', response.data);
          if (onsuccess) {
            onsuccess(response.data, cart);
          }
        })
        .catch(response => {
          console.dir(response);
          if (onfailure) {
            onfailure(response);
          }
        });
    }, onfailure);
  };

  /**
   * Remove variable IDs from the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  const variablesCartRemove = function(ids, onsuccess, onfailure) {
    variablesCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_delete';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: normalizeUrl(url),
        data: ids.join('\n')
      })
        .then(response => {
          //cartStorage.set('variables', response.data);
          if (onsuccess) {
            onsuccess(response.data, cart);
          }
        })
        .catch(response => {
          console.dir(response);
          if (onfailure) {
            onfailure(response);
          }
        });
    }, onfailure);
  };

  const variablesCartShowCount = function(elem, cart, lang) {
    $(elem).text(cart.count > 0 ? (cart.count > 50 ? '50+' : cart.count) : '')
      .attr('title', cart.count > 50 ? cart.count.toLocaleString(lang) : '');
  };

  /**
   * Check whether a variable is in the set.
   * @param cart
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  const variablesSetContains = function(cart, id, onsuccess, onfailure) {
    let url = '/ws/variables/set/' + cart.id + '/document/' + id + '/_exists';
    axios.get(normalizeUrl(url))
      .then(response => {
        if (onsuccess) {
          onsuccess(response.data, cart);
        }
      })
      .catch(response => {
        if (onfailure) {
          onfailure(response);
        }
      });
  };

  const variableSummary = function(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/summary';
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  const variableAggregation = function(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/aggregation';
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  /**
   * Get the harmonized variables of a Dataschema variable.
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  const variableHarmonizations = function(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/harmonizations';
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  const datasetHarmonizedVariables = function(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/harmonized-dataset/' + id + '/variables/harmonizations/_summary?from=' + from + '&limit=' + limit;
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  const datasetContingency = function(type, id, var1, var2, onsuccess, onfailure) {
    let url = '/ws/' + type.toLowerCase() + '-dataset/' + id + '/variable/' + var1 + '/contingency?by=' + var2;
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  const datasetVariablesCoverage = function(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'variable(eq(datasetId,' + id + '),sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(datasetId))),locale(' + lang + ')';
    url = url + '?query=' + query;
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  //
  // Harmo
  //

  const variableAttributeValue = function(variable, namespace, name) {
    if (!variable || !variable.attributes) {
      return undefined;
    }
    for (const attr of variable.attributes) {
      if (attr.namespace === namespace && attr.name === name) {
        return attr.values;
      }
    }
    return undefined;
  };

  /**
   * Get the css class that represents the harmonization status.
   * @param status
   * @returns {string}
   */
  const harmoStatusClass = function(status) {
    let iconClass = 'fas fa-minus text-muted';
    if (status === 'complete') {
      iconClass = 'fas fa-check text-success';
    } else if (status === 'impossible') {
      iconClass = 'fas fa-times text-danger';
    } else if (status === 'undetermined') {
      iconClass = 'fas fa-question text-warning';
    }
    return iconClass;
  };

  const harmoStatus = function(variable) {
    return variableAttributeValue(variable, 'Mlstr_harmo', 'status');
  };

  const harmoStatusDetail = function(variable) {
    return variableAttributeValue(variable, 'Mlstr_harmo', 'status_detail');
  };

  const harmoComment = function(variable) {
    return variableAttributeValue(variable, 'Mlstr_harmo', 'comment');
  };

  //
  // Study
  //

  /**
   * Find population in study by ID
   * @param study
   */
  const studyPopulation = function(study, id) {
    if (study.populationSummaries) {
      for (const pop of study.populationSummaries) {
        if (pop.id === id) {
          return pop;
        }
      }
    }
    return undefined;
  };

  /**
   * Find DCE in study population by ID
   * @param population
   * @param id
   * @returns {undefined|any}
   */
  const studyPopulationDCE = function(population, id) {
    if (population.dataCollectionEventSummaries) {
      for (const dce of population.dataCollectionEventSummaries) {
        if (dce.id === id) {
          return dce;
        }
      }
    }
    return undefined;
  };

  const studyVariablesCoverage = function(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'variable(eq(studyId,' + id + '),sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(dceId))),locale(' + lang + ')';
    url = url + '?query=' + query;
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  //
  // Network
  //

  const networkVariablesCoverage = function(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'network(eq(Mica_network.id,' + id + ')),variable(sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(studyId))),locale(' + lang + ')';
    url = url + '?query=' + query;
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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

  //
  // Files
  //

  const getFolder = function(type, id, path, onsuccess, onfailure) {
    let url = '/ws/file/' + type + '/' + id + (path ? path : '/');
    axios.get(normalizeUrl(url))
      .then(response => {
        //console.dir(response);
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
    'normalizeUrl': normalizeUrl,
    'stats': micaStats,
    'redirectError': micaRedirectError,
    'redirect': micaRedirect,
    'signin': signin,
    'signup': signup,
    'signout': signout,
    'forgotPassword': forgotPassword,
    'changeLanguage': changeLanguage,
    'info': micaInfo,
    'success': micaSuccess,
    'warning': micaWarning,
    'error': micaError,
    'dataAccess': {
      'create': createDataAccess,
      'delete': deleteDataAccess,
      'submit': submitDataAccess,
      'reopen': reopenDataAccess,
      'review': reviewDataAccess,
      'approve': approveDataAccess,
      'conditionallyApprove': conditionallyApproveDataAccess,
      'reject': rejectDataAccess,
      'sendComment': sendDataAccessComment,
      'deleteComment': deleteDataAccessComment,
      'addAction': addDataAccessAction,
      'startDate': setDataAccessStartDate,
      'deleteAttachment': deleteDataAccessAttachment,
      'attachFile': attachDataAccessFile
    },
    'uploadTempFile': uploadTempFile,
    'variable': {
      'summary': variableSummary,
      'aggregation': variableAggregation,
      'harmonizations': variableHarmonizations,
      'attributeValue': variableAttributeValue,
      'cart': {
        'get': variablesCart,
        'add': variablesCartAdd,
        'addQuery': variablesCartAddQuery,
        'remove': variablesCartRemove,
        'contains': variablesSetContains,
        'showCount': variablesCartShowCount,
        'storage': new MicaSetStorage('cart')
      },
      'sets': variablesSets,
      'set': {
        'searchDocuments': variablesSetSearchDocuments,
        'deleteDocuments': variablesSetDeleteDocuments
      }
    },
    'dataset': {
      'harmonizedVariables': datasetHarmonizedVariables,
      'contingency': datasetContingency,
      'variablesCoverage': datasetVariablesCoverage
    },
    'harmo': {
      'status': harmoStatus,
      'statusDetail': harmoStatusDetail,
      'comment': harmoComment,
      'statusClass': harmoStatusClass
    },
    'study': {
      'population': studyPopulation,
      'populationDCE': studyPopulationDCE,
      'variablesCoverage': studyVariablesCoverage
    },
    'network': {
      'variablesCoverage': networkVariablesCoverage
    },
    'files': {
      'list': getFolder
    }
  };

}());
