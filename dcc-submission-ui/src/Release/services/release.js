import { fetchHeaders } from '~/utils';
import { setSystemLockStatus } from '~/systemInfo';
import RELEASE_STATES from '~/Release/constants/RELEASE_STATES';

export async function fetchRelease ({releaseName}) {
  const response = await fetch(`/ws/releases/${releaseName}`, {
    headers: fetchHeaders.get()
  });
  const responseData = await response.json();
  if (responseData.state === RELEASE_STATES.OPENED) {
    setSystemLockStatus(responseData.locked);
  }
  return responseData;
}