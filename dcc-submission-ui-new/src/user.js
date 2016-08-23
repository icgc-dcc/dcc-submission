import {observable, action, runInAction} from 'mobx';

function generateToken(username, password) {
  return global.btoa(`${username}:${password}`);
}

const user = observable({
  username: '',
  token: '',
  roles: [],
  isLoggedIn: false,
  isLoggingIn: false,
});

user.login = action('login', async function (username, password) {
  this.isLoggingIn = true;
  const token = generateToken(username, password);
  const response = await fetch('/ws/users/self', {
      headers: {
        Authorization: `X-DCC-Auth ${token}`,
        Accept: 'application/json'
      }
    });

  runInAction('update login status', () => { this.isLoggingIn = false });

  if (response.status === 200) {
    const userData = await response.json(); 
    runInAction('update user data', () => {
      this.username = userData.username;
      this.roles = userData.roles;
      this.token = token;
      this.isLoggedIn = true;
    }); 
  } else if (response.status === 401) {
    throw new Error('Incorrect username or password');
  } else {
    throw new Error('Login failed');
  }
});

export default user;