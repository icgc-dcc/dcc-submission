import React, { Component } from 'react';
import {Link} from 'react-router';
import { map } from 'lodash';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import { formatFileSize } from '~/utils';
import Status from '~/common/components/Status';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import defaultTableOptions from '~/common/defaultTableOptions';
import ActionButton from '~/common/components/ActionButton/ActionButton';
import SubmissionActionButtons from '~/Submission/SubmissionActionButtons';

import RELEASE_STATES from './RELEASE_STATES';

import ReleaseModel from './ReleaseModel.js';

import SignOffSubmissionModal from '~/Submission/modals/SignOffSubmissionModal';
import ValidateSubmissionModal from '~/Submission/modals/ValidateSubmissionModal';
import PerformReleaseModal from '~/Release/modals/PerformReleaseModal';

import {queueSubmissionForValidation, signOffSubmission} from '~/Submission/SubmissionModel';

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
  @observable submissionToValidate;
  @computed get shouldShowValidateModal() {
    return !!this.submissionToValidate;
  }

  @observable submissionToSignOff;
  @computed get shouldShowSignOffModal() {
    return !!this.submissionToSignOff;
  }

  @observable releaseToPerform;
  @computed get shouldShowPerformReleaseModal() {
    return !!this.releaseToPerform;
  }

  handleClickValidateSubmission = (submission) => {
    this.submissionToValidate = submission;
  }

  handleClickSignOffSubmission = (submission) => {
    this.submissionToSignOff = submission;
  }

  handleClickPerformRelease = (release) => {
    this.releaseToPerform = release;
  }

  handleRequestSubmitForValidation = async ({dataTypes, emails}) => {
    await queueSubmissionForValidation({
      projectKey: this.submissionToValidate.projectKey,
      dataTypes,
      emails
    });
    this.closeValidateModal();
    this.release.fetch();
  };

  handleRequestSubmitForSignOff = async () => {
    await signOffSubmission({projectKey: this.submissionToSignOff.projectKey});
    this.closeSignOffModal();
    this.release.fetch();
  }

  handleRequestSubmitForRelease = async ({nextReleaseName}) => {
    await this.release.performRelease({nextReleaseName});
    this.closePerformReleaseModal();
    this.release.fetch();
  }

  closeValidateModal = () => { this.submissionToValidate = null };
  closeSignOffModal = () => { this.submissionToSignOff = null };
  closePerformReleaseModal = () => { this.releaseToPerform = null };

  componentWillMount () {
    const releaseName = this.props.params.releaseName;
    this.release = new ReleaseModel({name: releaseName});
    this.release.fetch();
  }

  render () {
    const release = this.release;
    const releaseName = this.props.params.releaseName;
    window.debugLoad = () => release.fetch(releaseName);
    const items = release.submissions;

    const tableOptions = {
      ...defaultTableOptions,
      noDataText: release.isLoading ? 'Loading...' : 'There is no data to display',
      defaultSortName: 'projectKey',
    };

    return (
    <div>
      <h1>Release Summary</h1>
      <ul className="ReleaseSummaryList">
        <li>
          <span className="key">Name</span>
          <span className="value">{release.name}</span>
        </li>
        <li>
          <span className="key">State</span>
          <span>{release.state}</span>
        </li>
        <li>
          <span>Dictionary Version</span>
          <span>{release.dictionaryVersion}</span>
        </li>
        <li>
          <span>Number of projects</span>
          <span>{release.submissions.length}</span>
        </li>
        <li>
          { map(release.summary, (count, summaryKey) => (
            <span className={`ReleaseSummaryBadge label ${summaryClassNameMap[summaryKey]}`} key={summaryKey}>
              <span className="summary-key">{summaryKey}</span>
              <span className="summary-count">{count}</span>
            </span>
          ))}
        </li>
        { release.state === RELEASE_STATES.OPENED && (
          <li>
            <ActionButton
              onClick={this.handleClickPerformRelease}
              className={`m-btn mini green-stripe`}
            >
              Release Now
            </ActionButton>
          </li>
        )}
      </ul>

      <div>
        <h2>Projects included in the {release.name} release</h2>
        <BootstrapTable
          data={items}
          keyField='projectKey'
          striped={true}
          pagination={true}
          ignoreSinglePage={true}
          search={items.length > tableOptions.sizePerPage}
          options={tableOptions}
        >
          <TableHeaderColumn
            dataField='projectKey'
            dataSort={true}
            dataFormat={ projectKey => (
              <Link to={`/releases/${release.name}/submissions/${projectKey}`}>{projectKey}</Link>
            )}
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
              const fileSize = formatFileSize(files
                  .map(x => x.size)
                  .reduce((a, b) => a + b));
              const fileCount = files.length;
              return `${fileCount} (${fileSize})`;
            }}
          >Files</TableHeaderColumn>

          <TableHeaderColumn
            dataField='state'
            dataSort={true}
            dataFormat={ state => (
              <Status statusCode={state}/>
            )}
          >State</TableHeaderColumn>

          <TableHeaderColumn
            hidden={release.state !== RELEASE_STATES.OPENED}
            dataFormat={ (cell, submission) => (
              <SubmissionActionButtons
                submission={submission}
                buttonClassName="m-btn mini"
                onClickValidate={() => this.handleClickValidateSubmission(submission)}
                onClickSignOff={() => { this.handleClickSignOffSubmission(submission) }}
              />
            )}
          >Actions</TableHeaderColumn>
        </BootstrapTable>
      </div>

      <ValidateSubmissionModal
        isOpen={this.shouldShowValidateModal}
        onRequestSubmit={this.handleRequestSubmitForValidation}
        onRequestClose={this.closeValidateModal}
        dataTypeReports={this.submissionToValidate ? this.submissionToValidate.report.dataTypeReports.slice() : []}
        initiallySelectedDataTypes={this.submissionToValidate ? this.submissionToValidate.report.dataTypeReports.map(x => x.dataType) : []}
        defaultEmailsText={``}
      />
      <SignOffSubmissionModal
        isOpen={this.shouldShowSignOffModal}
        onRequestSubmit={this.handleRequestSubmitForSignOff}
        onRequestClose={this.closeSignOffModal}
        projectKey={this.submissionToSignOff && this.submissionToSignOff.projectKey}
        projectName={this.submissionToSignOff && this.submissionToSignOff.projectName}
      />
      <PerformReleaseModal
        isOpen={this.shouldShowPerformReleaseModal}
        onRequestSubmit={this.handleRequestSubmitForRelease}
        onRequestClose={this.closePerformReleaseModal}
        releaseName={this.release.name}
      />
    </div>
    );
  }
}
