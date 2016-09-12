import {observable, action} from 'mobx';

const systemInfo = observable({
  version: '',
  commitId: '',
});

export const setSytemInfo = action('Set system version', function ({version, commitId}) {
  Object.assign(systemInfo, {version, commitId});
});

export const setSystemInfoFromHeaders = action('Set system info from headers', function (headers) {
  setSytemInfo({
    version: headers.get('X-ICGC-Submission-Version'),
    commitId: headers.get('X-ICGC-Submission-CommitId'),
  });
})

export default systemInfo;