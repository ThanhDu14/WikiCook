/** Units that are an amount by themselves: a number in front ("300 vừa đủ") means nothing. */
const QUALITATIVE_UNITS = new Set(['vừa đủ', 'tùy thích', 'một ít', 'ít']);

const NUMBER = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });

/** "1 kg", "1,5 chai", or the unit alone for qualitative amounts such as "vừa đủ" (US2 scenario 5). */
export function formatQuantity(quantity: number | null | undefined, unit: string | null | undefined): string {
  if (unit && QUALITATIVE_UNITS.has(unit.trim().toLowerCase())) return unit.trim();
  const amount = quantity == null ? '' : NUMBER.format(quantity);
  return [amount, unit ?? ''].filter(Boolean).join(' ');
}
