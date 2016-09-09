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
      <div className="App">
        <Header/>
        <div className="container">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.params}
            excludes={['App']}
          />
        </div>
        {this.props.children}
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
