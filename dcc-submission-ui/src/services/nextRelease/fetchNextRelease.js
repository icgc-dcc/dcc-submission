import { fetchHeaders } from '~/utils';

export async function fetchNextRelease () {
  const response = await fetch('/ws/nextRelease/', {
    method: 'GET',
    headers: fetchHeaders.get(),
  });
  const responseData = await response.json();
  if (!response.ok) {
    console.log('response not ok', responseData);
    throw new Error(responseData);
  }
  return responseData;
}
