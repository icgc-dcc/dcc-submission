import React, { Component } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import { groupBy, map } from 'lodash';
import { formatFileSize } from '~/utils';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import DATATYPE_DICTIONARY from '~/common/constants/DATATYPE_DICTIONARY';
import Status from '~/common/components/Status';
import getValidFileCount from './getValidFileCount.coffee';
import SubmissionActionButtons from '~/Submission/SubmissionActionButtons';

import GroupedReportList from './GroupedReportList/GroupedReportList';

import SubmissionModel from './SubmissionModel';

export default @observer
class Submission extends Component {
  @observable submission;

  componentWillMount () {
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;
    this.submission = new SubmissionModel({releaseName, projectKey});
    this.submission.fetch();
  }

  render () {
    const submission = this.submission;
    console.log(submission);
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;
    // window.debugLoad = () => submission.fetch(releaseName, projectKey);

    return (
      <div>
        <h1>Submission Summary</h1>
        <ul>
          <li>Name {submission.projectName}</li>
          <li>Number of submitted files{submission.submissionFiles.length}</li>
          <li>Number of valid files: {getValidFileCount(submission.report)}</li>
          <li>Size of submission data: {formatFileSize(submission.totalFileSizeInBytes)}</li>
          <li>State <Status statusCode={submission.state || ''}/></li>
          <li><SubmissionActionButtons submission={submission} buttonClassName="m-btn"/></li>
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
            dataFormat={ dataType => ( DATATYPE_DICTIONARY[dataType] || dataType )}
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
    );
  }
}
