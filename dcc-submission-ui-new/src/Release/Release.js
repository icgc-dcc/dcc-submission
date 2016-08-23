import React, { Component } from 'react';
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

window.rrrr = release;

export default @observer
class Release extends Component {
  componentWillMount () {
    // release.fetch();
  }

  render () {
    return <div>
      Single Release!
      <pre>
      {JSON.stringify(release, null, '  ')}
      </pre>
    </div>
  }
}