import React, { Component } from 'react';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import { groupBy, map } from 'lodash';
import { formatFileSize } from '~/utils';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import user from '~/user';
import DATATYPE_DICTIONARY from '~/common/constants/DATATYPE_DICTIONARY';
import Status from '~/common/components/Status';
import getValidFileCount from './getValidFileCount.coffee';
import SubmissionActionButtons from '~/Submission/SubmissionActionButtons';

import GroupedReportList from './GroupedReportList/GroupedReportList';

import ReleaseModel from '~/Release/ReleaseModel';
import SubmissionModel from './SubmissionModel';
import ValidateSubmissionModal from '~/Submission/modals/ValidateSubmissionModal';
import SignOffSubmissionModal from '~/Submission/modals/SignOffSubmissionModal';
import ResetSubmissionModal from '~/Submission/modals/ResetSubmissionModal';
import CancelSubmissionValidationModal from '~/Submission/modals/CancelSubmissionValidationModal';

// import SUBMISSION_OPERATIONS from '~/Submission/SUBMISSION_OPERATIONS';

//NOTE: "project" is synonymous with "submission"
export default @observer
class Submission extends Component {
  @observable submission;

  @observable dataTypesToValidate = [];
  @computed get shouldShowValidateModal() {
    return this.dataTypesToValidate.length !== 0;
  }

  @observable submissionToSignOff = undefined;
  @computed get shouldShowSignOffModal() {
    return !!this.submissionToSignOff;
  }

  @observable submissionToReset = undefined;
  @computed get shouldShowResetModal() {
    return !!this.submissionToReset;
  }

  @observable submissionToCancelValidation = undefined;
  @computed get shouldShowCancelValidationModal() {
    return !!this.submissionToCancelValidation;
  }

  componentWillMount () {
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;
    this.submission = new SubmissionModel({releaseName, projectKey});
    this.release = new ReleaseModel({name: releaseName});
    this.submission.fetch();
    this.release.fetch();
    this._pollInterval = global.setInterval(this.submission.fetch, require('~/common/constants/POLL_INTERVAL'));
  }

  componentWillUnmount() {
    global.clearInterval(this._pollInterval);
  }

  closeValidateModal = () => {
    this.dataTypesToValidate = [];
  }

  closeSignOffModal = () => {
    this.submissionToSignOff = null;
  }

  closeCancelValidationModal = () => {
    this.submissionToCancelValidation = null;
  }

  handleRequestSubmitValidate = async ({dataTypes, emails}) => {
    await this.submission.requestValidation({dataTypes, emails});
    await this.submission.fetch();
    this.closeValidateModal();
  };

  handleRequestSubmitSignOff = async () => {
    await this.submission.signOff();
    await this.submission.fetch();
    this.closeSignOffModal();
  };

  handleRequestSubmitCancelValidation = async () => {
    await this.submission.cancelValidation();
    await this.submission.fetch();
    this.closeSignOffModal();
  };

  closeResetModal = () => { this.submissionToReset = null };

  handleRequestSubmitReset = async () => {
    await this.submission.reset();
    await this.submission.fetch();
    this.closeResetModal();
  }

  setDataTypesToValidate = (dataTypes) => { this.dataTypesToValidate = dataTypes };

  handleClickValidate = () => {
    this.dataTypesToValidate = this.submission.report.dataTypeReports.map( x => x.dataType);
  };

  handleClickSignOff = () => {
    this.submissionToSignOff = this.submission;
  };

  handleClickReset = () => {
    this.submissionToReset = this.submission;
  };

  handleClickCancelValidation = () => {
    this.submissionToCancelValidation = this.submission;
  };

  render () {
    const submission = this.submission;
    const releaseName = this.props.params.releaseName;
    const projectKey = this.props.params.projectKey;

    return (
      <div className="container">
        <ValidateSubmissionModal
          isOpen={this.shouldShowValidateModal}
          onRequestSubmit={this.handleRequestSubmitValidate}
          onRequestClose={this.closeValidateModal}
          dataTypeReports={this.submission.report.dataTypeReports.slice()}
          initiallySelectedDataTypes={this.dataTypesToValidate.slice()}
        />
        <SignOffSubmissionModal
          isOpen={this.shouldShowSignOffModal}
          onRequestSubmit={this.handleRequestSubmitSignOff}
          onRequestClose={this.closeSignOffModal}
          projectKey={projectKey}
          projectName={this.submission.projectName}
        />
        <ResetSubmissionModal
          isOpen={this.shouldShowResetModal}
          onRequestSubmit={this.handleRequestSubmitReset}
          onRequestClose={this.closeResetModal}
          projectName={this.submission.projectName || ''}
        />
        <CancelSubmissionValidationModal
          isOpen={this.shouldShowCancelValidationModal}
          onRequestSubmit={this.handleRequestSubmitCancelValidation}
          onRequestClose={this.closeCancelValidationModal}
          projectName={this.submission.projectName || ''}
        />
        <h1>Submission Summary</h1>
        <ul>
          <li>Name {submission.projectName}</li>
          <li>Number of submitted files{submission.submissionFiles.length}</li>
          <li>Number of valid files: {getValidFileCount(submission.report)}</li>
          <li>Size of submission data: {formatFileSize(submission.totalFileSizeInBytes)}</li>
          <li>State <Status statusCode={submission.state || ''}/></li>
          <li>
            <SubmissionActionButtons
              submissionState={submission.state || ''}
              submissionHasFiles={!!submission.submissionFiles.length}
              releaseState={this.release.state || ''}
              userIsAdmin={user.isAdmin}
              buttonClassName="m-btn"
              onClickValidate={ this.handleClickValidate }
              onClickSignOff={ this.handleClickSignOff }
              onClickReset={ this.handleClickReset }
              onClickCancelValidation={ this.handleClickCancelValidation }
            />
          </li>
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
                    onRequestValidate={() => this.setDataTypesToValidate([dataType])}
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
                    onRequestValidate={() => this.setDataTypesToValidate([dataType])}
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
