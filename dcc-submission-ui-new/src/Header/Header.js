import React, { Component } from 'react';

import {observable, action} from 'mobx';
import {observer} from 'mobx-react';
import {Link} from 'react-router';

import user from '~/user.js';

import FeedbackModal from '~/common/components/FeedbackModal/FeedbackModal';
import AdminModal from '~/common/components/AdminModal/AdminModal';

import './Header.css';

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
        <nav className="navbar navbar-default">
          <div className="container-fluid">
            <div className="navbar-header">
              <Link className="navbar-brand main-logo" to="/">
                <img
                  alt="ICGC"
                  className="icgc-logo"
                  src={require('~/assets/images/logo-icgc.png')}
                />
                <span className="icgc-text">ICGC</span>
                <span className="icgc-subtext">Data Submission</span>
              </Link>
            </div>

            <div className="collapse navbar-collapse">
              <ul className="nav navbar-nav">
                <li>
                  <Link to={`/releases`}>Releases</Link>
                </li>
                <li>
                  <Link to={`/calendar`}>Calendar</Link>
                </li>
                { user.isAdmin &&
                  <li>
                    <a onClick={this.handleRequestShowAdmin}>Admin</a>
                  </li>
                }
                <li className="dropdown">
                  <a href="#" className="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Resources <span className="caret"></span></a>
                  <ul className="dropdown-menu">
                    <li><a href="http://docs.icgc.org/" target="_blank">Documentation</a></li>
                    <li><a href="http://docs.icgc.org/dictionary/viewer/" target="_blank">Dictionary Viewer</a></li>
                  </ul>
                </li>
              </ul>
              <ul className="nav navbar-nav navbar-right">
                <li></li>
                <li>
                  <a className="username">{user.username}</a>
                </li>
                <li>
                  <a style={{cursor: 'pointer'}} onClick={() => user.logout()}>Logout</a>
                </li>
              </ul>
            </div>
          </div>
        </nav>
        
        <button onClick={this.handleRequestShowFeedback}>feedback</button>

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