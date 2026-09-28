/**
 * The one button in the application.
 *
 * `loading` disables the control and marks it busy, which prevents the classic
 * double-submit and tells assistive technology that work is in progress.
 */
export default function Button({
  variant = 'primary',
  size,
  block = false,
  loading = false,
  disabled = false,
  type = 'button',
  children,
  ...rest
}) {
  const classes = ['btn', `btn-${variant}`, size === 'sm' && 'btn-sm', block && 'btn-block']
    .filter(Boolean)
    .join(' ');

  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <span className="btn-spinner" aria-hidden="true" />}
      {children}
    </button>
  );
}
