import React from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';

import { fetchHeaders } from '~/utils';

async function signOff ({projectKeys, onSuccess}) {
  const response = await fetch('/ws/nextRelease/signed', {
    method: 'POST',
    headers: {
      ...fetchHeaders.get(),
    },
    body: JSON.stringify(projectKeys),
  });

  const responseData = await response.json();
  if (!response.ok) {
    console.error('response not ok', responseData);
  } else {
    onSuccess();
  }
}

const SignOffModal = observer(function SignOffModal({projectKey, onClickClose, onSuccess}) {
  return (
    <div className="modal-content">
      <div className="modal-header">
        <button
          type="button"
          className="close"
          aria-label="Close"
          onClick={onClickClose}
        >
          <span aria-hidden="true">&times;</span>
        </button>
        <h3>Sign Off</h3>
      </div>
      <div className="modal-body">
        <div className="alert alert-info">
          
        </div>
      </div>
      <div className="modal-footer">
        <button className="m-btn grey-stripe" onClick={onClickClose}>Close</button>
        <button className="m-btn green" onClick={() => signOff({projectKey, onSuccess})}>Release</button>
      </div>
    </div>
  );
});

export default SignOffModal;