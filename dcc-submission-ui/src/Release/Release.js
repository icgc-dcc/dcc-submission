import React, { Component } from 'react';
import {Link} from 'react-router';
import { map, some } from 'lodash';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import { formatFileSize } from '~/utils';
import Status from '~/common/components/Status';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import user from '~/user';

import { defaultTableOptions, defaultTableProps } from '~/common/defaultTableOptions';
import ActionButton from '~/common/components/ActionButton/ActionButton';
import SubmissionActionButtons from '~/Submission/SubmissionActionButtons';

import RELEASE_STATES from './constants/RELEASE_STATES';

import ReleaseModel from './ReleaseModel.js';

import SignOffSubmissionModal from '~/Submission/modals/SignOffSubmissionModal';
import ValidateSubmissionModal from '~/Submission/modals/ValidateSubmissionModal';
import ResetSubmissionModal from '~/Submission/modals/ResetSubmissionModal';
import CancelSubmissionValidationModal from '~/Submission/modals/CancelSubmissionValidationModal';

import {queueSubmissionForValidation, signOffSubmission, resetSubmission, cancelSubmissionValidation} from '~/Submission/SubmissionModel';

import './Release.css';

const summaryClassNameMap = {
  SIGNED_OFF: 'label-success',
  VALID: 'label-success',
  QUEUED: 'label-warning',
  VALIDATING: 'label-warning',
  INVALID: 'label-danger',
  ERROR: 'label-danger',
  NOT_VALIDATED: 'label-default',
};

//NOTE: "project" is synonymous with "submission"
export default @observer
class Release extends Component {
  @observable release;

  @observable submissionToSignOff;
  @computed get shouldShowSignOffModal() { return !!this.submissionToSignOff }
  handleClickSignOffSubmission = (submission) => { this.submissionToSignOff = submission; }
  closeSignOffModal = () => { this.submissionToSignOff = null };
  handleRequestSubmitForSignOff = async () => {
    await signOffSubmission({projectKey: this.submissionToSignOff.projectKey});
    this.closeSignOffModal();
    this.release.fetch();
  }

  @observable submissionToValidate;
  @computed get shouldShowValidateModal() { return !!this.submissionToValidate }
  handleClickValidateSubmission = (submission) => { this.submissionToValidate = submission }
  closeValidateModal = () => { this.submissionToValidate = null };
  handleRequestSubmitForValidation = async ({dataTypes, emails}) => {
    await queueSubmissionForValidation({
      projectKey: this.submissionToValidate.projectKey,
      dataTypes,
      emails
    });
    this.closeValidateModal();
    this.release.fetch();
  };

  @observable submissionToReset;
  @computed get shouldShowResetModal() { return !!this.submissionToReset }
  handleClickReset = (submission) => { this.submissionToReset = submission }
  closeResetModal = () => { this.submissionToReset = null };
  handleRequestSubmitReset = async () => {
    await resetSubmission({projectKey: this.submissionToReset.projectKey});
    this.release.fetch();
    this.closeResetModal();
  }

  @observable submissionToCancelValidation;
  @computed get shouldShowCancelValidationModal() { return !!this.submissionToCancelValidation }
  handleClickCancelValidation = (submission) => { this.submissionToCancelValidation = submission }
  closeCancelValidationModal = () => { this.submissionToCancelValidation = null };
  handleRequestSubmitCancelValidation = async () => {
    await cancelSubmissionValidation({projectKey: this.submissionToCancelValidation.projectKey});
    this.release.fetch();
    this.closeResetModal();
  }

  componentWillMount() {
    const releaseName = this.props.params.releaseName;
    this.release = new ReleaseModel({name: releaseName});
    this.release.fetch();
    this._pollInterval = global.setInterval(this.release.fetch, require('~/common/constants/POLL_INTERVAL'));
  }

  componentWillUnmount() {
    global.clearInterval(this._pollInterval);
  }

