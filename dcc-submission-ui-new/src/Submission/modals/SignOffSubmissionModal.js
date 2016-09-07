import React, { Component, PropTypes } from 'react';
import {observer} from 'mobx-react';
import Modal from 'react-modal';

@observer
class ValidateModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
    onRequestSubmit: PropTypes.func.isRequired,
  };

  render () {
    const { isOpen, onRequestClose, onRequestSubmit, projectName, projectKey } = this.props;
    return (
      <Modal
          className={`Modal modal-dialog`}
          isOpen={isOpen}
          onRequestClose={onRequestClose}
          closeTimeoutMS={250}
          shouldCloseOnOverlayClick={true}
        >
        <div className="modal-container">
          <div className="modal-header">
            <button
              type="button"
              className="close"
              aria-label="Close"
              onClick={onRequestClose}
            >
              <span aria-hidden="true">&times;</span>
            </button>
            <h3>Sign Off Submission</h3>
          </div>
          <div className="modal-body">
            <div className="alert alert-info">
              Once you have confirmed signing off on the submission for the project "{projectName}" <em>({projectKey})</em> 
              it will be marked as SIGNED OFF and will be processed and included in this release. 
              You will <b>not</b> be able to change this submission. 
              Please be certain before you continue.
            </div>
          </div>
          <div className="modal-footer">
            <button className="m-btn grey-stripe" onClick={onRequestClose}>Close</button>
            <button
              type="submit"
              className="m-btn green"
              onClick={onRequestSubmit}
            >Sign Off Submission</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default ValidateModal;