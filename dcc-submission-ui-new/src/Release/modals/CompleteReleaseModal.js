import React from 'react';
import {observable} from 'mobx';
import {observer} from 'mobx-react';

import { fetchHeaders } from '~/utils';

const ErrorsDict = {
  'InvalidName': 'A release name must only use letters[a-z], numbers(0-9), underscores(_) and dashes(-)',
  'DuplicateReleaseName': 'That release name has already been used.',
  'SignedOffSubmissionRequired': 'The release needs at least one SIGNED OFF submission before it can be COMPLETED.',
  _default: 'An error occurred. Please contact Support for assistance.'
};

const nextRelease = observable({
  name: '',
  error: null,
});

const CompleteRelease = observer(function CompleteRelease({releaseName, onClickClose, onSuccess}) {
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
        <h3>Complete Release</h3>
      </div>
      <div className="modal-body">
        {nextRelease.error ? (
          <div className="alert alert-danger">
            {nextRelease.error.message}
          </div>
        ) : ''}
        <div className="alert alert-info">
          Once you have confirmed completing the release <strong>"{releaseName}"</strong> it will be marked as COMPLETED and all submissions that are SIGNED OFF will be processed and included in this release. Please be certain before you continue.
        </div>
        <form className="form-horizontal">
          <div className="form-group">
            <label className="col-sm-4 text-right" htmlFor="nextRelease">Next Release Name</label>
            <div className="col-sm-8">
              <input
                autoFocus="autofocus"
                type="text"
                className="form-control"
                placeholder="Enter next release name"
                id="nextRelease"
                onChange={(e) => { nextRelease.name = e.target.value }}
              />
            </div>
          </div>
        </form>
      </div>
      <div className="modal-footer">
        <button className="m-btn grey-stripe" onClick={onClickClose}>Close</button>
        <button className="m-btn green" onClick={() => performRelease({releaseName: nextRelease.name, onSuccess})}>Release</button>
      </div>
    </div>
  );
});

export default CompleteRelease;