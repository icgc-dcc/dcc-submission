import React from 'react';
import {Link} from 'react-router';
import {includes} from 'lodash';
import moment from 'moment';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import { formatFileSize } from '~/utils';
import defaultTableOptions from '~/common/defaultTableOptions';
import Status from '~/common/components/Status';

import DATATYPE_DICTIONARY from '~/common/constants/DATATYPE_DICTIONARY';

export default function GroupedReportList({
    isLoading,
    items,
    releaseName,
    projectKey,
    dataType,
    dataTypeReport,
    submissionState,
    onRequestValidate,
  }) {
  const tableOptions = {
      ...defaultTableOptions,
      noDataText: isLoading ? 'Loading...' : 'There is no data to display',
      defaultSortName: 'name',
    };

  const title = DATATYPE_DICTIONARY[dataType] || dataType;
  const submissionStateCanBeChanged = !includes(['QUEUED', 'VALIDATING', 'ERROR'], submissionState);
  const groupCanBeSubmittedForValidation = submissionStateCanBeChanged && dataTypeReport && includes(['VALID', 'INVALID', 'NOT_VALIDATED'], dataTypeReport.dataTypeState)

  return (
    <div>
      {dataType !== 'undefined' && (
        <div>
          {title}
          -
          <Status statusCode={dataTypeReport.dataTypeState || ''}/>
        </div>
      )}
      {
        groupCanBeSubmittedForValidation && (
          <a
            data-toggle="modal"
            className="m-btn mini blue"
            onClick={onRequestValidate}
          >
          Validate {title}
          </a>
        )
      }
      
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

            return shouldShowReport ? <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${submission.name}`}>view</Link> : '';
          }}
        >Report</TableHeaderColumn>

      </BootstrapTable>
    </div>
  )
}