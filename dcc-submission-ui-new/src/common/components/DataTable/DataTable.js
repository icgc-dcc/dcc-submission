import React, {Component, PropTypes} from 'react';
import {observable, computed} from 'mobx';
import BootstrapTable from 'reactjs-bootstrap-table';
import {observer} from 'mobx-react';
import { chunk, range } from 'lodash';
import Fuse from 'fuse.js';

@observer
class DataTable extends Component {
  static propTypes = {
    fieldsToSearch: PropTypes.array,
    pageSize: PropTypes.number,
  };

  static defaultProps = {
    fieldsToSearch: ['name', 'projectName'],
    pageSize: 5,
  };

  @observable totalItems = [];
  @observable currentPage = 0;
  @observable searchTerm = '';

  constructor (props, context) {
    super(props, context);
    this.totalItems = props.data;
  }

  componentWillReceiveProps(nextProps) {
    this.totalItems.replace(nextProps.data);
  }

  @computed get currentPageItems() {
    const pages = chunk(this.filteredItems, this.props.pageSize);
    return pages[this.currentPage] || pages.slice(-1)[0];
  }

  @computed get pageCount() {
    return Math.ceil(this.filteredItems.length / this.props.pageSize); 
  }

  @computed get fuzzySearcher() {
    return new Fuse(this.totalItems, {
      shouldSort: false,
      location: 0,
      distance: 100,
      maxPatternLength: 100,
      threshold: 0.2,
      keys: this.props.fieldsToSearch,
    });
  }

  @computed get filteredItems() {
    return !this.searchTerm
      ? this.totalItems
      : this.fuzzySearcher.search(this.searchTerm);
  }

  updateSearchTerm = (e) => {
    this.searchTerm = e.target.value;
  }

  render = () => {
    const props = this.props;
    return (
      <div>
        <label>Search:
          <input
            type="text"
            onChange={this.updateSearchTerm}
            onKeyUp={this.updateSearchTerm}
          />
        </label>
        <BootstrapTable
          {...props}
          data={this.currentPageItems}
        />
        
        {this.pageCount > 1 && (
          <div>
            {range(this.pageCount).map(n => (
              <button
                disabled={n === this.currentPage}
                onClick={()=>{ this.currentPage = n }}
                key={n}
              >
                Page {n+1}
              </button>
            ))}
          </div>
        )}
      
      </div>
      );
  }
}

export default DataTable;
