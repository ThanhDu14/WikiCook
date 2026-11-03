const NUMBER = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });

/** "1 kg", "1,5 chai", or the unit alone for qualitative amounts such as "vừa đủ" (US2 scenario 5). */
export function formatQuantity(quantity: number | null | undefined, unit: string | null | undefined): string {
  const amount = quantity == null ? '' : NUMBER.format(quantity);
  return [amount, unit ?? ''].filter(Boolean).join(' ');
}
