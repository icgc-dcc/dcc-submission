// original implementation from schema_report.coffee L41-53

import {zipObject, flatten} from 'lodash';

export default function processAndDenormalizeErrors (errorReports) {
  return flatten(errorReports.map( errorReport => errorReport.fieldErrorReports.map(
    fieldErrorReport => ({
      ...fieldErrorReport,
      errorType: errorReport.errorType,
      fieldNames: fieldErrorReport.fieldNames || [],
      lineValueMap: zipObject(fieldErrorReport.lineNumbers, fieldErrorReport.values),
      lineNumbers: fieldErrorReport.lineNumbers.sort((a, b) => a - b),
      key: `${errorReport.errorType}&${(fieldErrorReport.fieldNames || []).join('&')}`,
    }))));
};