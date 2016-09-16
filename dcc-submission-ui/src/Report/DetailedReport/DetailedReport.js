import React from 'react';
import { defaultTableOptions, defaultTableProps } from '~/common/defaultTableOptions';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';
import DetailedReportSummary from './DetailedReportSummary.js';
import ProgressCircle from '~/common/components/ProgressCircle/ProgressCircle';

function DetailedReportTable ({ items, isLoading }) {
  const tableOptions = {
    ...defaultTableOptions,
    noDataText: isLoading ? 'Loading...' : 'There is no data to display',
    defaultSortName: 'name',
  };

  return (
    <BootstrapTable
      {...defaultTableProps}
      data={items}
      keyField='name'
      striped={true}
      pagination={true}
      ignoreSinglePage={true}
      search={items.length > tableOptions.thresholdToShowSearch}
      options={tableOptions}
    >
      <TableHeaderColumn
        dataField='name'
        dataSort={true}
        columnClassName="mono-font"
      >Column Name</TableHeaderColumn>

      <TableHeaderColumn
        dataField='completeness'
        dataSort={true}
        width="100"
        dataAlign="right"
      >Percentage of populated rows</TableHeaderColumn>

      <TableHeaderColumn
        dataField='populated'
        dataSort={true}
        width="100"
        dataAlign="right"
      >Number of populated rows</TableHeaderColumn>

      <TableHeaderColumn
        dataField='missing'
        dataSort={true}
        width="100"
        dataAlign="right"
      >Number of missing values</TableHeaderColumn>

      <TableHeaderColumn
        dataField='nulls'
        dataSort={true}
        width="100"
        dataAlign="right"
      >Number of rows with nulls</TableHeaderColumn>

      <TableHeaderColumn
        dataSort={true}
        dataFormat={ (cell, report) => (
          <DetailedReportSummary
            report={report}
          />
        )}
        headerAlign="right"
      >Summary</TableHeaderColumn>
    </BootstrapTable>
  );
}

export default function DetailedReport({ items, isLoading }) {

  const progress = items.length ? items.map(x => x.completeness).reduce((a, b) => a + b) / items.length / 100 : 0;

  return (
    <div>
      <h2>Detailed Report</h2>
      <ProgressCircle
        progress={progress}
      />
      <DetailedReportTable
        items={items}
        isLoading={isLoading}
      />
    </div>
  );
}