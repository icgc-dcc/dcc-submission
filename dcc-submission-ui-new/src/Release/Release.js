import React, { Component } from 'react';
import {Link} from 'react-router';
import { map } from 'lodash';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import { formatFileSize } from '~/utils';
import Status from '~/common/components/Status';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import defaultTableOptions from '~/common/defaultTableOptions';
import SubmissionActionButtons from '~/Submission/SubmissionActionButtons';

import RELEASE_STATES from './RELEASE_STATES';
import ReleaseNowButton from './ReleaseNowButton';

import ReleaseModel from './ReleaseModel.js';

const summaryClassNameMap = {
  SIGNED_OFF: 'label-success',
  VALID: 'label-success',
  QUEUED: 'label-warning',
  VALIDATING: 'label-warning',
  INVALID: 'label-danger',
  ERROR: 'label-danger',
  NOT_VALIDATED: 'label-default',
};

export default @observer
class Release extends Component {
  @observable release;

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
            <ReleaseNowButton
              release={release}
              onSuccess={() => release.fetch(releaseName)}
              className="m-btn green"
            />
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
              <SubmissionActionButtons submission={submission} buttonClassName="m-btn mini"/>
            )}
          >Actions</TableHeaderColumn>
        </BootstrapTable>
      </div>
    </div>
    );
  }
}