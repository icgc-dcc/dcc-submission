import { fetchHeaders } from '~/utils';

export async function fetchSubmissions ({releaseName}) {
  const resourceUrl = releaseName
    ? `/ws/releases/${releaseName}/submissions`
    : `/ws/nextRelease/submissions`;
  const response = await fetch(resourceUrl, {
    headers: fetchHeaders.get()
  });
  return response.json();
}
