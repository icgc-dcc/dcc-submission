import React from 'react';

import './ActionButton.css';

function ActionButton (props = {}) {
  const { className = '' } = props;
  return (
    <div
      {...props}
      className={`ActionButton ${className}`}
      >
      {props.children}
    </div>
  );
}

export default ActionButton;