import React, { Component } from 'react';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import { groupBy, map, concat, flow, orderBy } from 'lodash';
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

import './Submission.css';

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

    const orderedDataTypes = flow(
      files => groupBy(files, 'dataType'),
      Object.keys,
    )(concat(submission.abstractlyGroupedSubmissionFiles.CLINICAL, submission.abstractlyGroupedSubmissionFiles.EXPERIMENTAL));

    return (
      <div className="Submission container">
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
        <div className="row">
          <div className="col-sm-5">
        <ul className="terms summary-terms">
          <li><span className="terms__term">Name</span><span className="terms__value">{submission.projectName}</span></li>
          <li><span className="terms__term">Number of submitted files</span><span className="terms__value">{submission.submissionFiles.length}</span></li>
          <li><span className="terms__term">Number of valid files</span><span className="terms__value">{getValidFileCount(submission.report)}</span></li>
          <li><span className="terms__term">Size of submission data</span><span className="terms__value">{formatFileSize(submission.totalFileSizeInBytes)}</span></li>
          <li><span className="terms__term">State</span> <Status statusCode={submission.state || ''}/></li>
          {
            submission.isFileTransferInProgress &&
            <li><span className="file-transfer-message">File transfer in progress</span></li>
          }
          <li className="actions-container">
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
              isFileTransferInProgress={ submission.isFileTransferInProgress }
            />
          </li>
        </ul>
          </div>
          <div className="col-sm-7 summary-table-container">
        <BootstrapTable
          data={orderBy(submission.report.dataTypeReports, (report) => orderedDataTypes.indexOf(report.dataType))}
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
          </div>
        </div>

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
