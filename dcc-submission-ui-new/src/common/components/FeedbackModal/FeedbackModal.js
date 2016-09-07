import React, { Component, PropTypes } from 'react';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import Modal from 'react-modal';
import isEmail from 'is-email';

import { fetchHeaders } from '~/utils';

function submitUserFeedback({address, subject, message}) {
  return fetch(`/ws/users/self`, {
    method: 'POST',
    headers: fetchHeaders.get(),
    body: JSON.stringify({
      email: address,
      message,
      subject,
    }),
  });
}

@observer
class FeedbackModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
  };

  @observable email = {
    address: '',
    subject: '',
    message: '',
  };

  @observable errorMessage = '';

  @computed get areAllFieldsFilled() {
    const {address, subject, message} = this.email;
    return !!(address.trim() && subject.trim() && message.trim());
  }

  @computed get isEmailValid() {
    return isEmail(this.email.address);
  }

  handleClickSubmit = async () => {
    try {
      if (!this.areAllFieldsFilled) {
        throw new Error('Please fill out all fields.');
      }
      if (!this.isEmailValid) {
        throw new Error('Please enter a valid email address.');
      }
      await submitUserFeedback(this.email);
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
            <h3>Feedback Form</h3>
          </div>
          <div className="modal-body">
            {this.errorMessage ? (
              <div className="alert alert-danger">
                {this.errorMessage}
              </div>
            ) : ''}
            <form className="form-horizontal">
              <div className="form-group">
                <label className="col-xs-2 control-label" htmlFor="email">Email</label>
                <div className="col-xs-10">
                  <input
                    type="text"
                    name="email"
                    autoFocus={true}
                    placeholder="Your Email Address"
                    className="form-control"
                    onChange={e => {this.email.address = e.target.value}}
                  />
                </div>
              </div>
              <div className="form-group">
                <label className="col-xs-2 control-label" htmlFor="subject">Subject</label>
                <div className="col-xs-10">
                  <input
                    type="text"
                    name="subject"
                    placeholder="Message Subject"
                    className="form-control"
                    onChange={e => {this.email.subject = e.target.value}}
                  />
                </div>
              </div>
              <div className="form-group">
                <label className="col-xs-2 control-label" htmlFor="message">Message</label>
                <div className="col-xs-10 controls">
                  <textarea
                    name="message"
                    placeholder="Please write a comment or describe a problem"
                    className="form-control"
                    rows="5"
                    onChange={e => {this.email.message = e.target.value}}
                  />
                </div>
              </div>
            </form>
          </div>
          <div className="modal-footer">
            <button className="m-btn grey-stripe" onClick={this.handleClickClose}>Close</button>
            <button type="submit" className="m-btn green" onClick={this.handleClickSubmit}>Send Feedback</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default FeedbackModal;
