import { fetchHeaders } from '~/utils';

export async function fetchSystems () {
  const response = await fetch(`/ws/systems`, {
    headers: fetchHeaders.get()
  });

  const versionInfo = {
    version: response.headers.get('X-ICGC-Submission-Version'),
    commitId: response.headers.get('X-ICGC-Submission-CommitId'),
  }

  const responseJson = await response.json();

  return Object.assign({}, versionInfo, responseJson);
}
