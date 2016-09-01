import React from 'react';

export default function CompleteRelease({releaseName, onClickClose}) {
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
        <div className="alert alert-info">
          Once you have confirmed completing the release <strong>"{releaseName}"</strong> it will be marked as COMPLETED and all submissions that are SIGNED OFF will be processed and included in this release. Please be certain before you continue.
        </div>
        <form className="form-horizontal">
          <div className="form-group">
            <label className="col-sm-4 text-right" htmlFor="nextRelease">Next Release Name</label>
            <div className="col-sm-8">
              <input autoFocus="autofocus" type="text" className="form-control" placeholder="Enter next release name" id="nextRelease"/>
            </div>
          </div>
        </form>
      </div>
      <div className="modal-footer"></div>
    </div>
  );
}