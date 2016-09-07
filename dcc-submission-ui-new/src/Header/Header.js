import React, { Component } from 'react';

import {observable, action} from 'mobx';
import {observer} from 'mobx-react';

import user from '~/user.js';

import FeedbackModal from '~/common/components/FeedbackModal/FeedbackModal';
import AdminModal from '~/common/components/AdminModal/AdminModal';

@observer
class Header extends Component {
  @observable shouldShowFeedback = false;
  @action handleRequestHideFeedback = () => {
    this.shouldShowFeedback = false;
  }
  @action handleRequestShowFeedback = () => {
    this.shouldShowFeedback = true;
  }

  @observable shouldShowAdmin = false;
  @action handleRequestHideAdmin = () => {
    this.shouldShowAdmin = false;
  }
  @action handleRequestShowAdmin = () => {
    this.shouldShowAdmin = true;
  }

  render() {
    return (
      <div className="Header">
        Header
        <button onClick={this.handleRequestShowFeedback}>feedback</button>
        <button onClick={this.handleRequestShowAdmin}>admin</button>
        username:{user.username}
        <a onClick={() => user.logout()}>Logout</a>

        <FeedbackModal
          isOpen={this.shouldShowFeedback}
          onRequestClose={this.handleRequestHideFeedback}
        />
        <AdminModal
          isOpen={this.shouldShowAdmin}
          onRequestClose={this.handleRequestHideAdmin}
        />
      </div>
    );
  }
}

export default Header;