import React, { Component } from 'react';
// import logo from './logo.svg';
import './App.css';

import Breadcrumbs from './common/components/Breadcrumbs/Breadcrumbs';

class App extends Component {
  render() {
    return (
      <div className="App">
        <Breadcrumbs pathName={this.props.location.pathname}/>
        {this.props.children}
      </div>
    );
  }
}

export default App;
