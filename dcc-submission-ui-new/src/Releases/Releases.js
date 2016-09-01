import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, runInAction} from 'mobx';
import {observer} from 'mobx-react';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';
import moment from 'moment';

import defaultTableOptions from '~/common/defaultTableOptions';
import { fetchHeaders } from '~/utils';
import user from '~/user';
import ActionButton from '~/common/components/ActionButton/ActionButton';

import { openModal, closeModal } from '~/App';
import CompleteReleaseModal from '~/common/components/modals/CompleteRelease/CompleteRelease';

const releases = observable({
  isLoading: false,
  items: []
});

window.releases = releases;

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

function showCompleteReleaseModal({releaseName, onSuccess}) {
  openModal(<CompleteReleaseModal
    releaseName={releaseName}
    onClickClose={closeModal}
    onSuccess={onSuccess}
    />);
}

function handleSuccessfulRelease() {
  closeModal();
  releases.fetch();
}

function releaseDateSortFunction(a, b, order, sortField) {
  const dateA = a[sortField] || new Date().getTime();
  const dateB = b[sortField] || new Date().getTime();
  return order === 'asc'
    ? dateA - dateB
    : dateB - dateA;
}

export default @observer
class Releases extends Component {
  componentWillMount () {
    releases.fetch();
  }

  render () {
    const items = releases.items;

    const tableOptions = {
      ...defaultTableOptions,
      noDataText: releases.isLoading ? 'Loading...' : 'There is no data to display',
      defaultSortName: 'releaseDate',
      defaultSortOrder: 'desc',
    };

    return (
      <div>
        <BootstrapTable
          data={items}
          keyField='name'
          striped={true}
          pagination={true}
          ignoreSinglePage={true}
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
            sortFunc={releaseDateSortFunction}
            dataFormat={(date) => date ? moment(date).format('MMMM Do YYYY, h:mm:ss a') : 'Unreleased'}
          >Release Date</TableHeaderColumn>
          <TableHeaderColumn
            hidden={!user.isAdmin}
            dataFormat={(cell, release) => (
              release.state === 'OPENED'
              ? <ActionButton
                onClick={() => showCompleteReleaseModal({releaseName: release.name, onSuccess: handleSuccessfulRelease})}
                className="m-btn mini green-stripe"
              >
                Release Now
              </ActionButton>
              : ''
            )}
          >Actions</TableHeaderColumn>
        </BootstrapTable>
      </div>
    )
  }
}
