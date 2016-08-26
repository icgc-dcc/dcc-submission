import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import BootstrapTable from 'reactjs-bootstrap-table';
import { fetchHeaders } from '~/utils';
import Status from '~/common/components/Status';

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
          data={release.submissions}
          headers={true}
          columns={[
            {
              name: 'projectKey',
              display: 'Project Key',
              sortable: true,
              renderer: project => (
                <Link to={`/releases/${release.name}/submissions/${project.projectKey}`}>{project.key}</Link>
              )
            },
            { name: 'projectName', display: 'Project Name' },
            { name: 'files', display: 'Files' },
            { name: 'state', display: 'State', renderer: project => (
              <Status statusCode={project.state}/>
            )},
            { name: 'actions', display: 'Actions' },
          ]}
        />
      </div>
      <pre>
      {JSON.stringify(release, null, '  ')}
      </pre>
    </div>
  }
}