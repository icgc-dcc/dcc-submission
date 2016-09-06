import {observable, action, runInAction, computed} from 'mobx';
import { includes, groupBy } from 'lodash';
import { fetchHeaders } from '~/utils';

import injectReportsToSubmissionFiles from './injectReportsToSubmissionFiles.coffee';

const CLINICAL_DATA_TYPES = ['CLINICAL_SUPPLEMENTAL_TYPE', 'CLINICAL_CORE_TYPE']; 

export async function fetchSubmission (releaseName, projectKey) {
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}`, {
      headers: fetchHeaders.get()
    });
  return await response.json();
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
    const responseData = await fetchSubmission(this.releaseName, this.projectKey);
    runInAction('update model', () => {
      this.isLoading = false;
      Object.assign(this, responseData);
      injectReportsToSubmissionFiles(this.submissionFiles, this.report);
    });
  };

};

export default SubmissionModel
