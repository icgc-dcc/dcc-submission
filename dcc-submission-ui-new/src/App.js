import React, { Component } from 'react';
import Breadcrumbs from 'react-breadcrumbs';
import Modal from 'react-modal';
import './App.css';

import {observable, action} from 'mobx';
import {observer} from 'mobx-react';

const modal = observable({
  isOpen: false,
  contents: <div>teststseadsf<sup>23423423</sup></div>
});

export const openModal = action('set modal contents and open', function (contents) {
  modal.contents = contents;
  modal.isOpen = true;
});

export const closeModal = action('close modal', function () {
  modal.isOpen = false;
});

@observer
class App extends Component {
  render() {
    return (
      <div className="App container">
        <Breadcrumbs
          routes={this.props.routes}
          params={this.props.params}
          excludes={['App']}
        />
        {this.props.children}
        <Modal
          className={`Modal modal-dialog`}
          isOpen={modal.isOpen}
          onRequestClose={closeModal}
          closeTimeoutMS={250}
          shouldCloseOnOverlayClick={true}
        >
          {modal.contents}
        </Modal>
      </div>
    );
  }
}

export default App;
