import React, { Component } from 'react';

import {observer} from 'mobx-react';

import user from '~/user.js';

@observer
class Login extends Component {

  state = {
    username: '',
    password: '',
  };

  submit = async () => {
    const { username, password } = this.state;
    user.login(username, password);
  }

  render() {
    return (
      <div className="Login">
        Login
        <label>
          Username:
          <input
            value={this.state.username}
            onChange={e => this.setState({username: e.target.value})}
          />
        </label>
        <label>
          Password:
          <input
            value={this.state.password}
            onChange={e => this.setState({password: e.target.value})}
          />
        </label>
        <button onClick={this.submit}>Login</button>
        <pre>
        {JSON.stringify(user, null, '  ')}
        </pre>
      </div>
    );
  }
}

export default Login;