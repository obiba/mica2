<!-- REQUIRED SCRIPTS -->

<!-- jQuery -->
<script src="${jQueryPath}/dist/jquery.min.js"></script>
<!-- Popper -->
<script src="${popperPath}/dist/umd/popper.min.js"></script>

<!-- Bootstrap 5 -->
<script src="${bootstrapPath}/dist/js/bootstrap.bundle.min.js"></script>
<!-- AdminLTE App -->
<script src="${adminLTEPath}/dist/js/adminlte.min.js"></script>
<!-- Moment -->
<script src="${momentPath}/min/moment-with-locales.min.js"></script>
<!-- DataTables -->
<script src="${datatablesPath}/js/jquery.dataTables.min.js"></script>
<script src="${datatablesBS5Path}/js/dataTables.bootstrap5.min.js"></script>
<script src="${datatablesFixedHeaderPath}/js/dataTables.fixedHeader.min.js"></script>
<script src="${datatablesFixedHeaderBS5Path}/js/fixedHeader.bootstrap5.min.js"></script>
<script src="${datatablesButtonsPath}/js/dataTables.buttons.min.js"></script>
<script src="${datatablesButtonsBS5Path}/js/buttons.bootstrap5.min.js"></script>
<script src="${datatablesButtonsPath}/js/buttons.html5.min.js"></script>
<script src="${datatablesButtonsPath}/js/buttons.print.min.js"></script>
<script src="${datatablesSelectPath}/js/dataTables.select.min.js"></script>
<!-- Toastr -->
<script src="${toastrPath}/build/toastr.min.js"></script>
<!-- Axios -->
<script src="${assetsPath}/libs/node_modules/axios/dist/axios.min.js"></script>
<!-- Marked -->
<script src="${assetsPath}/libs/node_modules/marked/marked.min.js"></script>
<!-- Mica Utils and dependencies -->
<script src="${assetsPath}/libs/node_modules/jquery.redirect/jquery.redirect.js"></script>
<script src="${assetsPath}/libs/node_modules/js-cookie/src/js.cookie.js"></script>
<script src="${assetsPath}/js/mica.js"></script>
<script src="${assetsPath}/js/mica-filters.js"></script>

<!-- Custom js -->
<#include "../models/scripts.ftl"/>

<!-- Global js variables -->
<script>
  const contextPath = "${contextPath}";
  <!-- Number formatting -->
  const numberFormatter = new Intl.NumberFormat('${.lang}');
  <!-- DataTable options -->
  let dataTablesDefaultOpts = {
    "paging": true,
    "pageLength": 25,
    "lengthChange": true,
    "searching": true,
    "ordering": true,
    "info": true,
    "autoWidth": true,
    "language": {
      "url": "${assetsPath}/i18n/datatables.${.lang}.json"
    }
  };
  let dataTablesSortSearchOpts = {
    "paging": false,
    "lengthChange": false,
    "searching": true,
    "ordering": true,
    "info": false,
    "autoWidth": true,
    "language": {
      "url": "${assetsPath}/i18n/datatables.${.lang}.json"
    }
  };
  let dataTablesSortOpts = {
    "paging": false,
    "lengthChange": false,
    "searching": false,
    "ordering": true,
    "info": false,
    "autoWidth": true,
    "language": {
      "url": "${assetsPath}/i18n/datatables.${.lang}.json"
    }
  };
  let dataTablesNoSortSearchOpts = {
    "paging": true,
    "pageLength": 25,
    "lengthChange": false,
    "searching": true,
    "ordering": false,
    "info": false,
    "autoWidth": true,
    "language": {
      "url": "${assetsPath}/i18n/datatables.${.lang}.json"
    }
  };
  <!-- Dto utility functions -->
  const localizedString = function(localizedObj) {
    if (localizedObj) {
      for (const obj of localizedObj) {
        if (obj.lang === '${.lang}') {
          return obj.value;
        }
      }
      for (const obj of localizedObj) {
        if (obj.lang === 'und') {
          return obj.value;
        }
      }
    }
    return '';
  };

  const escapeQuotes = function(str, quote) {
    const q = quote ? quote : '"';
    const regex = new RegExp(q, 'g');
    return str ? str.replace(regex, '\\' + q) : str;
  };

  <#if !user??>
  UserService.getCurrent();
  </#if>
</script>

<script>
  const Carts = [];

  <#if variablesCartEnabled && sets?? && sets.variablesCart??>
  Carts.push({
    id: '${sets.variablesCart.id}',
    type: 'variables',
    count: ${sets.variablesCart.count?c}
  });
  </#if>

  <#if studiesCartEnabled && sets?? && sets.studiesCart??>
  Carts.push({
    id: '${sets.studiesCart.id}',
    type: 'studies',
    count: ${sets.studiesCart.count?c}
  });
  </#if>

  <#if networksCartEnabled && sets?? && sets.networksCart??>
  Carts.push({
    id: '${sets.networksCart.id}',
    type: 'networks',
    count: ${sets.networksCart.count?c}
  });
  </#if>

  $(function () {
    /**
     * Uses browser to normalize input html (closing/removing tags)
     * @param html
     * @returns {*}
     */
    function tidy(html) {
      var d = document.createElement('div');
      d.innerHTML = html;
      return d.innerHTML;
    }

    // bs tooltip and popover initialization (Bootstrap 5 vanilla JS)
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
    const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl, {
      html: tooltipTriggerEl.getAttribute('data-bs-html') === 'true'
    }))

    const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]')
    const popoverList = [...popoverTriggerList].map(popoverTriggerEl => new bootstrap.Popover(popoverTriggerEl, { html: true }))
    // apply markdown rendering
    $('.marked').each(function () {
      const template = $(this).find('template');
      var msg = template && template.length > 0 ? $.trim($(template).html()) : $.trim($(this).html());
      if (msg && msg.length > 0) {
        if ($(this).hasClass('truncate')) {
          let length = 100;
          if ($(this).hasClass('truncate-300'))
            length = 300;
          msg = msg.substring(0, length) + '...';
        }
        $(this).html(marked.parse(tidy(msg)));
      }
    });
    $('.marked table').each(function () {
      $(this).addClass('table table-striped');
    });
    // set moment's locale
    moment.locale('${.lang}');
    $('.moment-date').each(function () {
      var msg = $.trim($(this).html());
      if (msg && msg.length > 0) {
        msg = moment(msg).format('LL');
        $(this).html(msg);
      }
    });
    $('.moment-datetime').each(function () {
      var msg = $.trim($(this).html());
      if (msg && msg.length > 0) {
        msg = moment(msg).format('LLL');
        $(this).html(msg);
      }
    });
    <#if cartEnabled>
    if (typeof onVariablesCartGet === 'function') {
      onVariablesCartGet(Carts.filter(c => c.type === 'variables').pop());
    }
    if (typeof onStudiesCartGet === 'function') {
      onStudiesCartGet(Carts.filter(c => c.type === 'studies').pop());
    }
    if (typeof onNetworksCartGet === 'function') {
      onNetworksCartGet(Carts.filter(c => c.type === 'networks').pop());
    }
    SetService.showCount('#cart-count', undefined, '${.lang}');
    </#if>
  });

  // Little helper to show desired elements after the page is ready
  window.addEventListener('DOMContentLoaded', (event) => {
    document.querySelectorAll(".show-on-content-loaded")
      .forEach(element => {
        setTimeout(() => element.classList.remove("d-none"), 250)
      })
  });
</script>
