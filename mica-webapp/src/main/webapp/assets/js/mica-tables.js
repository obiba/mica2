'use strict';

// Helpers for DataTables

const initSelectDataTable = function(dTable, options) {
  // index of the item ID in the row's array of data
  const idIndex = options.idIndex ? options.idIndex : 1;
  // table cell's class that contains the checkbox
  const selectClassName = options.className ? options.className : 'select-checkbox';
  // fa icons
  const checkedIconClassName = options.checkedIconClassName ? options.checkedIconClassName : 'fa-check-square';
  const notCheckedIconClassName = options.notCheckedIconClassName ? options.notCheckedIconClassName : 'fa-square';

  const headerSelector = 'th.' + selectClassName;
  const checkHeaderSelector = headerSelector + ' i';
  const checkRowSelector = 'td.' + selectClassName + ' i';

  dTable.on('draw', function() { //
    // init selection header (because of paging, then selection event will be triggered)
    $(checkHeaderSelector).removeClass(checkedIconClassName).addClass(notCheckedIconClassName);
    // apply selection
    dTable.rows().data().toArray().forEach((rowData, idx) => {
      const id = rowData[idIndex];
      if (options.isSelected(id)) {
        dTable.row(idx).select();
      }
    });
  }).on('click', headerSelector, function() {
    // select/deselect all (selection event will be triggered)
    const selectHeader = $(checkHeaderSelector);
    if (selectHeader.hasClass(checkedIconClassName)) {
      dTable.rows().deselect();
    } else {
      dTable.rows().select();
    }
  }).on('select deselect', function() {
    // toggle state of the header's checkbox depending on the count of the selected rows
    const selectHeader = $(checkHeaderSelector);
    if (dTable.rows({
      selected: true
    }).count() !== dTable.rows().count()) {
      selectHeader.removeClass(checkedIconClassName).addClass(notCheckedIconClassName);
    } else {
      selectHeader.removeClass(notCheckedIconClassName).addClass(checkedIconClassName);
    }
  }).on('select', function(e, dt, type, indexes) {
    // toggle state of the rows' checkbox
    $('tr.selected ' + checkRowSelector).removeClass(notCheckedIconClassName).addClass(checkedIconClassName);
    options.onSelectionChanged(dTable.rows(indexes).data().toArray().map(rowData => rowData[idIndex]), true);
  }).on('deselect', function(e, dt, type, indexes) {
    // toggle state of the rows' checkbox
    $('tr:not(.selected) ' + checkRowSelector).removeClass(checkedIconClassName).addClass(notCheckedIconClassName);
    options.onSelectionChanged(dTable.rows(indexes).data().toArray().map(rowData => rowData[idIndex]), false);
  });
}
