import React, { Component } from 'react';

import {observer} from 'mobx-react';
import {Link} from 'react-router';

import user from '~/user.js';

import './Header.css';

@observer
class Header extends Component {

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
                    <Link to="/admin" className="link">Admin</Link>
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
              { user.isLoggedIn && (
                <ul className="nav navbar-nav navbar-right">
                  <li></li>
                  <li>
                    <a className="username">{user.username}</a>
                  </li>
                  <li>
                    <a style={{cursor: 'pointer'}} onClick={() => user.logout()}>Logout</a>
                  </li>
                </ul>
              )}
            </div>
          </div>
        </nav>
      </div>
    );
  }
}

export default Header;