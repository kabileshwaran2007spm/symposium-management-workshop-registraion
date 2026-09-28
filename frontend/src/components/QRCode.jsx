import { QRCodeSVG } from 'qrcode.react';

/**
 * The registration QR code.
 *
 * The encoded value is the opaque registration token and nothing else. A printed
 * or photographed pass therefore reveals no student data to whoever finds it — the
 * organizer's scanner only ever sees an identifier it must still validate against
 * the server.
 */
export default function QRCode({ value, size = 190, label = 'Registration QR code' }) {
  if (!value) {
    return (
      <div className="pass-qr">
        <p className="text-caption" style={{ margin: 0 }}>
          No QR pass available for this registration.
        </p>
      </div>
    );
  }

  return (
    <div className="pass-qr">
      <QRCodeSVG
        value={value}
        size={size}
        level="M"
        marginSize={0}
        title={label}
        aria-label={label}
      />
    </div>
  );
}
