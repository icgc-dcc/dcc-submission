import React, { Component } from 'react';
import Breadcrumbs from 'react-breadcrumbs';
import './App.css';

import {observer} from 'mobx-react';
import Header from './Header/Header';

import user from '~/user';

@observer
class App extends Component {
  render() {
    const fullView = (
      <div className="App container">
        <Header/>
        <Breadcrumbs
          routes={this.props.routes}
          params={this.props.params}
          excludes={['App']}
        />
        {this.props.children}
      </div>
    );
    const minimalView = (
      <div className="App container">
        { this.props.children }
      </div>
    );
    return user.isLoggedIn ? fullView : minimalView;
  }
}

export default App;
