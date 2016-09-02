import React, { PropTypes } from 'react';
import ActionButton from '~/common/components/ActionButton/ActionButton';

export default function SubmissionActionButtons ({submission, buttonClassName}) {
  const { state: submissionState } = submission;

  switch (submissionState) {
    case 'INVALID':
      return <ActionButton className={`blue-stripe ${buttonClassName}`}>Validate</ActionButton>;
    case 'QUEUED':
    case 'VALIDATING':
      return <ActionButton className={`red-stripe ${buttonClassName}`}>Cancel Validation</ActionButton>
    case 'VALID':
      return (
        <div>
          <ActionButton className={`green-stripe ${buttonClassName}`}>
            Sign off
          </ActionButton>
          <ActionButton className={`blue-stripe ${buttonClassName}`} style={{marginLeft: 3}}>
            Validate
          </ActionButton>
        </div>
      );
    case 'NOT_VALIDATED':
      return (submission.submissionFiles.length && submission.submissionFiles.map(x => x.schemaName).filter(Boolean).length)
        ? <ActionButton className={`blue-stripe ${buttonClassName}`}>Validate</ActionButton>
        : <em>Upload Files</em>
    case 'SIGNED_OFF':
      return null;
    case 'ERROR':
    default:
      return <em>Contact dcc-support@icgc.org</em>;
  }
}

SubmissionActionButtons.propTypes = {
  submission: PropTypes.object.isRequired,
};