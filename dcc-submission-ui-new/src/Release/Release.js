import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders } from '~/utils';

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

window.rrrr = () => release.fetch('release1');

export default @observer
class Release extends Component {

  componentWillMount () {
    const releaseName = this.props.params.name
    release.fetch(releaseName);
  }

  render () {
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
        <table>
          <thead>
            <tr>
              <th>Project Key</th>
              <th>Project Name</th>
              <th>Files</th>
              <th>State</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            { /* TODO: see submission_table_view.coffee for all the logic involved */
              release.submissions.map(project => (
              <tr key={project.projectKey}>
                <td><Link to={`/releases/${release.name}/submissions/${project.projectKey}`}>{project.projectName}</Link></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <pre>
      {JSON.stringify(release, null, '  ')}
      </pre>
    </div>
  }
}