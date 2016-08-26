import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import BootstrapTable from 'reactjs-bootstrap-table';
import { fetchHeaders } from '~/utils';
// import ActionButton from '~/common/components/ActionButton/ActionButton';
import user from '~/user';

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

export default @observer
class Releases extends Component {
  componentWillMount () {
    releases.fetch();
  }

  render () {
    return <div>
      Releases!
      <BootstrapTable
        data={releases.items}
        headers={true}
        columns={[
          {
            name: 'name',
            display: 'Name',
            sort: true,
            renderer: release => (
              <Link to={`/releases/${release.name}`}>
                {release.name}
              </Link>
            ),
          },
          { name: 'state', display: 'State' },
          { name: 'releaseDate', display: 'Release Date', renderer: release => release.releaseDate || 'Unreleased'},
          { name: 'projects', display: 'Projects', renderer: release => release.submissions.length},

          // only admin users have actions, if not admin then hide column
          ...[user.isAdmin && { name: 'actions', display: 'Actions', renderer: release => (
            <div
              onClick={() => console.log('release')}
              className="m-btn green-stripe mini"
            >
              Release Now
            </div>
          )}]
        ]}
      />
    </div>
  }
}