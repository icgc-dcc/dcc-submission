import React, { Component } from 'react';

async function login (username, password) {
  const accessToken = global.btoa(`${username}:${password}`);
  return await fetch('/ws/users/self', {
      headers: {
        Authorization: `X-DCC-Auth ${accessToken}`,
        Accept: 'application/json'
      }
    }); 
}

export default class extends Component {

  state = {
    username: '',
    password: '',
  };

  submit = async () => {
    const { username, password } = this.state;
    const response = await login(username, password);
    
    if (response.status === 200) {
      const user = await response.json();
      this.handleLogin(user);
    } else if (response.status === 401) {
      throw new Error('Incorrect username or password');
    } else {
      throw new Error('Login failed');
    }
  }

  handleLogin = (user) => {
    // TODO: save to global state, redirect to post-login home page
    console.log('login success', user);
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
        <button onClick={this.submit}/>
      </div>
    );
  }
}
