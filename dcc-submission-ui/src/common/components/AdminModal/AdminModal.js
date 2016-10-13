import React, { Component, PropTypes } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import Modal from 'react-modal';
import systems from '~/systems';

@observer
class AdminModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
  };

  async componentWillReceiveProps({isOpen}) {
    systems.fetch();
  }

  handleClickSubmit = async () => {
    console.log(systems);
    try {
      if (systems.isReleaseLocked) {
        await systems.unlockRelease()
      } else {
        await systems.lockRelease();
      }
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
    const message = systems.isReleaseLocked 
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
                <li>There are <strong>{ systems.activeSftpSessions }</strong> active SFTP sessions:<br/>
                    <ul>
                      {systems.userSessions.map(user => (
                        <li key={user.userName}><span className="label label-default">{user.userName}</span></li>
                      ))}
                    </ul>
                </li>
            </ul>
          </div>
          <div className="modal-footer">
            <button className="btn btn-default" onClick={this.handleClickClose}>Close</button>
            <button
              type="submit"
              className={`btn ${systems.isReleaseLocked ? 'btn-primary' : 'btn-danger'}`}
              onClick={this.handleClickSubmit}
            >{systems.isReleaseLocked ? 'Unlock' : 'Lock'} Submissions</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default AdminModal;
