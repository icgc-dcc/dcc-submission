import { fetchHeaders } from '~/utils';

export async function fetchReleases () {
  const response = await fetch('/ws/releases', {
    headers: fetchHeaders.get()
  });
  return response.json();
}