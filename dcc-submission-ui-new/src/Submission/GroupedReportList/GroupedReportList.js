import React from 'react';
import {Link} from 'react-router';
import {includes} from 'lodash';
import moment from 'moment';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import { formatFileSize } from '~/utils';
import { defaultTableOptions, defaultTableProps } from '~/common/defaultTableOptions';
import Status from '~/common/components/Status';

import DATATYPE_DICTIONARY from '~/common/constants/DATATYPE_DICTIONARY';

import './GroupedReportList.css';

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
    <div className="GroupedReportList">
      <div className="row">
        <div className="col-xs-6">
          {dataType !== 'undefined' && (
            <div>
              {title}
              &nbsp;-&nbsp;
              <Status statusCode={dataTypeReport.dataTypeState || ''}/>
            </div>
          )}
        </div>
        <div className="col-xs-6 text-right">
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
        </div>
      </div>
      
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
          dataFormat={(fileName, submission) => {
            const fileState = submission.fileState;
            const isNotInProgress = !includes(['QUEUED', 'VALIDATING', 'SKIPPED'], fileState);
            const hasReports = (submission.errorReports && submission.errorReports.length)
              || (submission.fieldReports && submission.fieldReports.length)
              || (submission.summaryReports && submission.summaryReports.length);
            const shouldShowReport = isNotInProgress && hasReports;

            return shouldShowReport ? <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${submission.name}`}>{fileName}</Link> : fileName;
          }}
        >File</TableHeaderColumn>

        <TableHeaderColumn
          dataField='lastUpdate'
          dataSort={true}
          dataFormat={ date => (
            moment(date).format('MMMM Do YYYY, h:mm a')
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


      </BootstrapTable>
    </div>
  )
}

// saving in case we need to bring "report" column back
// <TableHeaderColumn
//           dataField='name'
//           dataFormat={ (cell, submission) => {
//             const fileState = submission.fileState;
//             const isNotInProgress = !includes(['QUEUED', 'VALIDATING', 'SKIPPED'], fileState);
//             const hasReports = (submission.errorReports && submission.errorReports.length)
//               || (submission.fieldReports && submission.fieldReports.length)
//               || (submission.summaryReports && submission.summaryReports.length);
//             const shouldShowReport = isNotInProgress && hasReports;

//             return shouldShowReport ? <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${submission.name}`}>view</Link> : '';
//           }}