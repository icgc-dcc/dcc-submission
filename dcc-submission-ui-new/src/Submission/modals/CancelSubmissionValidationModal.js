import React, { Component, PropTypes } from 'react';
import {observer} from 'mobx-react';
import Modal from 'react-modal';

@observer
class CancelSubmissionValidationModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
    onRequestSubmit: PropTypes.func.isRequired,
    projectName: PropTypes.string.isRequired,
  };

  render () {
    const { isOpen, onRequestClose, onRequestSubmit, projectName } = this.props;
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
            <h3>Cancel Submission Validation</h3>
          </div>
          <div className="modal-body">
            <div className="alert alert-info">
              This will cancel the validation on the <strong>{projectName}</strong> submission!
            </div>
          </div>
          <div className="modal-footer">
            <button className="m-btn grey-stripe" onClick={onRequestClose}>Close</button>
            <button
              type="submit"
              className="m-btn red"
              onClick={onRequestSubmit}
            >Cancel Submission Validation</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default CancelSubmissionValidationModal;