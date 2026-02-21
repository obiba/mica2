'use strict';

/**
 * StringLocalizer - resolves localized string arrays from taxonomy/search data structures.
 * Handles both formats:
 *   - [{locale: "en", text: "..."}]  (taxonomy titles)
 *   - [{lang: "en", value: "..."}]   (entity labels)
 */
const StringLocalizer = {
  _locale: null,

  _getLocale() {
    if (!this._locale) {
      this._locale = (typeof Mica !== 'undefined' && Mica.locale) ? Mica.locale : 'en';
    }
    return this._locale;
  },

  /**
   * Localize an array of {locale, text} or {lang, value} objects.
   * Falls back to 'und', then 'en', then first entry.
   * Also handles plain strings.
   */
  localize(entries) {
    if (!entries) return '';
    if (typeof entries === 'string') return entries;

    const lang = this._getLocale();

    if (Array.isArray(entries)) {
      if (entries.length === 0) return '';

      // Detect format: taxonomy titles use {locale, text}, entity labels use {lang, value}
      const first = entries[0];
      const isLocaleText = 'locale' in first && 'text' in first;
      const langKey = isLocaleText ? 'locale' : 'lang';
      const valueKey = isLocaleText ? 'text' : 'value';

      const find = (l) => entries.find(e => e[langKey] === l);

      const match = find(lang) || find('und') || find('en') || entries[0];
      return match ? (match[valueKey] || '') : '';
    }

    return String(entries);
  }
};

/**
 * MicaFilters - central filter/utility object for Vue 3 migration.
 *
 * In Vue 2, filters like "translate" and "localizeString" were registered globally.
 * In Vue 3, filters are removed. This object provides:
 *   - Static methods callable from plain JS: MicaFilters.translate(key)
 *   - asMixin(): returns a Vue mixin that adds these as component methods,
 *     making them available in all templates as {{ translate("key") }}
 */
const MicaFilters = {
  /**
   * Translate a key using Mica.tr lookup table.
   * Returns the key itself if no translation is found.
   */
  translate(key) {
    if (typeof Mica !== 'undefined' && Mica.tr) {
      const value = Mica.tr[key];
      return typeof value === 'string' ? value : (key || '');
    }
    return key || '';
  },

  /**
   * Localize a taxonomy string array [{locale, text}] or [{lang, value}].
   */
  localizeString(entries) {
    return StringLocalizer.localize(entries);
  },

  /**
   * Taxonomy title resolver - may be overridden at runtime by mica-search.js
   * after taxonomies are loaded.
   * Signature: (input: "taxonomy.vocabulary.term") => string
   */
  taxonomyTitle(input) {
    return input || '';
  },

  /**
   * Truncate text to maxLength characters. If readMoreUrl is provided,
   * appends a "read more" link. If text is shorter than maxLength, returns as-is.
   */
  ellipsis(text, maxLength, readMoreUrl) {
    if (!text) return '';
    const str = String(text);
    if (!maxLength || str.length <= maxLength) return str;
    const truncated = str.substring(0, maxLength) + '...';
    if (readMoreUrl) {
      return truncated + ' <a href="' + readMoreUrl + '">' + (MicaFilters.translate('read-more') || 'Read more') + '</a>';
    }
    return truncated;
  },

  /**
   * Render text as HTML using marked.js (if available).
   */
  markdown(text) {
    if (!text) return '';
    if (typeof marked !== 'undefined') {
      return marked.parse(String(text));
    }
    return String(text);
  },

  /**
   * Concatenate two or more string arguments.
   */
  concat(...args) {
    return args.join('');
  },

  /**
   * Format a number using the page locale number formatter.
   * Falls back to Intl.NumberFormat if numberFormatter is not defined globally.
   */
  localizeNumber(n) {
    if (typeof numberFormatter !== 'undefined') {
      return numberFormatter.format(n);
    }
    return new Intl.NumberFormat().format(n);
  },

  /**
   * Returns a Vue mixin object that exposes filter functions as component methods.
   * Used via: Vue.mixin(MicaFilters.asMixin())
   */
  asMixin() {
    const self = this;
    return {
      methods: {
        translate(key) {
          return self.translate(key);
        },
        localizeString(entries) {
          return self.localizeString(entries);
        },
        taxonomyTitle(input) {
          return self.taxonomyTitle(input);
        },
        ellipsis(text, maxLength, readMoreUrl) {
          return self.ellipsis(text, maxLength, readMoreUrl);
        },
        markdown(text) {
          return self.markdown(text);
        },
        concat(...args) {
          return self.concat(...args);
        },
        localizeNumber(n) {
          return self.localizeNumber(n);
        }
      }
    };
  }
};
