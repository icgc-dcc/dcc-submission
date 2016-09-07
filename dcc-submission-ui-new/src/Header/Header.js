import React, { Component } from 'react';

import {observable, action} from 'mobx';
import {observer} from 'mobx-react';

import user from '~/user.js';

import FeedbackModal from '~/common/components/FeedbackModal/FeedbackModal';

@observer
class Header extends Component {
  @observable shouldShowFeedback = false;
  @action handleRequestHideFeedback = () => {
    this.shouldShowFeedback = false;
  }
  @action handleRequestShowFeedback = () => {
    console.log('show feedback');
    this.shouldShowFeedback = true;
  }

  componentWillMount() {

  }

  render() {
    return (
      <div className="Header">
        Header
        <button onClick={this.handleRequestShowFeedback}>feedback</button>
        <FeedbackModal
          isOpen={this.shouldShowFeedback}
          onRequestClose={this.handleRequestHideFeedback}
        />
      </div>
    );
  }
}

export default Header;