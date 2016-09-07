import {observable, action, runInAction } from 'mobx';
import { fetchHeaders } from '~/utils';

export async function fetchReport ({releaseName, projectKey, fileName}) {
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}/files/${fileName}/report`, {
      headers: fetchHeaders.get()
    });
  return response.json();
};

class ReportModel {
  @observable isLoading= false;
  @observable errorReports= [];
  @observable fieldReports= [];
  @observable summaryReports= [];
  @observable fileName= undefined;
  @observable fileType= undefined;

  constructor ({releaseName, projectKey, fileName}) {
    Object.assign(this, {releaseName, projectKey, fileName});
  }

  @action fetch = async () => {
    const {releaseName, projectKey, fileName} = this;
    const responseData = await fetchReport({releaseName, projectKey, fileName});
    Object.assign(this, responseData);
  };
}

export default ReportModel;