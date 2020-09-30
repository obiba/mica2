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

/**
 * General utility services.
 */
class MicaService {

  static normalizeUrl(url) {
    return contextPath + url;
  }

  static toastInfo(text) {
    toastr.info(text);
  }

  static toastSuccess(text) {
    toastr.success(text);
  }

  static toastWarning(text) {
    toastr.warning(text);
  }

  static toastError(text) {
    toastr.error(text);
  }

  static redirect(path) {
    window.location.assign(path);
  }

}

/**
 * Files service.
 */
class FilesService {

  /**
   * Get the document's folder content at path.
   *
   * @param type
   * @param id
   * @param path
   * @param onsuccess
   * @param onfailure
   */
  static getFolder(type, id, path, onsuccess, onfailure) {
    let url = '/ws/file/' + type + '/' + id + (path ? path : '/');
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        //console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  /**
   * Upload a file in the temporary location.
   *
   * @param file
   * @param onsuccess
   * @param onprogress
   */
  static uploadTempFile(file, onsuccess, onprogress) {
    let data = new FormData();
    data.append('file', file);
    let config = {
      onUploadProgress: function(progressEvent) {
        let percentCompleted = Math.round( (progressEvent.loaded * 100) / progressEvent.total );
        onprogress(percentCompleted);
      }
    };
    let url = '/ws/files/temp';
    axios.post(MicaService.normalizeUrl(url), data, config)
      .then(response => {
        //console.dir(response);
        let fileId = response.headers.location.split('/').pop();
        onsuccess(fileId);
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('File upload failed.');
      });
  }
}

/**
 * User management.
 */
class UserService {

  /**
   * Check and submit signin form.
   *
   * @param formId
   * @param onFailure
   */
  static signin(formId, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/auth/sessions';
      let data = form.serialize(); // serializes the form's elements.

      axios.post(MicaService.normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          let redirect = MicaService.normalizeUrl('/');
          const q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          }
          MicaService.redirect(redirect);
        })
        .catch(handle => {
          console.dir(handle);
          if (onFailure) {
            let banned = handle.response.data && handle.response.data.message === 'User is banned';
            onFailure(banned, handle.response.data);
          }
        });
    });
  }

  /**
   * Check and submit signup form.
   *
   * @param formId
   * @param requiredFields
   * @param onFailure
   */
  static signup(formId, requiredFields, onFailure) {
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
            return field.name === item.name && field.value && field.value.trim().length>0;
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

      axios.post(MicaService.normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          let redirect = MicaService.normalizeUrl('/');
          const q = new URLSearchParams(window.location.search);
          if (q.get('redirect')) {
            redirect = q.get('redirect');
          } else if (passwordField || realmField) {
            redirect = 'just-registered?signin=true';
          } else {
            redirect = 'just-registered';
          }
          MicaService.redirect(redirect);
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

  /**
   * Signout current user.
   *
   * @param redirect
   */
  static signout(redirect) {
    $.ajax({
      type: 'DELETE',
      url: MicaService.normalizeUrl('/ws/auth/session/_current')
    })
      .always(function() {
        MicaService.redirect(redirect || MicaService.normalizeUrl('/'));
      });
  }

  /**
   * Check and submit forgot password form.
   *
   * @param formId
   * @param onFailure
   */
  static forgotPassword(formId, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/users/_forgot_password';
      let data = form.serialize(); // serializes the form's elements.

      if (decodeURI(data).trim() === 'username=') {
        return;
      }

      axios.post(MicaService.normalizeUrl(url), data)
        .then(() => {
          //console.dir(response);
          MicaService.redirect(MicaService.normalizeUrl('/'));
        })
        .catch(handle => {
          console.dir(handle);
          onFailure();
        });
    });
  }

  /**
   * Switch page language.
   *
   * @param lang
   */
  static changeLanguage(lang) {
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

  /**
   * Check and submit contact request form.
   *
   * @param formId
   * @param requiredFields
   * @param onSuccess
   * @param onFailure
   */
  static contact(formId, requiredFields, onSuccess, onFailure) {
    $(formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/users/_contact';
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
          $('#contact-' + item.name).removeClass('is-invalid');
          let found = formData.filter(function(field) {
            return field.name === item.name && field.value && field.value.trim().length>0;
          }).length;
          if (found === 0) {
            missingFields.push(item);
          }
        });
        if (missingFields.length>0) {
          onFailure(missingFields);
          return;
        }
      }

      axios.post(MicaService.normalizeUrl(url), data)
        .then(() => {
          onSuccess();
        })
        .catch(handle => {
          console.dir(handle);
          if (handle.response.data.message === 'Invalid reCaptcha response') {
            onFailure('server.error.bad-captcha');
          }else if (handle.response.data.messageTemplate) {
            onFailure(handle.response.data.messageTemplate);
          } else {
            onFailure('server.error.bad-request');
          }
        });
    });
  }

}

/**
 * Variables set utils.
 */
class VariablesSetService {

  /**
   * Get all variables sets, including the cart (set without a name).
   *
   * @param onsuccess
   * @param onfailure
   */
  static getSets(onsuccess, onfailure) {
    let url = '/ws/variables/sets';
    axios.get(MicaService.normalizeUrl(url))
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

  /**
   * List the variables of a set.
   *
   * @param id
   * @param from
   * @param limit
   * @param onsuccess
   * @param onfailure
   */
  static search(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/variables/_rql?query=variable(' +
      'in(Mica_variable.sets,' + id + '),' +
      'limit(' + from + ',' + limit + '),' +
      'fields(attributes.label.*,variableType,datasetId,datasetAcronym,attributes.Mlstr_area*),' +
      'sort(variableType,containerId,populationWeight,dataCollectionEventWeight,datasetId,index,name))';
    axios.get(MicaService.normalizeUrl(url))
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

  /**
   * Delete all (no selection) or selected documents from the set.
   *
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  static deleteVariables(id, selected, onsuccess, onfailure) {
    let url = '/ws/variables/set/' + id + '/documents';
    if (selected && selected.length>1) {
      url = url + '/_delete';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
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
      axios.delete(MicaService.normalizeUrl(url))
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
  }

  /**
   * Get or create the variables cart.
   *
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateCart(onsuccess, onfailure) {
    let url = '/ws/variables/sets/_cart';
    axios.get(MicaService.normalizeUrl(url))
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

  /**
   * Add variable IDs to the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static addToCart(ids, onsuccess, onfailure) {
    this.getOrCreateCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_import';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
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
  }

  static addQueryToCart(query, onsuccess, onfailure) {
    this.getOrCreateCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_rql';
      axios({
        method: 'POST',
        headers: {
          'content-type': 'application/x-www-form-urlencoded'
        },
        url: MicaService.normalizeUrl(url),
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
  }

  /**
   * Remove variable IDs from the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static removeFromCart(ids, onsuccess, onfailure) {
    this.getOrCreateCart(function(cart) {
      let url = '/ws/variables/set/' + cart.id + '/documents/_delete';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
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
  }

  static showCount(elem, set, lang) {
    $(elem).text(set.count > 0 ? (set.count > 50 ? '50+' : set.count) : '')
      .attr('title', set.count > 50 ? set.count.toLocaleString(lang) : '');
  }

  /**
   * Check whether a variable is in the set.
   * @param set
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static contains = function(set, id, onsuccess, onfailure) {
    let url = '/ws/variables/set/' + set.id + '/document/' + id + '/_exists';
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        if (onsuccess) {
          onsuccess(response.data, set);
        }
      })
      .catch(response => {
        if (onfailure) {
          onfailure(response);
        }
      });
  };

}
