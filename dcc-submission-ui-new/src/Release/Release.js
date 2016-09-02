import React, { Component } from 'react';
import {Link} from 'react-router';
import { map } from 'lodash';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders, formatFileSize } from '~/utils';
import Status from '~/common/components/Status';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import defaultTableOptions from '~/common/defaultTableOptions';
import SubmissionActionButtons from '~/common/components/SubmissionActionButtons/SubmissionActionButtons';

import RELEASE_STATES from './RELEASE_STATES';
import ReleaseNowButton from './ReleaseNowButton';

const summaryClassNameMap = {
  SIGNED_OFF: 'label-success',
  VALID: 'label-success',
  QUEUED: 'label-warning',
  VALIDATING: 'label-warning',
  INVALID: 'label-danger',
  ERROR: 'label-danger',
  NOT_VALIDATED: 'label-default',
};

const release = observable({
  isLoading: false,
  dictionaryVersion: undefined,
  locked: false,
  name: '',
  queue: [],
  releaseDate: undefined,
  state: undefined,
  submissions: [],
  summary: undefined
});

window.release = release;

release.fetch = action('fetch single release', async function (releaseName) {
  this.isLoading = true;
  const response = await fetch(`/ws/releases/${releaseName}`, {
    headers: fetchHeaders.get()
  });

  runInAction('update loading status', () => { this.isLoading = false });

  const releaseData = await response.json();

  runInAction('update releases', () => {
    Object.assign(this, releaseData);
  });
});

export default @observer
class Release extends Component {

  componentWillMount () {
    const releaseName = this.props.params.releaseName;
    release.fetch(releaseName);
  }

  render () {
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