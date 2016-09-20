import { observable, action, computed } from 'mobx';

import { fetchSystems, lockRelease, unlockRelease } from '~/services/systems';

class SystemsModel {
  @observable version: '';
  @observable commitId: '';

  @observable activeSftpSessions: 0;
  @observable sftpState: '';
  @observable sftpEnabled;
  @observable userSessions;

  constructor () {
    // NOTE: required due to babel issue https://mobxjs.github.io/mobx/best/pitfalls.html#-observable-properties-initialize-lazily-when-using-babel
    this.userSessions = [];
    this.sftpEnabled = true;
  }

  @computed get isReleaseLocked () {
    return !this.sftpEnabled; 
  }

  @action fetch = async () => {
    const systemsEndpointData = await fetchSystems();
    Object.assign(this, systemsEndpointData);
  };

  lockRelease = async () => {
    const responseData = await lockRelease();
    Object.assign(this, responseData);
  };

  unlockRelease = async () => {
    const responseData = await unlockRelease();
    Object.assign(this, responseData);
  };
}

export default SystemsModel;
