import React from 'react';
import { map, includes } from 'lodash';

const REPORT_TYPE_DICT = {
  AVERAGE: 'Statistics',
  FREQUENCY: 'Value Frequencies (value:count)',
  _DEFAULT: '',
};

export default function DetailedReportSummary({report}) {
  const title = REPORT_TYPE_DICT[report.type] || REPORT_TYPE_DICT._DEFAULT;
  return (
    <div>
      <span className="title">{title}</span>
      <ul>
        {map(report.summary, (value, key) => (
          <li key={key}>
            <strong>{key}:&nbsp;</strong>
            { includes(['stddev', 'avg']) ? Number(value).toFixed(2) : value }
          </li>
        ))}
      </ul>
    </div>
  );
}