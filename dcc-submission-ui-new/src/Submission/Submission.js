import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction, computed} from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders, formatFileSize } from '~/utils';

import Status from '~/common/components/Status';

import injectReportsToSubmissionFiles from './injectReportsToSubmissionFiles.coffee';
import getValidFileCount from './getValidFileCount.coffee';

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
  submissionFiles: []
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
        <li>Actions: TODO</li>
      </ul>

      <div>
        <h2>Projects included in the {submission.name} release</h2>
        <table>
          <thead>
            <tr>
              <th></th>
              <th></th>
              <th></th>
              <th></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            { submission.submissionFiles.map( file => {
              const hasAnyReports = [].concat(file.errorReports, file.fieldReports, file.summaryReports).filter(Boolean).length > 0;
              return (
                <tr key={file.name}>
                  <td>{file.name}</td>
                  <td>{file.lastUpdate}</td>
                  <td>{formatFileSize(file.size)}</td>
                  <td>
                    <Status statusCode={file.fileState} />
                  </td>
                  <td>
                      { hasAnyReports ? (
                        <Link to={`/releases/${releaseName}/submissions/${projectKey}/report/${file.name}`}>view report</Link>
                      ) : null }
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <pre>
      {JSON.stringify(submission, null, '  ')}
      </pre>
    </div>
  }
}