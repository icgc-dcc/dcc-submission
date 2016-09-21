import { fetchHeaders } from '~/utils';

export async function fetchUsers () {
  const response = await fetch('/ws/users', {
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
