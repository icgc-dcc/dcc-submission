import { fetchHeaders } from '~/utils';

export async function clearQueue () {
  const response = await fetch('/ws/nextRelease/queue', {
    method: 'DELETE',
    headers: fetchHeaders.get(),
  });
  if (!response.ok) {
    console.log('response not ok', response);
    throw new Error(response);
  }
  return response;
}
