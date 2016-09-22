import { fetchHeaders } from '~/utils';

export async function unlockUser (username) {
  const response = await fetch('/ws/users/unlock/'+username, {
    method: 'PUT',
    headers: fetchHeaders.get(),
    // body: JSON.stringify({
    //   name: username
    // }),
  });
  const responseData = await response.json();
  if (!response.ok) {
    console.log('response not ok', responseData);
    throw new Error(responseData.error);
  }
  return responseData;
}
