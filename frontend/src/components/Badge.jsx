import { statusLabel, statusTone } from '../utils/format';

/** Generic pill label. */
export default function Badge({ tone = 'neutral', children }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

/**
 * Event status pill.
 *
 * The word is always present next to the colour, so status is never communicated
 * by colour alone.
 */
export function EventStatusBadge({ status }) {
  if (!status) return null;
  return <Badge tone={statusTone(status)}>{statusLabel(status)}</Badge>;
}

/** Registration status pill, using the same wording rules. */
export function RegistrationStatusBadge({ status }) {
  if (!status) return null;
  return <Badge tone={statusTone(status)}>{statusLabel(status)}</Badge>;
}

/** Category pill, used on event cards and headers. */
export function CategoryBadge({ category }) {
  if (!category) return null;
  return <Badge tone="primary">{statusLabel(category)}</Badge>;
}

/** Small "Almost full" / "Full" indicator. */
export function AvailabilityBadge({ full, almostFull }) {
  if (full) return <Badge tone="error">Full</Badge>;
  if (almostFull) return <Badge tone="warning">Almost full</Badge>;
  return null;
}
