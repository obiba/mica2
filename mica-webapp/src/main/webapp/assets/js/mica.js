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

const BUTTON_PREVIOUS = 'button-previous';
const BUTTON_FIRST = 'button-first';
const BUTTON_LAST = 'button-last';
const BUTTON_NEXT = 'button-next';
const BUTTON_PAGE = 'button-page';
const BUTTON_ELLIPSIS_FIRST = 'button-ellipsis-first';
const BUTTON_ELLIPSIS_LAST = 'button-ellipsis-last';

const DEFAULT_PAGE_SIZES = [10,20,50,100];
const DEFAULT_PAGE_SIZE = DEFAULT_PAGE_SIZES[2];

/**
 * Simple widget for content pagination.
 * Client code must have a '<nav id="obiba-pagination"><ul class="pagination"></ul></nav>' element with an ID that is
 * passed to this widget for proper rendering.
 */
class OBiBaPagination {

  constructor(elementId, useFixedFirstLast, onPageChangeCB) {
    this.elementId = elementId;
    this.page = 1;
    this.useFixedFirstLast = useFixedFirstLast;
    this.onPageChangeCB = onPageChangeCB ? onPageChangeCB : () => ({});
    this.numberOfPages = 0; // Fields initialized in OBiBaPagination::update()
    // TODO make 'numberOfButtons' configurable
    this.numberOfButtons = 3; // # of buttons showing the page number excluding (<<, <, ..., >, >>)
    this.pageButtons = [1,2,3];  // used to control the DEFAULT_BUTTONS
    this.hasFirstLastButtons = false;
  }

  __cleanup() {
    // Most browsers is supposed to remove EventListeners as well
    document.querySelectorAll(`#${this.elementId} ul [id^="button-"]`).forEach(button => {
      switch (button.id) {
        case BUTTON_FIRST:
          button.removeEventListener('click', this.__onFirstClick);
          break;
        case BUTTON_PREVIOUS:
          button.removeEventListener('click', this.__onPreviousClick);
          break;
        case BUTTON_NEXT:
          button.removeEventListener('click', this.__onFirstClick);
          break;
        case BUTTON_ELLIPSIS_FIRST:
        case BUTTON_ELLIPSIS_LAST:
          break;
        case BUTTON_LAST:
          button.removeEventListener('click', this.__onLastClick);
          break;
        default:
          button.removeEventListener('click', this.__onPageClick);
          break;
      }

      button.closest('li').remove();
    });
  }

  __initializeSelection(start, count) {
    return Array(count).fill().map((_, idx) => start + idx);
  }

  __updateSelection(start, count) {
    if (this.numberOfPages > this.numberOfButtons ) {
      this.pageButtons = this.__initializeSelection(start, count);
    }
  }

  __createButton(parent, id, data, label, clickHandler) {
    parent.insertAdjacentHTML(
      'beforeend',
      `<li class="page-item"><a id="${id}" class="page-link" href="javascript:void(0)">${label}</a></li>`
    );

    const button = document.querySelector(`#${this.elementId} #${id}`);

    if (data && button) {
      button.dataset.page = data;
    }

    if (clickHandler && button) {
      button.addEventListener('click', clickHandler);
    }
  }

  __addClass(id, clazz) {
    const button = document.querySelector(`#${this.elementId} #${id}`);
    if (button) button.closest('li').classList.add(clazz);
  }

  __removeClass(id, clazz) {
    const button = document.querySelector(`#${this.elementId} #${id}`);
    if (button) button.closest('li').classList.remove(clazz);
  }

  __enableButton(id, enable) {
    if (enable) {
      this.__removeClass(id, 'disabled');
    } else {
      this.__addClass(id, 'disabled')
    }
  }

  __visible(id, show) {
    if (show) {
      this.__removeClass(id,'d-none');
    } else {
      this.__addClass(id,'d-none');
    }
  }

