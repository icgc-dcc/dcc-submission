import React, { Component } from 'react';

import {observable, action} from 'mobx';
import {observer} from 'mobx-react';

import systemInfo from '~/systemInfo';
import FeedbackModal from '~/common/components/FeedbackModal/FeedbackModal';

import './Footer.css';

@observer
class Header extends Component {
  @observable shouldShowFeedback = false;
  @action handleRequestHideFeedback = () => {
    this.shouldShowFeedback = false;
  }
  @action handleRequestShowFeedback = () => {
    this.shouldShowFeedback = true;
  }

  render () {
    const {version, commitId} = systemInfo;
    return (
      <div className="Footer row">
        <a className="col-sm-2 oicr-logo" href="http://oicr.on.ca" target="_blank">
          <img
            alt="OICR"
            width={112}
            src={require('~/assets/images/logo-oicr.png')}
          />
        </a>
        <div className="col-sm-8 text-center">
          <p>
            <a href="http://icgc.org" target="_blank">International Cancer Genome Consortium</a> <br/>
            © 2014 <br/>
            All Rights reserved
          </p>
        </div>
        <div className="col-sm-2 text-right">
          <p>
            {version} (<a target="_blank" href={`https://github.com/icgc-dcc/dcc-submission/commit/${commitId}`}>{commitId}</a>)
          </p>
          <p>
            <a className="link" onClick={this.handleRequestShowFeedback}>Feedback?</a>
          </p>
        </div>
        <FeedbackModal
          isOpen={this.shouldShowFeedback}
          onRequestClose={this.handleRequestHideFeedback}
        />
      </div>
    );
  }
}

export default Header;