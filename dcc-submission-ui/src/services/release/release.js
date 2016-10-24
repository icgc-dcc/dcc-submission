import { fetchHeaders } from '~/utils';

export async function fetchRelease ({releaseName}) {
  const response = await fetch(`/ws/releases/${releaseName}`, {
    headers: fetchHeaders.get()
  });
  const responseData = await response.json();
  return responseData;
}