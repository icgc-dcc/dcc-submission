import { fetchHeaders } from '~/utils';

const PERFORM_RELEASE_ERRORS = {
  'InvalidName': 'A release name must only use letters[a-z], numbers(0-9), underscores(_) and dashes(-)',
  'DuplicateReleaseName': 'That release name has already been used.',
  'SignedOffSubmissionRequired': 'The release needs at least one SIGNED OFF submission before it can be COMPLETED.',
  _default: 'An error occurred. Please contact Support for assistance.'
};

export async function performRelease ({nextReleaseName}) {
  const response = await fetch('/ws/nextRelease/', {
    method: 'POST',
    headers: fetchHeaders.get(),
    body: JSON.stringify({
      name: nextReleaseName || '',
      submissions: [],
    }),
  });
  const responseData = await response.json();
  if (!response.ok) {
    console.log('response not ok', responseData);
    throw new Error(PERFORM_RELEASE_ERRORS[responseData.code] || PERFORM_RELEASE_ERRORS._default);
  }
  return responseData;
}
