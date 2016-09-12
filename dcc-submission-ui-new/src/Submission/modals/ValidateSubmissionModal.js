import React, { Component, PropTypes } from 'react';
import { uniq, xor, includes } from 'lodash';
import {observable, computed} from 'mobx';
import {observer} from 'mobx-react';
import isEmail from 'is-email';

import Modal from 'react-modal';

import user from '~/user';
import Status from '~/common/components/Status';
import DATATYPE_DICTIONARY from '~/common/constants/DATATYPE_DICTIONARY';

// CLINICAL_CORE_TYPE is always checked

@observer
class ValidateModal extends Component {
  static propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onRequestClose: PropTypes.func.isRequired,
    onRequestSubmit: PropTypes.func.isRequired,
    initiallySelectedDataTypes: PropTypes.array.isRequired,
    dataTypeReports: PropTypes.array.isRequired,
    errorMessage: PropTypes.string,
  };

  // requiredDataTypes must always be selected if available and cannot be deselected
  requiredDataTypes = ['CLINICAL_CORE_TYPE'];
  @observable selectedDataTypes = [];
  @observable errorMessage;

  @observable emailsText = '';
  @computed get emails() {
    return this.emailsText
      .replace(/\n/g, '')
      .split(',')
        .map(x => x.trim())
        .filter(isEmail);
  }

  @computed get isValid() {
    return this.emails.length > 0;
  }

  componentWillMount() {
    this.emailsText = user.emailsToNotify.join(',\n');
  }

  componentWillReceiveProps({initiallySelectedDataTypes}) {
    if (initiallySelectedDataTypes) {
      this.selectedDataTypes = uniq(initiallySelectedDataTypes.concat(this.requiredDataTypes));
    }
  }

  handleClickSubmit = async () => {
    try {
      await this.props.onRequestSubmit({
        dataTypes: this.selectedDataTypes,
        emails: this.emails,
      });
      user.emailsToNotify = this.emails;
    } catch (e) {
      this.errorMessage = e.message;
    }
  }

  toggleSelectDataType = (dataType) => {
    this.selectedDataTypes = xor(this.selectedDataTypes.slice(), [dataType]);
  }

  handleClickClose = () => {
    this.errorMessage = '';
    this.props.onRequestClose();
  };

  handleClickSelectAll = () => {
    this.selectedDataTypes = this.props.dataTypeReports.map(report => report.dataType);
  };

  handleClickClear = () => {
    this.selectedDataTypes = [];
  };

  render () {
    const {
      isOpen,
      dataTypeReports,
    } = this.props;
 
    const queueLength = 5;
    return (
      <Modal
          className={`Modal modal-dialog`}
          isOpen={isOpen}
          onRequestClose={this.handleClickClose}
          closeTimeoutMS={250}
          shouldCloseOnOverlayClick={true}
        >
        <div className="modal-container">
          <div className="modal-header">
            <button
              type="button"
              className="close"
              aria-label="Close"
              onClick={this.handleClickClose}
            >
              <span aria-hidden="true">&times;</span>
            </button>
            <h3>Validate Submission</h3>
          </div>
          <div className="modal-body">
            {this.errorMessage ? (
              <div className="alert alert-danger">
                {this.errorMessage}
              </div>
            ) : ''}
            <table id="validate-file-types" className="table table-condensed">
              <tbody>
                {
                  dataTypeReports.map(report => {
                    const isRequired = includes(this.requiredDataTypes, report.dataType);
                    const isSelected = includes(this.selectedDataTypes, report.dataType);
                    return (
                      <tr
                        className={`${isRequired ? 'required-row' : ''}`}
                        onClick={() => !isRequired && this.toggleSelectDataType(report.dataType)}
                        key={report.dataType}
                      >
                        <td>
                          <i 
                            className={`fa fa-${isSelected ? 'check-square-o' : 'square-o'}`}
                          />
                          {DATATYPE_DICTIONARY[report.dataType]}
                        </td>
                        <td><Status statusCode={report.dataTypeState}/></td>
                      </tr>
                  )})
                }
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan="2">
                    <button type="button" className="m-btn mini blue-stripe" onClick={this.handleClickSelectAll}>Select All</button>
                    <button type="button" className="m-btn mini blue-stripe" onClick={this.handleClickClear}>Clear</button>
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div>
            <div className="alert alert-danger">Validation may take several hours to complete!</div>
            <div className="alert alert-info">
              There are currently <strong>{queueLength}</strong> submission(s) in the Validation Queue.
              <br/>
              Enter a comma(,) seperated list of the email addresses that should be notified when validation is finished:
              <br/><br/>
              <textarea
                autoFocus
                style={{width: '100%'}}
                className="m-wrap"
                id="emails"
                defaultValue={this.emailsText}
                onChange={e => {this.emailsText = e.target.value}}
              />
            </div>
          </div>
          <div className="modal-footer">
            <button className="btn btn-default" onClick={this.handleClickClose}>Close</button>
            <button
              type="submit"
              className="btn btn-primary"
              onClick={this.handleClickSubmit}
              disabled={!this.isValid}
            >Validate Submission</button>
          </div>
        </div>
      </Modal>
    );
  }
}

export default ValidateModal;