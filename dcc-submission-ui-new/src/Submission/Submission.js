import React, { Component } from 'react';
import {observable, action, runInAction, computed} from 'mobx';
import {observer} from 'mobx-react';
import { includes, groupBy, map } from 'lodash';
import { fetchHeaders, formatFileSize } from '~/utils';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import dataTypeDict from '~/common/dataTypeDict';
import Status from '~/common/components/Status';
import injectReportsToSubmissionFiles from './injectReportsToSubmissionFiles.coffee';
import getValidFileCount from './getValidFileCount.coffee';
import SubmissionActionButtons from '~/common/components/SubmissionActionButtons/SubmissionActionButtons';

import GroupedReportList from './GroupedReportList/GroupedReportList';

const CLINICAL_DATA_TYPES = ['CLINICAL_SUPPLEMENTAL_TYPE', 'CLINICAL_CORE_TYPE'];

const submission = observable({
  isLoading: false,
  lastUpdated: undefined,
  locked: false,
  projectAlias: undefined,
  projectKey: undefined,
  projectName: undefined,
  releaseName: undefined,
  report: {
    // why is this nested?
    dataTypeReports: []
  },
  state: undefined,
  submissionFiles: [],

  abstractlyGroupedSubmissionFiles: function () {
    return groupBy(submission.submissionFiles, file => (
      includes(CLINICAL_DATA_TYPES, file.dataType)
        ? 'CLINICAL'
        : !!file.dataType
          ? 'EXPERIMENTAL'
          : 'UNRECOGNIZED'
    ));
  },
});

submission.fetch = action('fetch single submission/project', async function (releaseName, projectKey) {
  this.isLoading = true;
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}`, {
      headers: fetchHeaders.get()
    });

  runInAction('update loading status', () => { this.isLoading = false });

  const submissionData = await response.json();

  runInAction('update submission', () => {
    Object.assign(this, submissionData);
    injectReportsToSubmissionFiles(this.submissionFiles, this.report);
  });
});

submission.totalFileSizeInBytes = computed(function () {
  return submission.submissionFiles.reduce((acc, file) => acc + file.size, 0);
})

window.submission = submission;

export default @observer
class Release extends Component {

  componentWillMount () {
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;
    submission.fetch(releaseName, projectKey);
  }

  render () {
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;
    window.debugLoad = () => submission.fetch(releaseName, projectKey);

    return <div>
      <h1>Submission Summary</h1>
      <ul>
        <li>Name {submission.projectName}</li>
        <li>Number of submitted files{submission.submissionFiles.length}</li>
        <li>Number of valid files: {getValidFileCount(submission.report)}</li>
        <li>Size of submission data: {formatFileSize(submission.totalFileSizeInBytes.get())}</li>
        <li>State {submission.state}</li>
        <li>Actions: <SubmissionActionButtons submission={submission}/></li>
      </ul>

      <BootstrapTable
        data={submission.report.dataTypeReports}
        keyField='dataType'
        striped={true}
        pagination={false}
        search={false}
      >
        <TableHeaderColumn
          dataField='dataType'
          dataFormat={ dataType => ( dataTypeDict[dataType] || dataType )}
        >Data Type</TableHeaderColumn>
        <TableHeaderColumn
          dataField='dataTypeState'
          dataFormat={ state => <Status statusCode={state || ''}/>}
        >State</TableHeaderColumn>
      </BootstrapTable>

      <div>
        {
          submission.abstractlyGroupedSubmissionFiles.CLINICAL && (
            <div>
              <h1>Clinical Report</h1>
              {map(groupBy(submission.abstractlyGroupedSubmissionFiles.CLINICAL, 'dataType'), (files, dataType) => (
                <GroupedReportList
                  submissionState={submission.state}
                  dataTypeReport={submission.report.dataTypeReports.find( report => report.dataType === dataType)}
                  key={dataType}
                  dataType={dataType}
                  items={files}
                  releaseName={releaseName}
                  projectKey={projectKey}
                  isLoading={submission.isLoading}
                />
              ))}
            </div>
          )
        }
        
        {
          submission.abstractlyGroupedSubmissionFiles.EXPERIMENTAL && (
            <div>
              <h1>Experimental Report</h1>
              {map(groupBy(submission.abstractlyGroupedSubmissionFiles.EXPERIMENTAL, 'dataType'), (files, dataType) => (
                <GroupedReportList
                  submissionState={submission.state}
                  dataTypeReport={submission.report.dataTypeReports.find( report => report.dataType === dataType)}
                  key={dataType}
                  dataType={dataType}
                  items={files}
                  releaseName={releaseName}
                  projectKey={projectKey}
                  isLoading={submission.isLoading}
                />
              ))}
            </div>
          )
        }

        {
          submission.abstractlyGroupedSubmissionFiles.UNRECOGNIZED && (
            <div>
              <h1>Unrecognized</h1>
              {map(groupBy(submission.abstractlyGroupedSubmissionFiles.UNRECOGNIZED, 'dataType'), (files, dataType) => (
                <GroupedReportList
                  submissionState={submission.state}
                  dataTypeReport={submission.report.dataTypeReports.find( report => report.dataType === dataType)}
                  key={dataType}
                  dataType={dataType}
                  items={files}
                  releaseName={releaseName}
                  projectKey={projectKey}
                  isLoading={submission.isLoading}
                />
              ))}
            </div>
          )
        }
        
      </div>
    </div>
  }
}
