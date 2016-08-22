import React from 'react';
import { Router, Route, browserHistory } from 'react-router'
import ReactDOM from 'react-dom';
import App from './App';
import './index.css';

ReactDOM.render((
  <Router history={browserHistory}>
    <Route path="/" component={App}/>
  </Router>
  ), document.getElementById('root')
);
