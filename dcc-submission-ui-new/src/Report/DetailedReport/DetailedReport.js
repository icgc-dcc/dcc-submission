import React from 'react';
import defaultTableOptions from '~/common/defaultTableOptions';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';
import DetailedReportSummary from './DetailedReportSummary.js';

export default function DetailedReportTable ({ items, isLoading }) {
  const tableOptions = {
    ...defaultTableOptions,
    noDataText: isLoading ? 'Loading...' : 'There is no data to display',
    defaultSortName: 'name',
  };

  return (
    <BootstrapTable
      data={items}
      keyField='name'
      striped={true}
      pagination={true}
      ignoreSinglePage={true}
      search={items.length > tableOptions.sizePerPage}
      options={tableOptions}
    >
      <TableHeaderColumn
        dataField='name'
        dataSort={true}
      >Column Name</TableHeaderColumn>

      <TableHeaderColumn
        dataField='completeness'
        dataSort={true}
        width="100"
      >Percentage of populated rows</TableHeaderColumn>

      <TableHeaderColumn
        dataField='populated'
        dataSort={true}
        width="100"
      >Number of populated rows</TableHeaderColumn>

      <TableHeaderColumn
        dataField='missing'
        dataSort={true}
        width="100"
      >Number of missing values</TableHeaderColumn>

      <TableHeaderColumn
        dataField='nulls'
        dataSort={true}
        width="100"
      >Number of rows with nulls</TableHeaderColumn>

      <TableHeaderColumn
        dataSort={true}
        width="160"
        dataFormat={ (cell, report) => (
          <DetailedReportSummary
            report={report}
          />
        )}
      >Summary</TableHeaderColumn>
    </BootstrapTable>
  );
}