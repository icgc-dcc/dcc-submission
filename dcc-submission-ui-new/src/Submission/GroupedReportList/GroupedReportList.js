import React from 'react';
import {Link} from 'react-router';
import {includes} from 'lodash';
import moment from 'moment';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import { formatFileSize } from '~/utils';
import defaultTableOptions from '~/common/defaultTableOptions';
import Status from '~/common/components/Status';

import dataTypeDict from './dataTypeDict';

export default function GroupedReportList(props) {
  const { isLoading, items, releaseName, projectKey, dataType } = props;
  const tableOptions = {
      ...defaultTableOptions,
      noDataText: isLoading ? 'Loading...' : 'There is no data to display',
      defaultSortName: 'name',
    };
  return (
    <div>
      {dataType !== 'undefined' && <h2>{dataTypeDict[dataType] || dataType}</h2>}
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
          dataFormat={ fileName => (
            <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${fileName}`}>{fileName}</Link>
          )}
        >File</TableHeaderColumn>

        <TableHeaderColumn
          dataField='lastUpdate'
          dataSort={true}
          dataFormat={ date => (
            moment(date).format('MMMM Do YYYY, h:mm:ss a')
          )}
        >Last Updated</TableHeaderColumn>

        <TableHeaderColumn
          dataField='size'
          dataSort={true}
          dataFormat={ fileSize => formatFileSize(fileSize) }
        >Size</TableHeaderColumn>

        <TableHeaderColumn
          dataField='fileState'
          dataSort={true}
          dataFormat={ fileState => (
            <Status statusCode={fileState || ''}/>
          )}
        >Status</TableHeaderColumn>

        <TableHeaderColumn
          dataField='name'
          dataFormat={ (cell, submission) => {
            const fileState = submission.fileState;
            const isNotInProgress = !includes(['QUEUED', 'VALIDATING', 'SKIPPED'], fileState);
            const hasReports = (submission.errorReports && submission.errorReports.length)
              || (submission.fieldReports && submission.fieldReports.length)
              || (submission.summaryReports && submission.summaryReports.length);
            const shouldShowReport = isNotInProgress && hasReports;

            return shouldShowReport ? <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${submission.name}`}>view report</Link> : '';
          }}
        >Report</TableHeaderColumn>

      </BootstrapTable>
    </div>
  )
}