export function readApiEnvelope(body) {
  if (
    body === null
    || typeof body !== 'object'
    || typeof body.success !== 'boolean'
    || typeof body.message !== 'string'
    || !Object.prototype.hasOwnProperty.call(body, 'data')
  ) {
    return null;
  }

  return body;
}