  __deactivateButton(id) {
    this.__removeClass(id, 'active');
  }

  __activateButton(id) {
    this.__addClass(id, 'active');
  }

  __createButtons() {
    let parent = document.querySelector(`#${this.elementId} ul`);

    if (parent) {
      // Previous button
      this.__createButton(parent, BUTTON_PREVIOUS, null, this.useFixedFirstLast ? '&lsaquo;' : 'Previous', this.__onPreviousClick.bind(this));
      this.__enableButton(BUTTON_PREVIOUS, false);

      // create first Ellipsis button
      this.__createButton(parent, BUTTON_ELLIPSIS_FIRST, null, '...', this.__onFirstEllipsisClick.bind(this));
      this.__enableButton(BUTTON_ELLIPSIS_FIRST, false);
      this.__visible(BUTTON_ELLIPSIS_FIRST, false);

      // Page buttons
      this.pageButtons.forEach((p, index) => {
        this.__createButton(parent,  `${BUTTON_PAGE}-${p}`, p, `${p}`, this.__onPageClick.bind(this));
        if (index === 0) {
          this.__activateButton(`${BUTTON_PAGE}-${p}`);
        }
      });

      // create last Ellipsis button
      this.__createButton(parent, BUTTON_ELLIPSIS_LAST, null, '...', this.__onLastEllipsisClick.bind(this));
      this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages > this.numberOfButtons);

      // Next button
      this.__createButton(parent, BUTTON_NEXT, null, this.useFixedFirstLast ? '&rsaquo;' : 'Next', this.__onNextClick.bind(this));

      // First/Last
      if (this.useFixedFirstLast) {
        this.__createFixedFirstLastButtons();

      } else {
        this.__createFirstLastButtons();
      }
    }
  }

  __createFixedFirstLastButtons() {
    let button = document.querySelector(`#${this.elementId} #${BUTTON_PREVIOUS}`);

    if (button) {
      button.closest('li').insertAdjacentHTML(
        'beforebegin',
        `<li class="page-item"><a id="${BUTTON_FIRST}" class="page-link" href="javascript:void(0)">&laquo;</a></li>`
      );

      button = document.querySelector(`#${this.elementId} #${BUTTON_FIRST}`);
      button.dataset.page = 1;
      this.__visible(BUTTON_FIRST, true);
      button.addEventListener('click', this.__onFirstClick.bind(this));

      button = document.querySelector(`#${this.elementId} #${BUTTON_NEXT}`);
      button.closest('li').insertAdjacentHTML(
        'afterend',
        `<li class="page-item"><a id="${BUTTON_LAST}" class="page-link" href="javascript:void(0)">&raquo;</a></li>`
      );
      button = document.querySelector(`#${this.elementId} #${BUTTON_LAST}`);
      button.dataset.page = this.numberOfPages;
      this.__visible(BUTTON_LAST, true);
      button.addEventListener('click', this.__onLastClick.bind(this));
    }
  }

  __createFirstLastButtons() {
    let button = document.querySelector(`#${this.elementId} #${BUTTON_PREVIOUS}`);

    if (button) {
      button.closest('li').insertAdjacentHTML(
        'afterend',
        `<li class="page-item"><a id="${BUTTON_FIRST}" class="page-link" href="javascript:void(0)">1</a></li>`
      );

      button = document.querySelector(`#${this.elementId} #${BUTTON_FIRST}`);
      button.dataset.page = 1;
      this.__visible(BUTTON_FIRST, false);
      button.addEventListener('click', this.__onFirstClick.bind(this));

      button = document.querySelector(`#${this.elementId} #${BUTTON_NEXT}`);
      button.closest('li').insertAdjacentHTML(
        'beforebegin',
        `<li class="page-item"><a id="${BUTTON_LAST}" class="page-link" href="javascript:void(0)">${this.numberOfPages}</a></li>`
      );
      button = document.querySelector(`#${this.elementId} #${BUTTON_LAST}`);
      button.dataset.page = this.numberOfPages;
      this.__visible(BUTTON_LAST, false);
      button.addEventListener('click', this.__onLastClick.bind(this));
    }
  }

  __updateVisibility() {
    const pagination = document.querySelector(`#${this.elementId}`);

    if (pagination) {
      if (this.numberOfPages <= 1) {
        pagination.classList.add('d-none');
      } else {
        pagination.classList.remove('d-none');
      }
    }
  }

  __updateButtons() {
    const anchors = document.querySelectorAll(`#${this.elementId} ul [id^="${BUTTON_PAGE}"]`);
    anchors.forEach((a, index) => {
      this.__deactivateButton(a.id);
      a.dataset.page = this.pageButtons[index];
      a.dataset.page = this.pageButtons[index];
      a.innerHTML = a.dataset.page;
    });

    const selected = this.pageButtons.indexOf(this.page);
    if (selected > -1) {
      this.__activateButton(anchors[selected].id);
    }
  }

  __doPageChange(page) {
    const isFirstPage = page === 1;
    const isFirstSelection = this.pageButtons.indexOf(page) === 0;
    const isLastSelection = this.pageButtons.indexOf(page) === this.numberOfButtons - 1;
    const isLastPage = page === this.numberOfPages;
    this.page = page;

    this.__enableButton(BUTTON_PREVIOUS, this.numberOfPages  > 1);
    this.__enableButton(BUTTON_NEXT, this.numberOfPages  > 1);

    if (isFirstPage) {
      this.__enableButton(BUTTON_PREVIOUS,false);
      this.__updateSelection(1, this.numberOfButtons);
    } else if (isLastPage) {
      this.__enableButton(BUTTON_NEXT, false);
      this.__updateSelection(this.numberOfPages-2, this.numberOfPages);
    } else if (isLastSelection || isFirstSelection) {
      this.__updateSelection(page-1, this.numberOfButtons);
    }

    if (this.numberOfPages > this.numberOfButtons) {
      this.__visible(BUTTON_ELLIPSIS_FIRST, page > 2);
      this.__enableButton(BUTTON_ELLIPSIS_FIRST, page > 2);
      this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages - page > 1)
      this.__enableButton(BUTTON_ELLIPSIS_LAST, this.numberOfPages - page > 1);
    }

    this.__updateFirstLastButtons(page);
    this.__updateButtons();
  }

  __doPageChangeAndCallback(page) {
    this.__doPageChange(page);

    if (this.onPageChangeCB) {
      this.onPageChangeCB({
        id: this.elementId,
        size: this.pageSize,
        total: this.total,
        from: (this.page - 1) * this.pageSize,
        to: (this.page) * this.pageSize
      });
    }
  }

  __updateFirstLastButtons(page) {
    if (this.hasFirstLastButtons) {
      this.__visible(BUTTON_FIRST, this.pageButtons[0] > 2 || page > 2);
      this.__visible(BUTTON_ELLIPSIS_FIRST, this.pageButtons[0] > 2);
      this.__visible(BUTTON_LAST, this.numberOfPages - this.pageButtons[2] > 2 || this.numberOfPages - page > 1);
      this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages - this.pageButtons[2] > 1);
    } else if (this.useFixedFirstLast) {
      this.__enableButton(BUTTON_LAST, page < this.numberOfPages);
      this.__enableButton(BUTTON_FIRST, page > 1);
    }
  }

  __onPageClick(event) {
    const page = parseInt(event.target.dataset.page);
    this.__doPageChangeAndCallback(page);
  }

  __onPreviousClick(event) {
    this.__doPageChangeAndCallback(this.page-1);
  }

  __onFirstEllipsisClick(event) {
    this.__doPageChangeAndCallback(this.pageButtons[0]-1);
  }

  __onLastEllipsisClick(event) {
    this.__doPageChangeAndCallback(this.pageButtons[this.numberOfButtons-1]+1);
  }

  __onFirstClick(event) {
    this.__doPageChangeAndCallback(1);
  }

  __onLastClick(event) {
    this.__doPageChangeAndCallback(this.numberOfPages);
  }

  __onNextClick(event) {
    this.__doPageChangeAndCallback(this.page+1);
  }

  __normalizePage(page) {
    let normalizedPage = page;
    if (page < 1) normalizedPage = 1;
    else if (page > this.numberOfPages) normalizedPage = this.numberOfPages;

    if (this.pageButtons.indexOf(normalizedPage) < 0) {
      // update selection before changing page
      this.__updateSelection(normalizedPage-2,normalizedPage);
    }

    return normalizedPage;
  }

  update(total, pageSize, page) {
    this.__cleanup();
    this.total = total;
    this.pageSize = pageSize;
    this.numberOfPages = Math.ceil(this.total / this.pageSize);
    this.numberOfButtons = Math.min(this.numberOfPages, 3);
    this.pageButtons = this.__initializeSelection(1,this.numberOfButtons);
    this.hasFirstLastButtons = !this.useFixedFirstLast && 2 < this.numberOfPages - this.numberOfButtons; // creates first/last button
    this.__createButtons();
    this.__doPageChange(this.__normalizePage(page));
    this.__updateVisibility();
  }

  gotoPage(page) {
    this.__doPageChange(this.__normalizePage(page));
  }

  gotoFrom(from) {
    if (from % this.pageSize !== 0) {
      throw new Error(`Invalid from value: ${from}. The valid value must be divisible by ${this.pageSize}.`);
    }

    this.gotoPage((from/this.pageSize)+1)
  }

  changePageSize(pageSize) {
    this.pageSize = pageSize;
    this.__cleanup();
    this.update(this.total, this.pageSize, this.page);
  }

  toString() {
    return `
      Total: ${this.total}
      Number of Pages: ${this.numberOfPages}
      Number of Buttons: ${this.numberOfPages}
      Page Size: ${this.pageSize}
      Page: ${this.page}
      Selections: ${this.pageButtons}
    `;
  }

}

