import {observable, action, autorun} from 'mobx';

import {fetchHeaders} from '~/utils';
import { setSystemInfoFromHeaders } from '~/systemInfo';

function generateToken(username, password) {
  return global.btoa(`${username}:${password}`);
}

let storedEmailsToNotify = [];
try {
  const emailsFromLocalstorage = JSON.parse(window.localStorage.getItem('storedEmailsToNotify'));
  if (Array.isArray(emailsFromLocalstorage)) {
    storedEmailsToNotify = emailsFromLocalstorage;
  };
} catch (e) {
  console.error('could not read emails from local storage', e);
}

const user = observable({
  username: '',
  token: '',
  roles: [],
  isLoggedIn: false,
  isLoggingIn: false,
  emailsToNotify: storedEmailsToNotify,

  isAdmin: function () {
    return this.roles.indexOf('admin') >= 0;
  },

  login: action(async function (username, password) {
    this.isLoggingIn = true;
    const token = generateToken(username, password);
    const response = await fetch('/ws/users/self', {
        headers: {
          ...fetchHeaders.get(),
          Authorization: `Basic ${token}`,
        }
      });

    setSystemInfoFromHeaders(response.headers);

    if (response.status === 200) {
      this.isLoggingIn = false;
      const userData = await response.json();
      this.username = userData.username;
      this.roles = userData.roles;
      this.token = token;
      this.isLoggedIn = true;
    } else if (response.status === 401) {
      throw new Error('Incorrect username or password');
    } else {
      throw new Error('Login failed');
    }
  }),

  logout: action(function () {
    this.isLoggedIn = false;
    this.username = '';
    this.token = '';
    this.emailsToNotify = [];
    this.roles = [];
  })
});

autorun(() => window.localStorage.setItem('storedEmailsToNotify', JSON.stringify(user.emailsToNotify.slice())));

export default user;
