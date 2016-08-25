import React from 'react';
import { Router, Route, browserHistory, IndexRoute } from 'react-router'
import ReactDOM from 'react-dom';
import 'whatwg-fetch';

import App from './App';
import './index.css';

import Login from './Login/Login.js';

import user from '~/user.js';

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
    <Route name="App" path="/" component={App}>
      <IndexRoute component={user.isLoggedIn ? require('./Releases/Releases.js') : Login}/>
      <Route name="Login" path="login" component={Login}/>
      <Route name="Releases" path="releases" onEnter={requireAuth}>
        <IndexRoute name="ReleasesIndex" component={require('./Releases/Releases.js')}/>
        <Route name="Release" path=":releaseName">
          <IndexRoute name="ReleaseIndex" component={require('./Release/Release.js')}/>
          <Route name="Submission" path="submissions/:projectKey">
            <IndexRoute name="ReleaseIndex" component={require('./Submission/Submission.js')}/>
            <Route name="Report" path="report/:fileName">
              <IndexRoute name="ReportIndex" component={require('./Report/Report.js')}/>
            </Route>
          </Route>
        </Route>
      </Route>
    </Route>
  </Router>
  ), document.getElementById('root')
);
