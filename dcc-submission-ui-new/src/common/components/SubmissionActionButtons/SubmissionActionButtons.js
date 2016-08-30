import React, { PropTypes } from 'react';

export default function SubmissionActionButtons ({submission}) {
  const { state: submissionState } = submission;

  switch (submissionState) {
    case 'INVALID':
      return <button>Validate</button>;
    case 'QUEUED':
    case 'VALIDATING':
      return <button>Cancel Validation</button>
    case 'VALID':
      return (
        <div>
          <button>
            Sign off
          </button>
          <button>
            Validate
          </button>
        </div>
      );
    case 'NOT_VALIDATED':
      return (submission.submissionFiles.length && submission.submissionFiles.map(x => x.schemaName).filter(Boolean).length)
        ? <button>Validate</button>
        : <em>Upload Files</em>
    case 'ERROR':
    default:
      return <em>Contact dcc-support@icgc.org</em>;
  }
}

SubmissionActionButtons.propTypes = {
  submission: PropTypes.object.isRequired,
};