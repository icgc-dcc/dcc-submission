import {observable, action, runInAction} from 'mobx';
import { fetchRelease, performRelease } from '~/services/release';

class ReleaseModel {
  @observable isLoading = false;
  @observable dictionaryVersion = undefined;
  @observable locked = false;
  @observable name = '';
  @observable queue = [];
  @observable releaseDate = undefined;
  @observable state = undefined;
  @observable submissions = [];
  @observable summary = undefined;

  constructor ({name}) {
    this.name = name;
  }

  @action fetch = async () => {
    global.clearTimeout(this._fetchTimeout);
    this.isLoading = true;
    const responseData = await fetchRelease({releaseName: this.name});
    runInAction('update release', () => {
      this.isLoading = false;
      Object.assign(this, responseData);
    });
  }

  performRelease = ({ nextReleaseName }) => {
    return performRelease({ nextReleaseName });
  }
}

export default ReleaseModel;