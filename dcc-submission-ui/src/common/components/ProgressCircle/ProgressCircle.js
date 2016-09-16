import React from 'react';
import { Circle } from 'react-progressbar.js';
import {find, sortBy} from 'lodash';
import rgbHex from 'rgb-hex';

import './ProgressCircle.css';

const progressColors = [
  {minProgress: 0, color: [216, 5, 5]},
  // {minProgress: 0.3, color: [255, 173, 24]},
  {minProgress: 0.5, color: [29, 148, 59]},
];

export default function ProgressCircle({progress, className}) {
  const diameter = 120;
  const hexColor = rgbHex(...find(sortBy(progressColors, 'minProgress').reverse(), item => progress >= item.minProgress).color);
  const containerStyle = {
    width: diameter,
    height: diameter,
  };
  const options = {
    strokeWidth: 12,
    color: '#' + hexColor,
    trailColor: '#ddd',
  };

  const statusClass = progress > 0.5 ? 'green' : 'red';
  return (
    <Circle
      progress={progress}
      text={ (progress * 100).toPrecision(4) + '%' }
      options={options}
      initialAnimate={true}
      containerStyle={containerStyle}
      containerClassName={`ProgressCircle ${className} ${statusClass}`}
    />
  );
}
