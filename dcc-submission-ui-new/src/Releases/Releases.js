import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

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
    const tableOptions = {
      sizePerPage: 10,
      paginationShowsTotal: true,
    }
    const items = releases.items;

    return <div>
      Releases!
      <BootstrapTable
        data={items}
        keyField='name'
        striped={true}
        pagination={true}
        ignoreSinglePage={true}
        hideSizePerPage={true}
        search={items.length > tableOptions.sizePerPage}
        options={tableOptions}
      >
        <TableHeaderColumn
          dataField='name'
          dataSort={true}
          dataFormat={ releaseName => (
            <Link to={`/releases/${releaseName}`}>
              {releaseName}
            </Link>
          )}
        >Name</TableHeaderColumn>
        <TableHeaderColumn
          dataField='state'
          dataSort={true}
        >State</TableHeaderColumn>
        <TableHeaderColumn
          dataField='releaseDate'
          dataSort={true}
          dataFormat={(releaseDate) => releaseDate || 'Unreleased'}
        >State</TableHeaderColumn>
        <TableHeaderColumn
          hidden={!user.isAdmin}
          dataFormat={(cell, release) => (
            <div
              onClick={() => console.log('release')}
              className="m-btn green-stripe mini"
            >
              Release Now
            </div>
          )}
        >Actions</TableHeaderColumn>
      </BootstrapTable>
    </div>
  }
}