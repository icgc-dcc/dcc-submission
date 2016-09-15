import React, { Component, PropTypes } from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';
import Modal from 'react-modal';

@observer
class PerformReleaseModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
    onRequestSubmit: PropTypes.func.isRequired,
    releaseName: PropTypes.string.isRequired,
  };

  @observable nextReleaseName;
  @observable errorMessage;

  handleClickSubmit = async () => {
    try {
      await this.props.onRequestSubmit({nextReleaseName: this.nextReleaseName});
    } catch (e) {
      this.errorMessage = e.message;
    }
  };

  handleClickClose = () => {
    this.errorMessage = '';
    this.props.onRequestClose();
  };

  render () {
    const {isOpen, releaseName} = this.props;
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
            <h3>Complete Release</h3>
          </div>
          <div className="modal-body">
            {this.errorMessage ? (
              <div className="alert alert-danger">
                {this.errorMessage}
              </div>
            ) : ''}
            <div className="alert alert-info">
              Once you have confirmed completing the release <strong>"{releaseName}"</strong> it will be marked as COMPLETED and all submissions that are SIGNED OFF will be processed and included in this release. Please be certain before you continue.
            </div>
            <br/>
            <form className="form-horizontal">
              <div className="form-group">
                <label className="col-sm-4 text-right" htmlFor="nextRelease">Next Release Name</label>
                <div className="col-sm-8">
                  <input
                    autoFocus="autofocus"
                    type="text"
                    className="form-control"
                    placeholder="Enter next release name"
                    id="nextRelease"
                    onChange={(e) => { this.nextReleaseName = e.target.value }}
                  />
                </div>
              </div>
            </form>
          </div>
          <div className="modal-footer">
            <button className="btn btn-default" onClick={this.handleClickClose}>Close</button>
            <button className="btn release-now-btn" onClick={this.handleClickSubmit} style={{marginLeft: 4}}>
              <i className="fa fa-rocket" style={{marginRight: 4}}/>Release
            </button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default PerformReleaseModal;
