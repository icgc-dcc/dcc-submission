import {computed} from 'mobx';

import user from '~/user.js';

export const fetchHeaders = computed(() => ({
  Authorization: `X-DCC-Auth ${user.token}`,
  Accept: 'application/json',
})); 