import {observable, action, runInAction, computed} from 'mobx';
import { includes, groupBy, omit } from 'lodash';
import { fetchHeaders } from '~/utils';

import injectReportsToSubmissionFiles from './injectReportsToSubmissionFiles.coffee';

const CLINICAL_DATA_TYPES = ['CLINICAL_SUPPLEMENTAL_TYPE', 'CLINICAL_CORE_TYPE']; 

export async function fetchSubmission ({releaseName, projectKey}) {
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}`, {
      headers: fetchHeaders.get()
    });
  return await response.json();
}

export async function queueSubmissionForValidation ({projectKey, emails, dataTypes}) {
  const response = await fetch('/ws/nextRelease/queue', {
    method: 'POST',
    headers: fetchHeaders.get(),
    body: JSON.stringify([{
      key: projectKey,
      emails: emails,
      dataTypes,
    }]),
  });
  if (!response.ok) {
    console.error('response not ok', response);
  }
}

export function signOffSubmission ({projectKey}) {
  return fetch('/ws/nextRelease/signed', {
    method: 'POST',
    headers: fetchHeaders.get(),
    body: JSON.stringify([projectKey]),
  });
}

export async function resetSubmission ({projectKey}) {
  const response = await fetch(`/ws/nextRelease/state/${projectKey}`, {
    method: 'DELETE',
    headers: fetchHeaders.get(),
  });
  if (!response.ok) {
    console.error('response not ok', response);
  }
  return response
}


class SubmissionModel {
  @observable isLoading = false;
  @observable lastUpdated = undefined;
  @observable locked = false;
  @observable projectAlias = undefined;
  @observable projectKey = undefined;
  @observable projectName = undefined;
  @observable releaseName = undefined;
  @observable report = {
    // why is this nested?
    dataTypeReports: []
  };
  @observable state = undefined;
  @observable submissionFiles = [];

  constructor ({releaseName, projectKey}) {
    this.releaseName = releaseName;
    this.projectKey = projectKey;
  }

  @computed get totalFileSizeInBytes() {
    return this.submissionFiles.reduce((acc, file) => acc + file.size, 0);
  }

  @computed get abstractlyGroupedSubmissionFiles() {
    return groupBy(this.submissionFiles, file => (
      includes(CLINICAL_DATA_TYPES, file.dataType)
        ? 'CLINICAL'
        : !!file.dataType
          ? 'EXPERIMENTAL'
          : 'UNRECOGNIZED'
    ));
  };

  @action fetch = async () => {
    this.isLoading = true;
    const responseData = await fetchSubmission({
      releaseName: this.releaseName,
      projectKey: this.projectKey,
    });
    runInAction('update model', () => {
      this.isLoading = false;
      // NOTE: omit is a temporary workaround due to endpoint returning 'null' as releaseName
      Object.assign(this, omit(responseData, 'releaseName'));
      injectReportsToSubmissionFiles(this.submissionFiles, this.report);
    });
  };

  @action requestValidation = async ({emails, dataTypes}) => {
    const responseData = await queueSubmissionForValidation({
      projectKey: this.projectKey,
      emails,
      dataTypes,
    });
    return responseData;
  };

  @action reset = () => {
    return resetSubmission({projectKey: this.projectKey});
  };

  @action signOff = () => {
    return signOffSubmission({projectKey: this.projectKey});
  };

};

export default SubmissionModel
