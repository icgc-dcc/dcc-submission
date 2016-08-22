import React from 'react';
import { Router, Route, browserHistory, IndexRoute } from 'react-router'
import ReactDOM from 'react-dom';
import 'whatwg-fetch';

import App from './App';
import './index.css';

import Login from './Login/Login.js';

ReactDOM.render((
  <Router history={browserHistory}>
    <Route path="/" component={App}>
      <IndexRoute component={Login}/>
      <Route path="Login" component={Login}/>
    </Route>
  </Router>
  ), document.getElementById('root')
);
