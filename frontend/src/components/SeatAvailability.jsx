import { AvailabilityBadge } from './Badge';

/**
 * Seat availability.
 *
 * The plan documents render availability two different ways ("120 / 200 seats
 * available" versus capacity minus registrations), which contradicts the rule that
 * availability is `capacity - registeredCount`. This component always shows the
 * computed figure so the number can never disagree with the business rule, and the
 * bar's fill ratio shows how full the event is.
 */
export default function SeatAvailability({ availableSeats, capacity, registeredCount, full, almostFull }) {
  if (capacity === null || capacity === undefined) return null;

  const available = Math.max(0, availableSeats ?? capacity - (registeredCount ?? 0));
  const filledPercent = capacity === 0 ? 100 : Math.min(100, Math.round(((registeredCount ?? 0) / capacity) * 100));

  const fillTone = full ? 'full' : almostFull ? 'almost' : 'available';

  return (
    <div className="seat-block">
      <div className="seat-line">
        <span className="seat-count">
          {available} of {capacity} seats available
        </span>
        <AvailabilityBadge full={full} almostFull={almostFull} />
      </div>

      <div
        className="seat-track"
        role="progressbar"
        aria-valuenow={filledPercent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${filledPercent}% of seats taken`}
      >
        <div className={`seat-fill seat-fill-${fillTone}`} style={{ width: `${filledPercent}%` }} />
      </div>
    </div>
  );
}
