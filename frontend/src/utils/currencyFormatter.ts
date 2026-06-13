/**
 * Currency formatting utilities for Indian Rupee (₹)
 *
 * Provides consistent formatting across the application using
 * localized Indian accounting notation systems.
 */

/**
 * Formats a number as Indian Rupee with proper symbol and notation.
 * Uses Intl.NumberFormat for locale-aware formatting.
 *
 * @param amount - The amount to format (number or BigDecimal string)
 * @returns Formatted string with ₹ symbol, e.g., "₹1,234.50"
 *
 * @example
 * formatINR(1234.56) → "₹1,234.56"
 * formatINR('1234.56') → "₹1,234.56"
 * formatINR(0) → "₹0.00"
 */
export function formatINR(amount: number | string): string {
  const numAmount = typeof amount === 'string' ? parseFloat(amount) : amount;
  
  if (isNaN(numAmount)) {
    return '₹0.00';
  }

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numAmount);
}

/**
 * Formats a number as rupee amount without the ₹ symbol.
 * Useful for inline display where the symbol is already present.
 *
 * @param amount - The amount to format
 * @returns Formatted string without symbol, e.g., "1,234.50"
 *
 * @example
 * formatINRAmount(1234.56) → "1,234.50"
 */
export function formatINRAmount(amount: number | string): string {
  const formatted = formatINR(amount);
  // Remove the ₹ symbol (it's typically at the start or after locale prefix)
  return formatted.replace('₹', '').trim();
}

/**
 * Formats rupee for quick inline display with 2 decimal places.
 * Equivalent to: ₹{price.toFixed(2)} but with proper formatting.
 *
 * @param amount - The amount to format
 * @returns Formatted string, e.g., "₹1234.56"
 *
 * @example
 * formatQuickINR(1234.567) → "₹1234.57"
 * formatQuickINR(100) → "₹100.00"
 */
export function formatQuickINR(amount: number | string): string {
  const numAmount = typeof amount === 'string' ? parseFloat(amount) : amount;
  
  if (isNaN(numAmount)) {
    return '₹0.00';
  }

  return `₹${numAmount.toFixed(2)}`;
}

/**
 * Extracts the numeric value from a formatted rupee string.
 *
 * @param formatted - Formatted string like "₹1,234.50"
 * @returns Numeric value, e.g., 1234.50
 *
 * @example
 * parseINR("₹1,234.50") → 1234.50
 * parseINR("1234.50") → 1234.50
 */
export function parseINR(formatted: string): number {
  const cleaned = formatted
    .replace(/₹/g, '')
    .replace(/,/g, '')
    .trim();
  
  const parsed = parseFloat(cleaned);
  return isNaN(parsed) ? 0 : parsed;
}

/**
 * Compares two currency amounts for equality (accounting for floating point precision).
 *
 * @param amount1 - First amount to compare
 * @param amount2 - Second amount to compare
 * @param precision - Number of decimal places to consider (default: 2)
 * @returns True if amounts are equal within precision
 *
 * @example
 * compareINR(1234.56, 1234.56) → true
 * compareINR(1234.567, 1234.564, 2) → true
 */
export function compareINR(
  amount1: number | string,
  amount2: number | string,
  precision: number = 2
): boolean {
  const num1 = typeof amount1 === 'string' ? parseFloat(amount1) : amount1;
  const num2 = typeof amount2 === 'string' ? parseFloat(amount2) : amount2;
  
  const factor = Math.pow(10, precision);
  return Math.round(num1 * factor) === Math.round(num2 * factor);
}
