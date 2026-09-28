import { Link } from 'react-router-dom';

import { CategoryBadge, EventStatusBadge } from './Badge';
import SeatAvailability from './SeatAvailability';
import { formatDate, formatTime } from '../utils/format';

/**
 * Event card used everywhere events are listed.
 *
 * Shows the details the UI design asks for: category, title, date, time, venue,
 * seat availability and a call to action.
 */
export default function EventCard({ event, showStatus = false }) {
  return (
    <article className="event-card">
      <div className="row-between">
        <CategoryBadge category={event.category} />
        {showStatus && <EventStatusBadge status={event.status} />}
      </div>

      <h3 className="event-card-title">{event.title}</h3>

      <div className="event-card-meta">
        <span className="meta-item">
          <span className="meta-icon" aria-hidden="true">
            📅
          </span>
          {formatDate(event.date)}
        </span>
        <span className="meta-item">
          <span className="meta-icon" aria-hidden="true">
            🕐
          </span>
          {formatTime(event.startTime)}
        </span>
        <span className="meta-item">
          <span className="meta-icon" aria-hidden="true">
            📍
          </span>
          {event.venue}
        </span>
      </div>

      <SeatAvailability
        availableSeats={event.availableSeats}
        capacity={event.capacity}
        registeredCount={event.registeredCount}
        full={event.full}
        almostFull={event.almostFull}
      />

      <div className="event-card-footer">
        <Link className="btn btn-secondary btn-block" to={`/events/${event.id}`}>
          View details
        </Link>
      </div>
    </article>
  );
}
