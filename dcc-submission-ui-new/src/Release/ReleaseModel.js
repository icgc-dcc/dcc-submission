import {observable, action, runInAction} from 'mobx';

import { fetchHeaders } from '~/utils';

import { setSystemLockStatus } from '~/systemInfo';
import RELEASE_STATES from './RELEASE_STATES';

const PERFORM_RELEASE_ERRORS = {
  'InvalidName': 'A release name must only use letters[a-z], numbers(0-9), underscores(_) and dashes(-)',
  'DuplicateReleaseName': 'That release name has already been used.',
  'SignedOffSubmissionRequired': 'The release needs at least one SIGNED OFF submission before it can be COMPLETED.',
  _default: 'An error occurred. Please contact Support for assistance.'
};

export async function fetchRelease ({releaseName}) {
  const response = await fetch(`/ws/releases/${releaseName}`, {
    headers: fetchHeaders.get()
  });
  const responseData = await response.json();
  if (responseData.state === RELEASE_STATES.OPENED) {
    setSystemLockStatus(responseData.locked);
  }
  return responseData;
}

export async function performRelease ({nextReleaseName}) {
  const response = await fetch('/ws/nextRelease/', {
    method: 'POST',
    headers: fetchHeaders.get(),
    body: JSON.stringify({
      name: nextReleaseName || '',
      submissions: [],
    }),
  });
  const responseData = await response.json();
  if (!response.ok) {
    console.log('response not ok', responseData);
    throw new Error(PERFORM_RELEASE_ERRORS[responseData.code] || PERFORM_RELEASE_ERRORS._default);
  }
  return responseData;
}

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