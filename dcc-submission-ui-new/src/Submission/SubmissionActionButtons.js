import React, { PropTypes } from 'react';
import ActionButton from '~/common/components/ActionButton/ActionButton';
import { some, includes } from 'lodash';
import RELEASE_STATES from '~/Release/RELEASE_STATES';
import SUBMISSION_STATES from './SUBMISSION_STATES';

export default function SubmissionActionButtons ({
  submissionState,
  submissionHasFiles,
  releaseState,
  userIsAdmin,
  buttonClassName,
  onClickValidate,
  onClickSignOff,
  onClickCancelValidation,
  onClickReset,
}) {

  const validateButton = (
    <ActionButton
      key="validateButton"
      className={`blue-stripe ${buttonClassName}`}
      style={{marginLeft: 3}}
      onClick={onClickValidate}
    >Validate</ActionButton>
  );

  const signOffButton = (
    <ActionButton
      key="signOffButton"
      className={`green-stripe ${buttonClassName}`}
      onClick={onClickSignOff}
    >Sign off</ActionButton>
  );

  const cancelValidationButton = (
    <ActionButton
      key="cancelValidationButton"
      className={`red-stripe ${buttonClassName}`}
      onClick={onClickCancelValidation}
    >Cancel Validation</ActionButton>
  );

  const resetButton = (
    <ActionButton
      key="resetButton"
      className={`red-stripe ${buttonClassName}`}
      onClick={onClickReset}
    >Reset</ActionButton>
  );

  const elements = [
    {
      element: signOffButton,
      isVisible: submissionState === SUBMISSION_STATES.VALID && userIsAdmin,
    },
    {
      element: validateButton,
      isVisible: some([
        submissionState === SUBMISSION_STATES.INVALID,
        submissionState === SUBMISSION_STATES.VALID,
        submissionState === SUBMISSION_STATES.NOT_VALIDATED && submissionHasFiles,
      ]),
    },
    {
      element: cancelValidationButton,
      isVisible: includes([SUBMISSION_STATES.QUEUED, SUBMISSION_STATES.VALIDATING], submissionState),
    },
    {
      element: resetButton,
      isVisible: releaseState === RELEASE_STATES.OPENED,
    },
    {
      element: <em key="uploadFilesMessage">Upload Files</em>,
      isVisible: submissionState === SUBMISSION_STATES.NOT_VALIDATED && !submissionHasFiles,
    },
    {
      element: <em key="contactMessage">Contact dcc-support@icgc.org</em>,
      isVisible: submissionState === SUBMISSION_STATES.ERROR,
    },
  ];

  const visibleElements = elements.filter( x => x.isVisible ).map(x => x.element);
  return <div>{visibleElements}</div>;
  // switch (submissionState) {
  //   case 'INVALID':
  //     return validateButton;
  //   case 'QUEUED':
  //   case 'VALIDATING':
  //     return cancelValidationButton
  //   case 'VALID':
  //     return (
  //       <div>
  //         {signOffButton}
  //         {validateButton}
  //       </div>
  //     );
  //   case 'NOT_VALIDATED':
  //     return (submissionHasFiles)
  //       ? validateButton
  //       : <em>Upload Files</em>
  //   case 'SIGNED_OFF':
  //     return null;
  //   case 'ERROR':
  //   default:
  //     return <em>Contact dcc-support@icgc.org</em>;
  // }
}

SubmissionActionButtons.propTypes = {
  submissionState: PropTypes.string.isRequired,
  releaseState: PropTypes.string.isRequired,
  submissionHasFiles: PropTypes.bool.isRequired,
  userIsAdmin: PropTypes.bool.isRequired,
};