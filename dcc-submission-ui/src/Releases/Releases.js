import React, { Component } from 'react';
import {Link} from 'react-router';
import {observable, action, computed } from 'mobx';
import {observer} from 'mobx-react';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';
import Tooltip from 'rc-tooltip';
import moment from 'moment';

import ActionButton from '~/common/components/ActionButton/ActionButton';
import Status from '~/common/components/Status';
import { defaultTableOptions, defaultTableProps } from '~/common/defaultTableOptions';
import user from '~/user';

import RELEASE_STATES from '../Release/constants/RELEASE_STATES';
import PerformReleaseModal from '~/Release/modals/PerformReleaseModal';
import { performRelease } from '~/services/release';
import { fetchReleases } from '~/services/releases';

const releases = observable({
  isLoading: false,
  items: []
});

window.releases = releases;

releases.fetch = action('fetch releases', async function () {
  this.isLoading = true;
  const items = await fetchReleases();
  this.isLoading = false;
  this.items = items;
});

function releaseDateSortFunction(a, b, order, sortField) {
  const dateA = a[sortField] || new Date().getTime();
  const dateB = b[sortField] || new Date().getTime();
  return order === 'asc'
    ? dateA - dateB
    : dateB - dateA;
}

export default @observer
class Releases extends Component {
  @observable releaseToPerform;
  @computed get shouldShowPerformReleaseModal() {
    return !!this.releaseToPerform;
  }
  handleClickPerformRelease = (release) => {
    this.releaseToPerform = release;
  }
  closePerformReleaseModal = () => { this.releaseToPerform = null };
  handleRequestSubmitForRelease = async ({nextReleaseName}) => {
    await performRelease({nextReleaseName});
    this.closePerformReleaseModal();
    releases.fetch();
  }

  componentWillMount () {
    releases.fetch();
    this._pollInterval = global.setInterval(releases.fetch, require('~/common/constants/POLL_INTERVAL'));
  }

  componentWillUnmount () {
    global.clearInterval(this._pollInterval);
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
      <div className="container">
        <h1>Releases</h1>
        <BootstrapTable
          {...defaultTableProps}
          data={items}
          keyField='name'
          striped={true}
          pagination={true}
          ignoreSinglePage={true}
          search={items.length > tableOptions.thresholdToShowSearch}
          options={tableOptions}
        >
          <TableHeaderColumn
            key="name"
            dataField="name"
            dataSort={true}
            dataFormat={ releaseName => (
              <Link to={`/releases/${releaseName}`}>
                {releaseName}
              </Link>
            )}
          >Name</TableHeaderColumn>

          <TableHeaderColumn
            key="state"
            dataField="state"
            dataSort={true}
            dataFormat={(state) => <Status statusCode={state}/>}
          >State</TableHeaderColumn>

          <TableHeaderColumn
            key="releaseState"
            dataField='releaseDate'
            dataSort={true}
            sortFunc={releaseDateSortFunction}
            dataFormat={(date) => date
              ? (
                <Tooltip
                  mouseLeaveDelay={0}
                  overlay={<span>{moment(date).format('h:mm:ss a')}</span>}
                >
                  <span>{moment(date).format('MMMM Do YYYY')}</span>
                </Tooltip>
              )
              : 'Unreleased'}
          >Release Date</TableHeaderColumn>

        </BootstrapTable>
        <PerformReleaseModal
          isOpen={this.shouldShowPerformReleaseModal}
          onRequestSubmit={this.handleRequestSubmitForRelease}
          onRequestClose={this.closePerformReleaseModal}
          releaseName={this.releaseToPerform ? this.releaseToPerform.name : ''}
        />
      </div>
    )
  }
}
