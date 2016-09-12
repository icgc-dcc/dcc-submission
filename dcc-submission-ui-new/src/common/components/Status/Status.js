import React, {PropTypes} from 'react';

import './Status.css';

const STATUS_MAP = {
  NOT_VALIDATED: {
    wrapperClass: '',
    iconClass: 'fa fa-question-circle',
  },
  ERROR: {
    wrapperClass: 'error',
    iconClass: 'fa fa-exclamation-circle',
  },
  INVALID: {
    wrapperClass: 'error',
    iconClass: 'fa fa-minus-circle',
  },
  QUEUED: {
    wrapperClass: 'queued',
    iconClass: 'fa fa-clock-o',
  },
  VALIDATING: {
    wrapperClass: 'validating',
    iconClass: 'fa fa-cogs',
  },
  VALID: {
    wrapperClass: 'valid',
    iconClass: 'fa fa-check-circle',
  },
  SIGNED_OFF: {
    wrapperClass: 'valid',
    iconClass: 'fa fa-lock',
  },
  OPENED: {
    wrapperClass: 'valid',
  },
  COMPLETED: {
    wrapperClass: 'unimportant',
  },
  _DEFAULT: {
    wrapperClass: '',
    iconClass: '',
  }
};

function Status ({statusCode}) {
  const { wrapperClass, iconClass } = STATUS_MAP[statusCode] || STATUS_MAP._DEFAULT;
  return (
    <span className={`StatusLabel ` + wrapperClass}>
      {iconClass && <i className={iconClass}/>}
      {(statusCode || 'SKIPPED').replace(/_/g, ' ')}
    </span>
  );
}

Status.propTypes = {
  statusCode: PropTypes.string.isRequired,
};

export default Status;