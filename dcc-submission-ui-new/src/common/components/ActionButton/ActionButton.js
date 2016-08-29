import React from 'react';

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