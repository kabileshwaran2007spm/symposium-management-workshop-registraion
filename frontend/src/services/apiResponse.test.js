import assert from 'node:assert/strict';
import test from 'node:test';

import { readApiEnvelope } from './apiResponse.js';

test('accepts the API success envelope with an empty paginated event list', () => {
  const response = {
    success: true,
    message: 'Events retrieved successfully',
    data: {
      content: [],
      page: 0,
      size: 3,
      totalElements: 0,
      totalPages: 0,
      last: true,
    },
  };

  assert.deepEqual(readApiEnvelope(response), response);
});

test('accepts successful envelope with null data', () => {
  const response = {
    success: true,
    message: 'Logged out',
    data: null,
  };

  assert.deepEqual(readApiEnvelope(response), response);
});

test('recognizes a well-formed error envelope without treating it as success', () => {
  const response = {
    success: false,
    message: 'Authentication is required',
    data: null,
  };

  assert.deepEqual(readApiEnvelope(response), response);
  assert.equal(readApiEnvelope(response).success, false);
});

test('rejects malformed or non-envelope JSON responses', () => {
  for (const body of [null, [], 'not json', {}, { success: true, data: [] },
    { success: true, message: 'Missing data' }, { success: 'true', message: 'Invalid', data: [] }]) {
    assert.equal(readApiEnvelope(body), null);
  }
});
