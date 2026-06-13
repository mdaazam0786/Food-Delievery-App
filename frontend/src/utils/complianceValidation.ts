/** Indian GST: 15-char alphanumeric e.g. 22AAAAA0000A1Z5 */
const GST_PATTERN = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;

/** FSSAI license: exactly 14 digits */
const FSSAI_PATTERN = /^\d{14}$/;

export function formatGstInput(raw: string): string {
  return raw.toUpperCase().replace(/[^0-9A-Z]/g, '').slice(0, 15);
}

export function formatFssaiInput(raw: string): string {
  return raw.replace(/\D/g, '').slice(0, 14);
}

export function validateGst(gst: string): string | null {
  const v = gst.trim().toUpperCase();
  if (!v) return 'GST number is required.';
  if (v.length !== 15) return 'GST number must be 15 characters.';
  if (!GST_PATTERN.test(v)) return 'Enter a valid GST number (e.g. 22AAAAA0000A1Z5).';
  return null;
}

export function validateFssai(fssai: string): string | null {
  const v = fssai.trim();
  if (!v) return 'FSSAI license number is required.';
  if (!FSSAI_PATTERN.test(v)) return 'FSSAI must be exactly 14 digits.';
  return null;
}

/** Restaurant is fully provisioned when address + coordinates are set. */
export function isRestaurantConfigured(r: {
  addressText?: string;
  latitude?:  number;
  longitude?: number;
} | null | undefined): boolean {
  if (!r) return false;
  if (!r.addressText?.trim()) return false;
  if (r.latitude == null || r.longitude == null) return false;
  if (r.latitude === 0 && r.longitude === 0) return false;
  return true;
}