/**
 * Simple widget used for selecting pagination page size.
 * Client code must have a '<select>' element with an ID that is passed to this widget for proper rendering.
 */
class OBiBaPageSizeSelector {
  constructor(elementId, pageSizes, pageSize, pageSizeChangeCB) {
    this.elementId = elementId;
    this.pageSizes = pageSizes || [10, 20, 50, 100];
    this.pageSize = pageSizes.indexOf(pageSize) > 0 ? pageSize : pageSizes[0];
    this.pageSizeChangeCB = pageSizeChangeCB;
    this.__createOptions();
  }

  __createOptions() {
    const parent = document.querySelector(`select#${this.elementId}`);

    if (parent) {
      parent.addEventListener('change', this.__onPageSizeChanged.bind(this));

      this.pageSizes.forEach(pageSize => {
        parent.insertAdjacentHTML('beforeend', `<option id="PAGE-SIZE-${pageSize}" value="${pageSize}">${pageSize}</option>`);
      });

      parent.value = this.pageSize;
    }
  }

  __onPageSizeChanged(event) {
    if (this.pageSizeChangeCB) {
      this.pageSizeChangeCB({id: this.elementId, size: parseInt(event.target.value)});
    }
  }

  update(pageSize) {
    if (this.pageSizes.indexOf(pageSize) < 0) {
      throw new Error("Invalid pageSize");
    }

    const parent = document.querySelector(`select#${this.elementId}`);
    if (parent) {
      this.pageSize = pageSize;
      parent.value = this.pageSize;
    }
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
    this.deselectAll()
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
    if (path && path.startsWith('http')) {
      window.location.assign(this.normalizeUrl('/check?redirect=') + path);
    } else if (path) {
      window.location.assign(path);
    } else {
      $.redirect(this.normalizeUrl('/'), {}, 'GET');
    }
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
   * @param otpId
   * @param onFailure
   */
  static signin(formId, otpId, onFailure) {
    const toggleSubmitButton = function(enable)  {
      const submitSelect = '#' + formId + ' button[type="submit"]';
      if (enable) {
        $(submitSelect).prop("disabled",false);
        $( submitSelect + ' i').hide();
      } else {
        $(submitSelect).prop("disabled",true);
        $( submitSelect + ' i').show();
      }
    };
    $('#' + formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = $(this);
      let url = '/ws/auth/sessions';
      let data = form.serialize(); // serializes the form's elements.

      toggleSubmitButton(false);
      const config = {}
      const otp = $('#' + otpId).val();
      if (otp) {
        config.headers = {
          'X-Obiba-TOTP': otp
        }
      }
      axios.post(MicaService.normalizeUrl(url), data, config)
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
          toggleSubmitButton(true);
          //console.dir(handle);
          if (onFailure) {
            let banned = handle.response.data && handle.response.data.message === 'User is banned';
            onFailure(handle.response, banned);
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
    const toggleSubmitButton = function(enable)  {
      const submitSelect = '#' + formId + ' button[type="submit"]';
      if (enable) {
        $(submitSelect).prop("disabled",false);
        $( submitSelect + ' i').hide();
      } else {
        $(submitSelect).prop("disabled",true);
        $( submitSelect + ' i').show();
      }
    };
    $('#' + formId).submit(function(e) {
      e.preventDefault(); // avoid to execute the actual submit of the form.
      let form = document.querySelector('#' + formId);
      let url = '/ws/users';
      const formData = new FormData(form);
      const json = Object.fromEntries(formData.entries());

      if (requiredFields) {
        let missingFields = [];
        requiredFields.forEach(function(item) {
          let found = json[item.name];
          if (!found) {
            missingFields.push(item.title);
          }
        });
        if (missingFields.length>0) {
          onFailure(missingFields);
          return;
        }
      }
      const passwordField = json.password;
      if (passwordField) {
        if (passwordField.length < 8) {
          onFailure('server.error.password.too-short');
          return;
        }
        if (passwordField.length > 64) {
          onFailure('server.error.password.too-long');
          return;
        }
      }

      const realmField = json.realm;

      toggleSubmitButton(false);
      axios.post(MicaService.normalizeUrl(url), json)
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
          toggleSubmitButton(true);
          if (handle.response.data?.message?.startsWith('Email already in use')) {
            onFailure('server.error.email-already-assigned');
          } else if (handle.response.data?.message === 'Invalid reCaptcha response') {
            onFailure('server.error.bad-captcha');
          }else if (handle.response.data?.messageTemplate) {
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
 * Generic document set service.
 */
class SetService {

  /**
   * Get all documents sets, including the cart (set without a name).
   * @param type
   * @param onsuccess
   * @param onfailure
   */
  static getSets(type, onsuccess, onfailure) {
    let url = '/ws/' + type + '/sets';
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
   * Add document IDs to the cart (verifies that it exists and is valid).
   * @param type
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static addToCart(type, ids, onsuccess, onfailure) {
    this.getOrCreateCart(type,function(cart) {
      let url = '/ws/' + type + '/set/' + cart.id + '/documents/_import';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
        data: ids.join('\n')
      })
        .then(response => {
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

  static addToSet(type, setId, name, ids, onsuccess, onfailure) {
    this.getOrCreateSet(type, setId, name, function(set) {
      let url = '/ws/' + type + '/set/' + set.id + '/documents/_import';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
        data: ids.join('\n')
      })
        .then(response => {
          //cartStorage.set('variables', response.data);
          if (onsuccess) {
            onsuccess(response.data, set);
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

  static addQueryToCart(type, query, onsuccess, onfailure) {
    this.getOrCreateCart(type,function(cart) {
      let url = '/ws/' + type + '/set/' + cart.id + '/documents/_rql';
      axios({
        method: 'POST',
        headers: {
          'content-type': 'application/x-www-form-urlencoded'
        },
        url: MicaService.normalizeUrl(url),
        data: 'query=' + query
      })
        .then(response => {
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

  static addQueryToSet(type, setId, name, query, onsuccess, onfailure) {
    this.getOrCreateSet(type, setId, name, function(set) {
      let url = '/ws/' + type + '/set/' + set.id + '/documents/_rql';
      axios({
        method: 'POST',
        headers: {
          'content-type': 'application/x-www-form-urlencoded'
        },
        url: MicaService.normalizeUrl(url),
        data: 'query=' + query
      })
        .then(response => {
          if (onsuccess) {
            onsuccess(response.data, set);
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
   * Remove document IDs from the cart (verifies that it exists and is valid).
   * @param type
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static removeFromCart(type, ids, onsuccess, onfailure) {
    this.getOrCreateCart(type,function(cart) {
      let url = '/ws/' + type + '/set/' + cart.id + '/documents/_delete';
      axios({
        method: 'POST',
        headers: { 'content-type': 'text/plain' },
        url: MicaService.normalizeUrl(url),
        data: ids.join('\n')
      })
        .then(response => {
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

  static deleteSet(type, id, onsuccess, onfailure) {
    let url = '/ws/' + type + '/set/' + id;

    axios({
      method: 'DELETE',
      headers: { 'content-type': 'text/plain' },
      url: MicaService.normalizeUrl(url)
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
  }

  /**
   * Show the count of documents in the cart (or an estimate).
   * @param elem
   * @param set
   * @param lang
   */
  static showCount(elem, set, lang) {
    if (set) {
      Carts.filter(c => c.id === set.id).forEach(c => c.count = set.count);
    }
    let count = Carts.map(c => c.count).reduce((prev, curr) => prev + curr, 0);
    $(elem).text(count > 0 ? (count > 50 ? '50+' : count) : '')
      .attr('title', count > 50 ? count.toLocaleString(lang) : '');
  }

  /**
   * Check whether a document is in the set.
   * @param type
   * @param set
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static contains(type, set, id, onsuccess, onfailure) {
    let url = '/ws/' + type + '/set/' + set.id + '/document/' + id + '/_exists';
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

  /**
   * Delete all (no selection) or selected documents from the set.
   * @param type
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  static deleteDocuments(type, id, selected, onsuccess, onfailure) {
    let url = '/ws/' + type + '/set/' + id + '/documents';
    if (selected && selected.length>0) {
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
  };

  /**
   * Get or create the documents cart.
   * @param type
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateCart(type, onsuccess, onfailure) {
    let url = '/ws/' + type + '/sets/_cart';
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
  };

  /**
   * Get or create a variable set.
   * @param type
   * @param setId
   * @param name
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateSet(type, setId, name, onsuccess, onfailure) {
    if (!setId) {
      let url = '/ws/' + type + '/sets?name=' + name;
      axios({
        method: 'POST',
        url: MicaService.normalizeUrl(url),
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
      let url = '/ws/' + type + '/set/' + setId;
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
  };

}
/**
 * Networks set utils.
 */
class NetworksSetService extends SetService {

  /**
   * Get all networks sets, including the cart (set without a name).
   *
   * @param onsuccess
   * @param onfailure
   */
  static getSets(onsuccess, onfailure) {
    SetService.getSets('networks', onsuccess, onfailure);
  };

  /**
   * Add networks IDs to the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static addToCart(ids, onsuccess, onfailure) {
    SetService.addToCart('networks', ids, onsuccess, onfailure);
  }

  static addQueryToCart(query, onsuccess, onfailure) {
    SetService.addQueryToCart('networks', query, onsuccess, onfailure);
  }

  /**
   * Remove networks IDs from the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static removeFromCart(ids, onsuccess, onfailure) {
    SetService.removeFromCart('networks', ids, onsuccess, onfailure);
  }

  /**
   * Check whether a network is in the set.
   * @param set
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static contains(set, id, onsuccess, onfailure) {
    SetService.contains('networks', set || {}, id, onsuccess, onfailure);
  };

  /**
   * Delete all (no selection) or selected documents from the set.
   *
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  static deleteNetworks(id, selected, onsuccess, onfailure) {
    SetService.deleteDocuments('networks', id, selected, onsuccess, onfailure);
  };

  /**
   * Get or create the networks cart.
   *
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateCart(onsuccess, onfailure) {
    SetService.getOrCreateCart('networks', onsuccess, onfailure);
  };

  /**
   * List the networks of a set.
   *
   * @param id
   * @param from
   * @param limit
   * @param onsuccess
   * @param onfailure
   */
  static search(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/networks/_rql?query=network(' +
      'in(Mica_network.sets,' + id + '),' +
      'limit(' + from + ',' + limit + '),' +
      'fields(acronym.*,name.*),' +
      'sort(acronym))';
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
}

/**
 * Studies set utils.
 */
class StudiesSetService extends SetService {

  /**
   * Get all studies sets, including the cart (set without a name).
   *
   * @param onsuccess
   * @param onfailure
   */
  static getSets(onsuccess, onfailure) {
    SetService.getSets('studies', onsuccess, onfailure);
  };

  /**
   * Add study IDs to the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static addToCart(ids, onsuccess, onfailure) {
    SetService.addToCart('studies', ids, onsuccess, onfailure);
  }

  static addQueryToCart(query, onsuccess, onfailure) {
    SetService.addQueryToCart('studies', query, onsuccess, onfailure);
  }

  /**
   * Remove study IDs from the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static removeFromCart(ids, onsuccess, onfailure) {
    SetService.removeFromCart('studies', ids, onsuccess, onfailure);
  }

  /**
   * Check whether a study is in the set.
   * @param set
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static contains(set, id, onsuccess, onfailure) {
    SetService.contains('studies', set || {}, id, onsuccess, onfailure);
  };

  /**
   * Delete all (no selection) or selected documents from the set.
   *
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  static deleteStudies(id, selected, onsuccess, onfailure) {
    SetService.deleteDocuments('studies', id, selected, onsuccess, onfailure);
  };

  /**
   * Get or create the studies cart.
   *
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateCart(onsuccess, onfailure) {
    SetService.getOrCreateCart('studies', onsuccess, onfailure);
  };

  /**
   * List the studies of a set.
   *
   * @param id
   * @param from
   * @param limit
   * @param onsuccess
   * @param onfailure
   */
  static search(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/studies/_rql?query=study(' +
      'in(Mica_study.sets,' + id + '),' +
      'limit(' + from + ',' + limit + '),' +
      'fields(acronym.*,name.*,model.methods.design,populations.dataCollectionEvents.model.dataSources,model.numberOfParticipants.participant),' +
      'sort(acronym))';
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
}

/**
 * Variables set utils.
 */
class VariablesSetService extends SetService {

  /**
   * Get all variables sets, including the cart (set without a name).
   *
   * @param onsuccess
   * @param onfailure
   */
  static getSets(onsuccess, onfailure) {
    SetService.getSets('variables', onsuccess, onfailure);
  };

  /**
   * Delete all (no selection) or selected documents from the set.
   *
   * @param id
   * @param selected
   * @param onsuccess
   * @param onfailure
   */
  static deleteVariables(id, selected, onsuccess, onfailure) {
    SetService.deleteDocuments('variables', id, selected, onsuccess, onfailure);
  }

  /**
   * Get or create the variables cart.
   *
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateCart(onsuccess, onfailure) {
    SetService.getOrCreateCart('variables', onsuccess, onfailure);
  }

  /**
   * Get or create a variable set.
   *
   * @param setId
   * @param name
   * @param onsuccess
   * @param onfailure
   */
  static getOrCreateSet(setId, name, onsuccess, onfailure) {
    SetService.getOrCreateSet('variables', setId, name, onsuccess, onfailure);
  }

  /**
   * Add variable IDs to the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static addToCart(ids, onsuccess, onfailure) {
    SetService.addToCart('variables', ids, onsuccess, onfailure);
  }

  static addToSet(setId, name, ids, onsuccess, onfailure) {
    SetService.addToSet('variables', setId, name, ids, onsuccess, onfailure);
  }

  static addQueryToCart(query, onsuccess, onfailure) {
    SetService.addQueryToCart('variables', query, onsuccess, onfailure);
  }

  static addQueryToSet(setId, name, query, onsuccess, onfailure) {
    SetService.addQueryToSet('variables', setId, name, query, onsuccess, onfailure);
  }

  /**
   * Remove variable IDs from the cart (verifies that it exists and is valid).
   * @param ids
   * @param onsuccess
   * @param onfailure
   */
  static removeFromCart(ids, onsuccess, onfailure) {
    SetService.removeFromCart('variables', ids, onsuccess, onfailure);
  }

  static deleteSet(id, onsuccess, onfailure) {
    SetService.deleteSet('variables', id, onsuccess, onfailure);
  }

  /**
   * Check whether a variable is in the set.
   * @param set
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static contains(set, id, onsuccess, onfailure) {
    SetService.contains('variables', set || {}, id, onsuccess, onfailure);
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
  static search(id, from, limit, onsuccess, onfailure) {
    let url = '/ws/variables/_rql?query=variable(' +
      'in(Mica_variable.sets,' + id + '),' +
      'limit(' + from + ',' + limit + '),' +
      'fields(attributes.label.*,variableType,datasetId,datasetAcronym,datasetName,attributes.Mlstr_area*),' +
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
   * Show the count of lists.
   * @param listElem
   */
  static showSetsCount(listElem, onVariablesSet) {
    VariablesSetService.getSets(data => {
      if (Array.isArray(data)) {
        const variableSets = data.filter(set => set.name);
        if (variableSets.length === 0) {
          listElem.hide();
        } else {
          listElem.show();
        }
        listElem.text(variableSets.length);
        if (onVariablesSet) {
          onVariablesSet(variableSets);
        }
      } else {
        listElem.hide();
      }
    }, response => {

    });
  }

}


/**
 * Helper class to perform sorting and decorating operations.
 */
class TaxonomyHelper {
  constructor() {}

  static newInstance() {
    return new TaxonomyHelper();
  }

  __vocabularyAttributeValue(vocabulary, key, defaultValue) {
    let value = defaultValue;
    if (vocabulary.attributes) {
      vocabulary.attributes.some(function (attribute) {
        if (attribute.key === key) {
          value = attribute.value;
          return true;
        }
        return false;
      });
    }

    return value;
  }

  sortVocabularyTerms(vocabulary, sortKey) {
    const terms = vocabulary.terms || [];
    const termsSortKey = sortKey || this.__vocabularyAttributeValue(vocabulary, 'termsSortKey', null);

    if (termsSortKey && terms.length > 0) {
      switch (termsSortKey) {
        case 'name':
          terms.sort(function (a, b) {
            return a[termsSortKey].localeCompare(b[termsSortKey]);
          });
          break;
        case 'title':
          terms.sort(function (a, b) {
            const titleA = StringLocalizer.localize(a[termsSortKey]);
            const titleB = StringLocalizer.localize(b[termsSortKey]);
            return titleA.localeCompare(titleB);
          });
          break;
      }
    }
  }

  sortVocabulariesTerms(taxonomy, sortKey) {
    (taxonomy.vocabularies || []).forEach(vocabulary => this.sortVocabularyTerms(vocabulary, sortKey));
  }
}

