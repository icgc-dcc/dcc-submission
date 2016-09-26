import React, { Component } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import moment from 'moment';
import pluralize from 'pluralize';

import systems from '~/systems';
import ActionButton from '~/common/components/ActionButton/ActionButton';
import Tooltip from 'rc-tooltip';
import { sortBy } from 'lodash';

import ReleaseModel from '~/Release/ReleaseModel';

import { fetchQueue, clearQueue } from '~/services/nextRelease/queue';
import { fetchUsers, unlockUser } from '~/services/users';

import './Admin.css';

function User({name, isLocked, onRequestUnlock}) {
  const user = <span className="user normal label label-default">{name}</span>;
  const lockedUser = (
    <Tooltip
      mouseLeaveDelay={0}
      overlay={<span>Click to unlock</span>}
      placement="bottom"
    >
      <span
        className="user locked btn btn-xs btn-warning"
        onClick={onRequestUnlock}
      >
        <i className="fa fa-lock"/>
        <i className="fa fa-unlock"/>
        {name}
      </span>
    </Tooltip>
  );
  return isLocked ? lockedUser : user;
}

@observer
class Admin extends Component {
  @observable nextRelease;
  @observable validationQueue = [];

  @observable errorMessages = {
    lock: '',
    release: '',
    user: '',
  };

  @observable users = [];

  async componentWillMount() {
    systems.fetch();
    this.nextRelease = new ReleaseModel();
    this.nextRelease.fetch({shouldFetchUpcomingRelease: true});
    this.loadValidationQueue();
    this.users = await fetchUsers();
  }

  loadValidationQueue = async () => {
    this.validationQueue = await fetchQueue();
  };

  clearValidationQueue = async () => {
    await clearQueue();
    await this.loadValidationQueue();
  }

  handleClickLockUnlockButton = async () => {
    try {
      if (systems.isReleaseLocked) {
        await systems.unlockRelease();
      } else {
        await systems.lockRelease();
      }
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessages.lock = e.message;
    }
  };

  handleClickPerformRelease = async () => {
    if (!window.confirm('Do you really want to perform a release?')) {
      return;
    }
    try {
      await this.nextRelease.performRelease();
      this.handlePerformReleaseSuccess();
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessages.release = e.message;
    }
  };

  handlePerformReleaseSuccess = () => {
    this.nextRelease.fetch({shouldFetchUpcomingRelease: true});
  }

  handleClickUnlockUser = async (username) => {
    this.errorMessages.user = '';
    try {
      await unlockUser(username);
      this.users = await fetchUsers();
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessages.user = e.message;
    }
  };
  render () {
    const lockMessage = systems.isReleaseLocked 
      ? `Click on "Unlock Submissions" to allow uploading and validation on non-admin accounts.`
      : `Click on "Lock Submissions" to prevent uploading and validation on non-admin accounts.`;

    const lockUnlockButton = (
      <Tooltip
            mouseLeaveDelay={0}
            overlay={<span>{lockMessage}</span>}
            placement="bottom"
          >
            { systems.isReleaseLocked
              ? (
                <button
                  className="lock-unlock unlock btn btn-xs btn-primary hover-fade-in"
                  onClick={this.handleClickLockUnlockButton}
                >
                  <i className="fa fa-unlock"/>
                  Unlock
                </button>
              )
              : (
                <button
                  className="lock-unlock lock btn btn-xs btn-danger hover-fade-in"
                  onClick={this.handleClickLockUnlockButton}
                >
                  <i className="fa fa-lock"/>
                  Lock
                </button>
                )
            }
          </Tooltip>
    );
    const releaseNowButton = (
      <ActionButton
        onClick={() => this.handleClickPerformRelease()}
        className={`btn btn-xs release-now-btn hover-fade-in`}
      >
        <i className="fa fa-rocket"/>
        Release Now
      </ActionButton>
    );

    return (
      <div className="Admin container">
          <h1>Admin</h1>
          <header className="heading">
            { (this.errorMessages.lock || this.errorMessages.release) ? (
              <div className="alert alert-danger">
                {this.errorMessages.lock}
                {this.errorMessages.release}
              </div>
            ) : ''}
            <small>Upcoming Release</small>
            <h2>
              <span className="release-name">{this.nextRelease.name}</span>
              <span className="terms creation-time">
                <span className="terms__term">Created</span>
                <span className="terms__value">{moment(this.nextRelease.created, 'x').fromNow()}</span>
              </span>
              {lockUnlockButton}
              {releaseNowButton}
            </h2>
          </header>

          <h2>
            Validation
            {
              this.validationQueue.length
              ? (
                <button
                  className={`btn btn-xs btn-danger hover-fade-in clear-queue-btn`}
                  onClick={this.clearValidationQueue}
                  disabled={!this.validationQueue.length}
                >
                  <i className="fa fa-trash"/>
                  Clear Queue
                </button>
              ) : ''
            }
          </h2>


          <ul className="validation-list">
          {
            this.validationQueue.length
              ? this.validationQueue.map(projectKey => (
                <li className="validation-item" key={projectKey}>
                  <span className="">{projectKey}</span>
                </li>
              ))
              : <li>No files are being validated</li>
          }
          </ul>

          <h2>SFTP</h2>
          <ul>
            <li>
              There { pluralize('is', systems.activeSftpSessions) } <strong>{ systems.activeSftpSessions }</strong> active SFTP { pluralize('session', systems.activeSftpSessions) }
              <ul>
                {systems.userSessions.map(user => (
                  <li className="sftp-item" key={user.userName}>
                    <span className="label label-default sftp-username">{user.userName}</span>
                    <span className="label label-primary sftp-filename">{user.ioSessionMap.fileTransfer
                      ? (
                        <span>
                          <span style={{fontWeight: 700}}>transferring: </span> 
                          <span style={{fontWeight: 600}}>{user.ioSessionMap.fileTransfer}</span>
                        </span>
                        ) : 'idle' }
                    </span>
                  </li>
                ))}
              </ul>
            </li>
          </ul>

          <h2>Users</h2>
          { this.errorMessages.user ? (
              <div className="alert alert-danger">
                {this.errorMessages.user}
              </div>
            ) : ''}
          <div className="user-list">
            {
              sortBy(this.users, 'name')
              .filter(user => user.locked)
              .map( user => (
                <User
                  key={user.username}
                  name={user.name}
                  isLocked={user.locked}
                  onRequestUnlock={() => this.handleClickUnlockUser(user.username)}
                />
              ))
            }
          </div>
          <div className="user-list">
            {
              sortBy(this.users, 'name')
              .filter(user => !user.locked)
              .map( user => (
                <User
                  key={user.username}
                  name={user.name}
                  isLocked={user.locked}
                  onRequestUnlock={() => this.handleClickUnlockUser(user.username)}
                />
              ))
            }
          </div>
      </div>
    );
  }
}

export default Admin;