import React, { PropTypes } from 'react';

export default function SubmissionActionButtons ({submission}) {
  const { state: submissionState } = submission;

  switch (submissionState) {
    case 'INVALID':
      return <div>Validate</div>;
    case 'QUEUED':
    case 'VALIDATING':
      return <div>Cancel Validation</div>
    case 'VALID':
      return (
        <div>
          Sign off,
          Validate
        </div>
      );
    case 'NOT_VALIDATED':
      return (submission.submissionFiles.length && submission.submissionFiles.map(x => x.schemaName).filter(Boolean).length)
        ? <div>Validate</div>
        : <em>Upload Files</em>
    case 'ERROR':
    default:
      return <em>Contact dcc-support@icgc.org</em>;
  }
}

SubmissionActionButtons.propTypes = {
  submission: PropTypes.object.isRequired,
};