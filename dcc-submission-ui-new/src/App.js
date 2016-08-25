import React, { Component } from 'react';
import Breadcrumbs from 'react-breadcrumbs';
// import logo from './logo.svg';
import './App.css';

// import Breadcrumbs from './common/components/Breadcrumbs/Breadcrumbs';

class App extends Component {
  render() {
    return (
      <div className="App">
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
