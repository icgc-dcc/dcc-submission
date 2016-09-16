import { fetchHeaders } from '~/utils';
import { setSystemInfoFromHeaders } from '~/systemInfo';

export async function fetchReleases () {
  const response = await fetch('/ws/releases', {
    headers: fetchHeaders.get()
  });
  setSystemInfoFromHeaders(response.headers);
  return response.json();
}