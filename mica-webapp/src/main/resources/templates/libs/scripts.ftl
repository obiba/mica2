<!-- REQUIRED SCRIPTS -->

<!-- jQuery -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/jquery/jquery.min.js"></script>
<!-- Bootstrap 4 -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/bootstrap/js/bootstrap.bundle.min.js"></script>
<!-- AdminLTE App -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/dist/js/adminlte.min.js"></script>
<!-- Moment -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/moment/moment-with-locales.min.js"></script>
<!-- DataTables -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/datatables/jquery.dataTables.js"></script>
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/datatables-bs4/js/dataTables.bootstrap4.js"></script>
<!-- Toastr -->
<script src="${pathPrefix!".."}/js/libs/node_modules/admin-lte/plugins/toastr/toastr.min.js"></script>
<!-- Axios -->
<script src="${pathPrefix!".."}/js/libs/node_modules/axios/dist/axios.min.js"></script>
<!-- Marked -->
<script src="${pathPrefix!".."}/js/libs/node_modules/marked/lib/marked.js"></script>
<!-- Mica Utils and dependencies -->
<script src="${pathPrefix!".."}/js/libs/node_modules/jquery.redirect/jquery.redirect.js"></script>
<script src="${pathPrefix!".."}/js/libs/node_modules/js-cookie/src/js.cookie.js"></script>
<script src="${pathPrefix!".."}/js/mica.js"></script>

<!-- Global js variables -->
<script>
    var dataTablesDefaultOpts = {
        "paging": true,
        "lengthChange": true,
        "searching": true,
        "ordering": true,
        "info": true,
        "autoWidth": true,
        "language": {
            "url": "${pathPrefix!".."}/i18n/datatables.${.lang}.json"
        }
    };
    var dataTablesSortSearchOpts = {
        "paging": false,
        "lengthChange": false,
        "searching": true,
        "ordering": true,
        "info": false,
        "autoWidth": true,
        "language": {
            "url": "${pathPrefix!".."}/i18n/datatables.${.lang}.json"
        }
    };
    var dataTablesSortOpts = {
        "paging": false,
        "lengthChange": false,
        "searching": false,
        "ordering": true,
        "info": false,
        "autoWidth": true,
        "language": {
            "url": "${pathPrefix!".."}/i18n/datatables.${.lang}.json"
        }
    };
    var dataTablesNoSortSearchOpts = {
        "paging": true,
        "lengthChange": false,
        "searching": true,
        "ordering": false,
        "info": false,
        "autoWidth": true,
        "language": {
            "url": "${pathPrefix!".."}/i18n/datatables.${.lang}.json"
        }
    };
</script>

<script>
    $(function () {
        // apply markdown rendering
        $('.marked').each(function () {
            var msg = $.trim($(this).html());
            if (msg && msg.length > 0) {
                $(this).html(marked(msg));
            }
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
    });
</script>
