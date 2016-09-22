import { fetchHeaders } from '~/utils';

export function setSystemSftpStatus({sftpEnabled}) {
  return fetch(`/ws/systems`, {
    method: 'PATCH',
    headers: fetchHeaders.get(),
    body: JSON.stringify({
      active: sftpEnabled,
    }),
  }).then( res => res.json() );
}
