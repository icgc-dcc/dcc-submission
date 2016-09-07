import React from 'react';
import defaultTableOptions from '~/common/defaultTableOptions';
import { BootstrapTable, TableHeaderColumn } from 'react-bootstrap-table';

import processAndDenormalizeErrors from './processAndDenormalizeErrors';
import errorDict from './errorDict.coffee';
import getErrorDetails from './getErrorDetails.coffee';

export default function ErrorReportTable ({ items, isLoading }) {
  const tableOptions = {
    ...defaultTableOptions,
    noDataText: isLoading ? 'Loading...' : 'There is no data to display',
    defaultSortName: 'name',
  };

  const denormalizedErrors = processAndDenormalizeErrors(items);

  return (
    <BootstrapTable
      data={denormalizedErrors}
      keyField='errorType'
      striped={true}
      pagination={true}
      ignoreSinglePage={true}
      search={denormalizedErrors.length > tableOptions.sizePerPage}
      options={tableOptions}
    >
      <TableHeaderColumn
        dataField='name'
        dataSort={true}
        dataFormat={(cell, {errorType}) => (
          errorDict[errorType] ? errorDict[errorType].name : errorType
        )}
      >Error Type</TableHeaderColumn>

      <TableHeaderColumn
        dataField='fieldNames'
        dataFormat={(fieldNames) => fieldNames && fieldNames.map( fieldName => <div>{fieldName}</div>)}
        width="200"
      >Columns</TableHeaderColumn>

      <TableHeaderColumn
        dataField='count'
        dataSort={true}
        width="110"
      >Count of occurrences</TableHeaderColumn>

      <TableHeaderColumn
        dataFormat={(cell, error) => (<div>
          <div dangerouslySetInnerHTML={{__html: errorDict[error.errorType] && errorDict[error.errorType].description(error)}}/>
          <div dangerouslySetInnerHTML={{__html: getErrorDetails(error)}}/>
        </div>)}
      >Details</TableHeaderColumn>

    </BootstrapTable>
  );
}