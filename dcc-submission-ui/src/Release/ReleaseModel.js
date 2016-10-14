import {observable, action } from 'mobx';
import { fetchRelease } from '~/services/release';
import { fetchSubmissions } from '~/services/submissions';
import { fetchNextRelease, performRelease } from '~/services/nextRelease';

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

  constructor ({name} = {}) {
    this.name = name;
  }

  @action fetch = async ({ shouldFetchUpcomingRelease } = {}) => {
    global.clearTimeout(this._fetchTimeout);
    this.isLoading = true;
    console.log('fetch release');
    const fetchMethod = shouldFetchUpcomingRelease ? fetchNextRelease : fetchRelease;
    const [release, submissions] = [
      await fetchMethod({releaseName: this.name}),
      await fetchSubmissions(shouldFetchUpcomingRelease ? {} : {releaseName: this.name})
    ];
    this.isLoading = false;
    Object.assign(this, Object.assign({}, release, {submissions}));
  }

  performRelease = () => {
    const [currentReleasePrefix, currentReleaseNumber] = this.name.match(/^(\D+)(\d+)$/).slice(1);
    const nextReleaseName = `${currentReleasePrefix}${Number(currentReleaseNumber) + 1}`;
    return performRelease({ nextReleaseName });
  }
}

export default ReleaseModel;