import React, { Component } from 'react';

import {observable, action, runInAction } from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders } from '~/utils';

import ErrorTable from './ReportErrorTable';
import errorMap from './errorMap.coffee';

const report = observable({
  isLoading: false,

  errorReports: [],
  fieldReports: [],
  summaryReports: [],
  fileName: undefined,
  fileType: undefined,
});

/*
format of errorReports:

   "errorReports":[  
      {  
         "errorType":"FILE_HEADER_ERROR",
         "number":0,
         "description":"File header error: %s",
         "fieldErrorReports":[  
            {  
               "fieldNames":null,
               "parameters":{  
                  "VALUE":[  
                     "analyzed_sample_id",
                     "specimen_id",
                     "analyzed_sample_type",
                     "analyzed_sample_type_other",
                     "analyzed_sample_interval",
                     "percentage_cellularity",
                     "level_of_cellularity",
                     "analyzed_sample_notes"
                  ],
                  "EXPECTED":[  
                     "analyzed_sample_id",
                     "specimen_id",
                     "analyzed_sample_interval",
                     "percentage_cellularity",
                     "level_of_cellularity",
                     "analyzed_sample_notes",
                     "study"
                  ]
               },
               "count":1,
               "lineNumbers":[  
                  -1
               ],
               "values":[  
                  null
               ]
            }
         ],
         "converted":false
      }
   ]

 */

/*
Format of fieldReports
{  
    "name":"specimen_interval",
    "type":"AVERAGE",
    "nulls":0,
    "missing":0,
    "populated":2,
    "completeness":100.0,
    "summary":{  
      "min":20.0,
      "max":20.0,
      "avg":20.0,
      "stddev":0.0
    }
}
 */

report.fetch = action('fetch single report', async function (releaseName, projectKey, reportName) {
  this.isLoading = true;
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}/report/${reportName}`, {
      headers: fetchHeaders.get()
    });

  runInAction('update loading status', () => { this.isLoading = false });

  const responseData = await response.json();

  runInAction('update report', () => {
    Object.assign(this, responseData);
  });
});

export default @observer
class Report extends Component {

  componentWillMount () {
    const {releaseName, projectKey, fileName } = this.props.params;
    report.fetch(releaseName, projectKey, fileName);
  }

  render () {
    window.debugLoad = () => report.fetch('release1', 'project.1', 'donor.txt.bz2');

    const {releaseName, projectKey, fileName } = this.props.params;

    const hasErrors = report.errorReports.length
    const pageTitle = hasErrors ? 'Error Report' : 'Detailed Report';

    return <div>
      <h1>File Summary</h1>
      <ul>
        <li>File name: {report.fileName}</li>
        <li>State: {report.fileState}</li>
      </ul>

      { /* report.summaryReports.length && */ (
        <table width="100%" id="schema-summary-container" className="table table-striped table-bordered alert-info">
          TODO: figure out what goes inside schema-summary-container
        </table>
      )}

      <div>
        <h2>{ pageTitle }</h2>
        <table>
          <thead>
            <tr>
              <th></th>
              <th></th>
              <th></th>
              <th></th>
              <th></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            { /* display error table if  */}
            <tr>
              
            </tr>
          </tbody>
        </table>
      </div>
      <pre>
      {JSON.stringify(report, null, '  ')}
      </pre>
    </div>
  }
}