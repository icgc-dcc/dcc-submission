import React from 'react';
import { Router, Route, browserHistory, IndexRoute } from 'react-router'

import user from '~/user.js';

function requireAuth(nextState, replace) {
  if (!user.isLoggedIn) {
    replace({
      pathname: '/login',
      state: { nextPathname: nextState.location.pathname }
    })
  }
}

const routes = (
    <Router history={browserHistory}>
      <Route name="App" path="/" component={require('./App.js')}>
        <IndexRoute component={require('./Releases/Releases.js')} onEnter={requireAuth} />
        <Route name="Login" path="login" component={require('./Login/Login.js')}/>
        <Route name="Calendar" path="calendar" component={require('./Calendar/Calendar.js')}/>
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
)

export default routes;