import React from 'react';

import { openModal, closeModal } from '~/App';
import ActionButton from '~/common/components/ActionButton/ActionButton';

import CompleteReleaseModal from '~/Release/modals/CompleteReleaseModal';

function showCompleteReleaseModal({releaseName, onSuccess}) {
  openModal(<CompleteReleaseModal
    releaseName={releaseName}
    onClickClose={closeModal}
    onSuccess={onSuccess}
    />);
}

export default function ReleaseNowButton({ release, onSuccess, className }) {
  return (
    <ActionButton
      onClick={() => showCompleteReleaseModal({
        releaseName: release.name, 
        onSuccess: () => {
          closeModal();
          onSuccess();
        }})}
      className={className || `m-btn mini green-stripe`}
    >
      Release Now
    </ActionButton>
  );
}
