import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders } from '~/utils';

const releases = observable({
  isLoading: false,
  items: []
});

releases.fetch = action('fetch releases', async function () {
  this.isLoading = true;
  const response = await fetch('/ws/releases', {
      headers: fetchHeaders.get()
    });

  runInAction('update loading status', () => { this.isLoading = false });

  const items = await response.json();

  runInAction('update releases', () => {
    this.items = items;
  });
});

window.asdf = releases;

function Actions ({ state }) {
  // TODO: see submissions_table_view.coffee#158-228
  return <div>actions for {state}</div>
}

export default @observer
class Releases extends Component {
  componentWillMount () {
    releases.fetch();
  }

  render () {
    return <div>
      Releases!
      {releases.items.map( release => (
        <div key={release.name}>
          <Link to={`/releases/${release.name}`}>
            Name: {release.name}
          </Link> |
            State: {release.state} |
            Release Date: {release.releaseDate || 'Unreleased'} |
            Projects: {release.submissions.length} |
            Actions: <Actions state={release.state} />
        </div>
      ))}
    </div>
  }
}