  render () {
    const release = this.release;
    const releaseName = this.props.params.releaseName;
    const items = release.submissions;

    const tableOptions = {
      ...defaultTableOptions,
      noDataText: release.isLoading ? 'Loading...' : 'There is no data to display',
      defaultSortName: 'projectKey',
    };

    return (
    <div className="Release container">
      <h1>Release Summary</h1>
      <ul className="ReleaseSummaryList terms summary-terms">
        <li>
          <span className="terms__term">Name</span>
          <span className="terms__value">{release.name}</span>
        </li>
        <li>
          <span className="terms__term">State</span>
          <span className="terms__value">{release.state}</span>
        </li>
        <li>
          <span className="terms__term">Dictionary Version</span>
          <span className="terms__value">
            <a
              href={`http://docs.icgc.org/dictionary/viewer/#?vFrom=${release.dictionaryVersion}`}
              target="_blank"
            >
              {release.dictionaryVersion}
            </a>
          </span>
        </li>
        <li>
          <span className="terms__term">Number of projects</span>
          <span className="terms__value">{release.submissions.length}</span>
        </li>
        <li>
          { map(release.summary, (count, summaryKey) => (
            <span className={`ReleaseSummaryBadge label ${summaryClassNameMap[summaryKey]}`} key={summaryKey}>
              <span className="summary-key">{summaryKey}</span>
              <span className="summary-count">{count}</span>
            </span>
          ))}
        </li>
        
      </ul>

      <div>
        <h2>Projects included in the <em className="releaseName">{release.name}</em> release</h2>
        <BootstrapTable
          {...defaultTableProps}
          data={items}
          keyField='projectKey'
          striped={true}
          pagination={true}
          ignoreSinglePage={true}
          search={items.length > tableOptions.thresholdToShowSearch}
          options={tableOptions}
        >
          <TableHeaderColumn
            dataField='projectKey'
            dataSort={true}
            dataFormat={ projectKey => (
              <Link to={`/releases/${release.name}/submissions/${projectKey}`}>{projectKey}</Link>
            )}
            width="150"
          >Project Key</TableHeaderColumn>
          
          <TableHeaderColumn
            dataField='projectName'
            dataSort={true}
          >Project Name</TableHeaderColumn>
          
          <TableHeaderColumn
            dataField='submissionFiles'
            dataSort={true}
            sortFunc={(a, b, order) => order === 'desc' ? a.submissionFiles.length - b.submissionFiles.length : b.submissionFiles.length - a.submissionFiles.length}
            dataFormat={ files => {
              const fileSize = files
                  .map(x => x.size)
                  .reduce((a, b) => a + b, 0);
              const formattedFileSize = formatFileSize(fileSize);
              const fileCount = files.length;
              return `${fileCount} (${formattedFileSize})`;
            }}
            width="150"
          >Files</TableHeaderColumn>

          <TableHeaderColumn
            dataField='state'
            dataSort={true}
            dataFormat={ state => (
              <Status statusCode={state}/>
            )}
            width="170"
          >State</TableHeaderColumn>

          <TableHeaderColumn
            hidden={release.state !== RELEASE_STATES.OPENED}
            dataFormat={ (cell, submission) => (
              <SubmissionActionButtons
                submissionState={submission.state || ''}
                releaseState={release.state || ''}
                submissionHasFiles={!!submission.submissionFiles.length}
                userIsAdmin={user.isAdmin}
                buttonClassName="m-btn mini"
                onClickValidate={() => this.handleClickValidateSubmission(submission)}
                onClickSignOff={() => this.handleClickSignOffSubmission(submission)}
                onClickReset={() => this.handleClickReset(submission)}
                onClickCancelValidation={() => this.handleClickCancelValidation(submission)}
                isFileTransferInProgress={some(submission.submissionFiles, x => x.transferring)}
              />
            )}
            width="220"
          >Actions</TableHeaderColumn>
        </BootstrapTable>
      </div>

      <ValidateSubmissionModal
        isOpen={this.shouldShowValidateModal}
        onRequestSubmit={this.handleRequestSubmitForValidation}
        onRequestClose={this.closeValidateModal}
        dataTypeReports={this.submissionToValidate ? this.submissionToValidate.report.dataTypeReports.slice() : []}
        initiallySelectedDataTypes={this.submissionToValidate ? this.submissionToValidate.report.dataTypeReports.map(x => x.dataType) : []}
      />
      <SignOffSubmissionModal
        isOpen={this.shouldShowSignOffModal}
        onRequestSubmit={this.handleRequestSubmitForSignOff}
        onRequestClose={this.closeSignOffModal}
        projectKey={this.submissionToSignOff && this.submissionToSignOff.projectKey}
        projectName={this.submissionToSignOff && this.submissionToSignOff.projectName}
      />
      <ResetSubmissionModal
        isOpen={this.shouldShowResetModal}
        onRequestSubmit={this.handleRequestSubmitReset}
        onRequestClose={this.closeResetModal}
        projectName={this.submissionToReset ? this.submissionToReset.projectName : ''}
      />
      <CancelSubmissionValidationModal
        isOpen={this.shouldShowCancelValidationModal}
        onRequestSubmit={this.handleRequestSubmitCancelValidation}
        onRequestClose={this.closeCancelValidationModal}
        projectName={this.submissionToCancelValidation ? this.submissionToCancelValidation.projectName : ''}
      />
    </div>
    );
  }
}
