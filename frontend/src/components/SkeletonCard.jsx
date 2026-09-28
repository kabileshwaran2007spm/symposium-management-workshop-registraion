/**
 * Skeleton placeholder shown while event cards load.
 *
 * It mirrors the real card's shape so the layout does not jump when the data
 * arrives. Hidden from assistive technology, since the surrounding region already
 * announces that it is loading.
 */
export default function SkeletonCard() {
  return (
    <div className="skeleton-card" aria-hidden="true">
      <div className="skeleton" style={{ height: 20, width: '40%' }} />
      <div className="skeleton" style={{ height: 22, width: '85%' }} />
      <div className="skeleton" style={{ height: 14, width: '60%' }} />
      <div className="skeleton" style={{ height: 14, width: '45%' }} />
      <div className="skeleton" style={{ height: 8, width: '100%' }} />
      <div className="skeleton" style={{ height: 38, width: '100%' }} />
    </div>
  );
}

/** A grid of skeleton cards, sized to the page it is standing in for. */
export function SkeletonGrid({ count = 6 }) {
  return (
    <div className="grid-cards">
      {Array.from({ length: count }, (_, index) => (
        <SkeletonCard key={index} />
      ))}
    </div>
  );
}
