import React, { Component } from 'react';
import Breadcrumbs from 'react-breadcrumbs';
import './App.css';

import {observer} from 'mobx-react';
import Header from './Header/Header';
import Footer from './Footer/Footer';

import user from '~/user';
import systems from '~/systems';


@observer
class App extends Component {
  componentWillMount () {
    this._pollInterval = global.setInterval(function () {
      if (user.isLoggedIn) systems.fetch();
    }, require('~/common/constants/POLL_INTERVAL'));
  }

  componentWillUnmount () {
    global.clearInterval(this._pollInterval);
  }
  render() {
    const fullView = (
      <div className="App">
        <Header/>
        { systems.isReleaseLocked ? (
          <div className="lock-message alert alert-danger">Release is locked. No validations or file transfers are permitted</div>
        ) : null }
        <div className="container">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.params}
            excludes={['App']}
            itemClass="breadcrumb-item"
            separator={<span className="breadcrumb-separator"><i className="fa fa-chevron-right"/></span>}
          />
        </div>
        {this.props.children}
        <Footer/>
      </div>
    );
    const minimalView = (
      <div className="App">
        { this.props.children }
      </div>
    );
    return user.isLoggedIn ? fullView : minimalView;
  }
}

export default App;
