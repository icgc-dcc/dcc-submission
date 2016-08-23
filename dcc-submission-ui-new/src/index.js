import React from 'react';
import { Router, Route, browserHistory, IndexRoute } from 'react-router'
import ReactDOM from 'react-dom';
import {observe} from 'mobx';
import 'whatwg-fetch';

import App from './App';
import './index.css';

import Login from './Login/Login.js';
import Releases from './Releases/Releases.js';

import user from '~/user.js';

observe(user, change => {
  if (change.name === 'isLoggedIn' && change.oldValue === false && change.newValue === true) {
    browserHistory.push('/releases')
  }
})

function requireAuth(nextState, replace) {
  if (!user.isLoggedIn) {
    replace({
      pathname: '/login',
      state: { nextPathname: nextState.location.pathname }
    })
  }
}

// hardcode user to be logged in

user.name = 'admin';
user.token = 'YWRtaW46YWRtaW5zcGFzc3dk';
user.roles = ['admin'];
user.isLoggedIn = true;

ReactDOM.render((
  <Router history={browserHistory}>
    <Route path="/" component={App}>
      <IndexRoute component={Login}/>
      <Route path="login" component={Login}/>
      <Route path="releases" component={Releases} onEnter={requireAuth}/>
    </Route>
  </Router>
  ), document.getElementById('root')
);
