import React, { Component } from 'react';
import Breadcrumbs from 'react-breadcrumbs';
import './App.css';

import {observer} from 'mobx-react';

import Header from './Header/Header';

@observer
class App extends Component {
  render() {
    return (
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
  }
}

export default App;
