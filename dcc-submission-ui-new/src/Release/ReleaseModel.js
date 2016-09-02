import {observable, action, runInAction} from 'mobx';

import { fetchHeaders } from '~/utils';

const PERFORM_RELEASE_ERRORS = {
  'InvalidName': 'A release name must only use letters[a-z], numbers(0-9), underscores(_) and dashes(-)',
  'DuplicateReleaseName': 'That release name has already been used.',
  'SignedOffSubmissionRequired': 'The release needs at least one SIGNED OFF submission before it can be COMPLETED.',
  _default: 'An error occurred. Please contact Support for assistance.'
};

export async function fetchRelease (releaseName) {
  const response = await fetch(`/ws/releases/${releaseName}`, {
    headers: fetchHeaders.get()
  });
  const responseData = await response.json();
  return responseData;
}

export async function performRelease ({release}) {
  const releaseName = release.name;
  const response = await fetch('/ws/nextRelease/', {
    method: 'POST',
    headers: {
      ...fetchHeaders.get(),
    },
    body: JSON.stringify({
      name: releaseName,
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
    this.isLoading = true;
    const responseData = await fetchRelease(this.name);
    runInAction('update release', () => {
      this.isLoading = false;
      Object.assign(this, responseData);
    });
  }

  performRelease = () => {
    performRelease(this);
  }
}

export default ReleaseModel;