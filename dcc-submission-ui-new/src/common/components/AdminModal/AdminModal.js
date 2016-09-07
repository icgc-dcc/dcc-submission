import React, { Component, PropTypes } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import Modal from 'react-modal';

import { fetchHeaders } from '~/utils';

export async function fetchSystemStatus () {
  const response = await fetch(`/ws/systems`, {
    headers: fetchHeaders.get()
  });
  return await response.json();
}

function setSystemSftpStatus({sftpEnabled}) {
  return fetch(`/ws/systems`, {
    method: 'PATCH',
    headers: fetchHeaders.get(),
    body: JSON.stringify({
      active: sftpEnabled,
    }),
  });
}

@observer
class AdminModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
  };

  @observable sftpEnabled = undefined;
  @observable activeSftpSessions = 0;
  @observable userSessions = [];

  async componentWillReceiveProps({isOpen}) {
    if (isOpen) {
      const responseData = await fetchSystemStatus();
      Object.assign(this, responseData);
    }
  }

  handleClickSubmit = async () => {
    try {
      await setSystemSftpStatus({sftpEnabled: !this.sftpEnabled});
      this.props.onRequestClose();
    } catch (e) {
      console.log('caught an error', e.message);
      this.errorMessage = e.message;
    }
  };

  handleClickClose = () => {
    this.errorMessage = '';
    this.props.onRequestClose();
  };

  render () {
    const {isOpen} = this.props;
    const message = this.sftpEnabled 
      ? `Click on "Lock Submissions" to prevent uploading and validation on non-admin accounts.`
      : `Click on "Unlock Submissions" to allow uploading and validation on non-admin accounts.`;
    return (
      <Modal
        className={`Modal modal-dialog`}
        isOpen={isOpen}
        onRequestClose={this.handleClickClose}
        closeTimeoutMS={250}
        shouldCloseOnOverlayClick={true}
      >
        <div className="modal-content">
          <div className="modal-header">
            <button
              type="button"
              className="close"
              aria-label="Close"
              onClick={this.handleClickClose}
            >
              <span aria-hidden="true">&times;</span>
            </button>
            <h3>Admin</h3>
          </div>
          <div className="modal-body">
            {this.errorMessage ? (
              <div className="alert alert-danger">
                {this.errorMessage}
              </div>
            ) : ''}
            <div>{message}</div>
            <ul>
                <li>There are <strong>{ this.activeSftpSessions }</strong> active SFTP sessions:<br/>
                    <ul>
                      {this.userSessions.map(user => (
                        <li><span class="label label-badge">{user.username}</span></li>
                      ))}
                    </ul>
                </li>
            </ul>
          </div>
          <div className="modal-footer">
            <button className="m-btn grey-stripe" onClick={this.handleClickClose}>Close</button>
            <button
              type="submit"
              className={`m-btn ${this.sftpEnabled ? 'red' : 'green'}`}
              onClick={this.handleClickSubmit}
            >{this.sftpEnabled ? 'Lock' : 'Unlock'} Submissions</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default AdminModal;
