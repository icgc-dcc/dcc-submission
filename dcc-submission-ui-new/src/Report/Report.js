import React, { Component } from 'react';

import {observable, action, runInAction } from 'mobx';
import {observer} from 'mobx-react';
import { fetchHeaders } from '~/utils';
import Status from '~/common/components/Status';

import DetailedReport from './DetailedReport/DetailedReport';
import ErrorReport from './ErrorReport/ErrorReport';

const report = observable({
  isLoading: false,

  errorReports: [],
  fieldReports: [],
  summaryReports: [],
  fileName: undefined,
  fileType: undefined,
});

report.fetch = action('fetch single report', async function (releaseName, projectKey, reportName) {
  this.isLoading = true;
  const response = await fetch(`/ws/releases/${releaseName}/submissions/${projectKey}/report/${reportName}/`, {
      headers: fetchHeaders.get()
    });

  runInAction('update loading status', () => { this.isLoading = false });

  const responseData = await response.json();

  runInAction('update report', () => {
    Object.assign(this, responseData);
  });
});

window.report = report;

export default @observer
class Report extends Component {

  componentWillMount () {
    const {releaseName, projectKey, fileName } = this.props.params;
    report.fetch(releaseName, projectKey, fileName);
  }

  render () {
    const {releaseName, projectKey, fileName } = this.props.params;
    window.debugLoad = () => report.fetch(releaseName, projectKey, fileName);

    const hasErrors = report.errorReports.length
    const pageTitle = hasErrors ? 'Error Report' : 'Detailed Report';

    const ReportDetails = hasErrors ? ErrorReport : DetailedReport;
    const reportItems = hasErrors ? report.errorReports : report.fieldReports;

    return (
      <div>
        <h1>File Summary</h1>
        <ul>
          <li>{report.fileName}</li>
          <li>
            <Status
              statusCode={report.fileState || ''}
            />
          </li>
        </ul>
        <div>
          <h2>{ pageTitle }</h2>
          <ReportDetails
            items={reportItems.slice()}
            isLoading={report.isLoading}
          />
        </div>
        <pre>
        {JSON.stringify(report, null, '  ')}
        </pre>
      </div>
    );
  }
}