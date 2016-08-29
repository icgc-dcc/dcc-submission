import React from 'react';
import ReactDOM from 'react-dom';
import { AppContainer } from 'react-hot-loader';
import 'whatwg-fetch';

import './index.css';

import user from '~/user';

import routes from './routes';

// hardcode user to be logged in

user.name = 'admin';
user.token = 'YWRtaW46YWRtaW5zcGFzc3dk';
user.roles = ['admin'];
user.isLoggedIn = true;
window.user = user;

const rootEl = document.getElementById('root');

ReactDOM.render((
  <AppContainer>
    {routes}
  </AppContainer>
  ), rootEl
);

if (module.hot) {
  module.hot.accept('./routes', () => {
    // If you use Webpack 2 in ES modules mode, you can
    // use <App /> here rather than require() a <NextApp />.
    const nextRoutes = require('./routes');
    ReactDOM.render(
      <AppContainer>
         {nextRoutes}
      </AppContainer>,
      rootEl
    );
  });
}
