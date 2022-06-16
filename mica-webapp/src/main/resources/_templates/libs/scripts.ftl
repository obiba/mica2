<!-- REQUIRED SCRIPTS -->

<!-- jQuery -->
<script src="${adminLTEPath}/plugins/jquery/jquery.min.js"></script>
<!-- Popper -->
<script src="${adminLTEPath}/plugins/popper/umd/popper.min.js"></script>
<script src="${adminLTEPath}/plugins/popper/umd/popper-utils.min.js"></script>

<!-- Bootstrap 4 -->
<script src="${adminLTEPath}/plugins/bootstrap/js/bootstrap.bundle.min.js"></script>
<!-- AdminLTE App -->
<script src="${adminLTEPath}/dist/js/adminlte.min.js"></script>
<!-- Moment -->
<script src="${adminLTEPath}/plugins/moment/moment-with-locales.min.js"></script>
<!-- DataTables -->
<script src="${adminLTEPath}/plugins/datatables/jquery.dataTables.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-bs4/js/dataTables.bootstrap4.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-fixedheader/js/dataTables.fixedHeader.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-fixedheader/js/fixedHeader.bootstrap4.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-buttons/js/dataTables.buttons.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-buttons/js/buttons.bootstrap4.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-buttons/js/buttons.html5.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-buttons/js/buttons.print.min.js"></script>
<script src="${adminLTEPath}/plugins/datatables-select/js/dataTables.select.min.js"></script>
<!-- Toastr -->
<script src="${adminLTEPath}/plugins/toastr/toastr.min.js"></script>
<!-- Axios -->
<script src="${assetsPath}/libs/node_modules/axios/dist/axios.min.js"></script>
<!-- Marked -->
<script src="${assetsPath}/libs/node_modules/marked/lib/marked.js"></script>
<!-- Mica Utils and dependencies -->
<script src="${assetsPath}/libs/node_modules/jquery.redirect/jquery.redirect.js"></script>
<script src="${assetsPath}/libs/node_modules/js-cookie/src/js.cookie.js"></script>
<script src="${assetsPath}/js/mica.js"></script>

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

    // bs tooltip
    $('[data-toggle="tooltip"]').tooltip();
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
        $(this).html(marked(tidy(msg)));
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
</script>
