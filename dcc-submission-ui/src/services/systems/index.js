export * from './fetchSystems';
export * from './setSystemSftpStatus';

import {setSystemSftpStatus} from './setSystemSftpStatus';

export function lockRelease() {
  return setSystemSftpStatus({sftpEnabled: false});
}

export function unlockRelease() {
  return setSystemSftpStatus({sftpEnabled: true});
}