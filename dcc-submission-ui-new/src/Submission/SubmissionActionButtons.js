import React, { PropTypes } from 'react';
import ActionButton from '~/common/components/ActionButton/ActionButton';

export default function SubmissionActionButtons ({
  submission,
  buttonClassName,
  onClickValidate,
  onClickSignOff,
}) {
  const { state: submissionState } = submission;

  const validateButton = (
    <ActionButton
      className={`blue-stripe ${buttonClassName}`}
      style={{marginLeft: 3}}
      onClick={onClickValidate}
    >
      Validate
    </ActionButton>
  );

  const signOffButton = (
    <ActionButton
      className={`green-stripe ${buttonClassName}`}
      onClick={onClickSignOff}
    >
      Sign off
    </ActionButton>
  );

  switch (submissionState) {
    case 'INVALID':
      return validateButton;
    case 'QUEUED':
    case 'VALIDATING':
      return <ActionButton className={`red-stripe ${buttonClassName}`}>Cancel Validation</ActionButton>
    case 'VALID':
      return (
        <div>
          {signOffButton}
          {validateButton}
        </div>
      );
    case 'NOT_VALIDATED':
      return (submission.submissionFiles.length)
        ? validateButton
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