import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import LoadingSpinner from '../components/LoadingSpinner';
import { CategoryBadge, RegistrationStatusBadge } from '../components/Badge';
import { useToast } from '../context/useToast';
import { registrationService } from '../services/registrationService';
import { formatDate, formatTimeRange } from '../utils/format';
import useAsync from '../utils/useAsync';

const TABS = [
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'completed', label: 'Completed' },
  { id: 'cancelled', label: 'Cancelled' },
];

export default function MyRegistrations() {
  const toast = useToast();
  const navigate = useNavigate();

  const [tab, setTab] = useState('upcoming');
  const [pendingCancel, setPendingCancel] = useState(null);
  const [busy, setBusy] = useState(false);
  const [today] = useState(() => new Date().toISOString().slice(0, 10));

  const { data, loading, error, reload } = useAsync(
    () => registrationService.myRegistrations(),
    [],
  );

  const grouped = useMemo(() => {
    const all = data ?? [];

    return {
      upcoming: all.filter((item) => item.status !== 'CANCELLED' && item.eventDate >= today),
      completed: all.filter((item) => item.status !== 'CANCELLED' && item.eventDate < today),
      cancelled: all.filter((item) => item.status === 'CANCELLED'),
    };
  }, [data, today]);

  const handleCancel = async () => {
    if (!pendingCancel) return;

    setBusy(true);
    try {
      await registrationService.cancel(pendingCancel.eventId);
      toast.success('Registration cancelled');
      setPendingCancel(null);
      await reload();
    } catch (cancelError) {
      toast.error(cancelError.message);
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading your registrations…" />;
  }

  if (error) {
    return (
      <ErrorState
        title="We couldn't load your registrations"
        message={error.message}
        onRetry={reload}
      />
    );
  }

  const rows = grouped[tab];

  return (
    <>
      <div className="page-header">
        <h1>My registrations</h1>
        <p className="page-subtitle">Every event you have registered for, with its QR pass.</p>
      </div>

      <div className="tabs" role="tablist" aria-label="Registration filters">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={tab === item.id}
            className={`tab${tab === item.id ? ' active' : ''}`}
            onClick={() => setTab(item.id)}
          >
            {item.label} ({grouped[item.id].length})
          </button>
        ))}
      </div>

      {rows.length === 0 ? (
        <EmptyState
          icon="🎫"
          title={
            tab === 'upcoming'
              ? "You haven't registered for any events yet"
              : `No ${tab} registrations`
          }
          message={
            tab === 'upcoming'
              ? 'Explore what is coming up and reserve your seat.'
              : undefined
          }
          action={
            tab === 'upcoming' ? (
              <Button onClick={() => navigate('/events')}>Explore events</Button>
            ) : null
          }
        />
      ) : (
        <div className="stack">
          {rows.map((registration) => (
            <div key={registration.registrationId} className="card card-padded">
              <div className="row-between">
                <div style={{ minWidth: 0 }}>
                  <div className="row" style={{ marginBottom: 'var(--space-2)' }}>
                    <CategoryBadge category={registration.category} />
                    <RegistrationStatusBadge status={registration.status} />
                  </div>

                  <h3 style={{ marginBottom: 'var(--space-1)' }}>{registration.eventTitle}</h3>

                  <div className="text-caption">
                    {formatDate(registration.eventDate)} ·{' '}
                    {formatTimeRange(registration.startTime, registration.endTime)} ·{' '}
                    {registration.venue}
                  </div>
                </div>

                <div className="row">
                  <Link to={`/my-registrations/${registration.registrationId}/pass`}>
                    View pass
                  </Link>

                  {registration.status !== 'CANCELLED' && registration.status !== 'ATTENDED' && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => setPendingCancel(registration)}
                    >
                      Cancel
                    </Button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingCancel)}
        title="Cancel registration?"
        message={
          pendingCancel
            ? `You will lose your seat for "${pendingCancel.eventTitle}". You can register again later if space remains.`
            : ''
        }
        confirmLabel="Cancel registration"
        cancelLabel="Keep my seat"
        tone="danger"
        busy={busy}
        onConfirm={handleCancel}
        onCancel={() => setPendingCancel(null)}
      />
    </>
  );
}
