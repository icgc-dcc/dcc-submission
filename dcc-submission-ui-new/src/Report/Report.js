import React, { Component } from 'react';

import {observable} from 'mobx';
import {observer} from 'mobx-react';
import Status from '~/common/components/Status';

import ReportModel from './ReportModel';
import DetailedReport from './DetailedReport/DetailedReport';
import ErrorReport from './ErrorReport/ErrorReport';

import './Report.css';

export default @observer
class Report extends Component {
  @observable report;

  componentWillMount () {
    const {releaseName, projectKey, fileName } = this.props.params;
    this.report = new ReportModel({releaseName, projectKey, fileName});
    this.report.fetch();
    this._pollInterval = global.setInterval(this.report.fetch, require('~/common/constants/POLL_INTERVAL'));
  }

  componentWillUnmount () {
    global.clearInterval(this._pollInterval);
  }

  render () {
    const report = this.report;
    const hasErrors = report.errorReports.length
    const pageTitle = hasErrors ? 'Error Report' : 'Detailed Report';

    const ReportDetails = hasErrors ? ErrorReport : DetailedReport;
    const reportItems = hasErrors ? report.errorReports : report.fieldReports;

    return (
      <div className="Report container">
        <h1>File Summary: <em className="colorize">{report.fileName}</em></h1>
        <ul>
          <li></li>
          <li>
            <Status
              statusCode={report.fileState || ''}
            />
          </li>
        </ul>
        <div>
          <h2>{ pageTitle }</h2>
          <ReportDetails
            items={reportItems}
            isLoading={report.isLoading}
          />
        </div>
      </div>
    );
  }
}