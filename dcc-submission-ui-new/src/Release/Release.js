import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders, formatFileSize } from '~/utils';
import Status from '~/common/components/Status';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import SubmissionActionButtons from '~/common/components/SubmissionActionButtons/SubmissionActionButtons';

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
      sizePerPage: 3,
      paginationShowsTotal: true,
    };

    return <div>
      <h1>Release Summary</h1>
      <ul>
        <li>Name {release.name}</li>
        <li>State {release.state}</li>
        <li>Dictionary Version {release.dictionaryVersion}</li>
        <li>Number of projects {release.submissions.length}</li>
      </ul>

      <div>
        <h2>Projects included in the {release.name} release</h2>
        <BootstrapTable
          data={items}
          keyField='projectKey'
          striped={true}
          pagination={true}
          hideSizePerPage={true}
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
            dataFormat={ (cell, submission) => (
              <SubmissionActionButtons submission={submission}/>
            )}
          >Actions</TableHeaderColumn>
        </BootstrapTable>
      </div>
      <pre>
      {JSON.stringify(release, null, '  ')}
      </pre>
    </div>
  }
}