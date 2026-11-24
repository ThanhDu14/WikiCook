import { groupByDish } from './byDish';
import { formatQuantity } from './format';
import type { ShoppingList } from './types';

/**
 * Plain text of the list for Zalo, SMS or notes (US5), grouped like the screen: the title, then each dish with its
 * ingredients as "[ ]" lines ("[x]" once bought), quantity after a dash.
 */
export function listToPlainText(list: ShoppingList, isChecked: (clientUuid: string, fallback: boolean) => boolean): string {
  const lines: string[] = [list.title];
  for (const section of groupByDish(list)) {
    lines.push('', section.times > 1 ? `${section.title} ×${section.times}` : section.title);
    for (const { item, quantity, unit } of section.entries) {
      const mark = isChecked(item.clientUuid, item.isChecked) ? '[x]' : '[ ]';
      const amount = formatQuantity(quantity, unit);
      lines.push(amount ? `${mark} ${item.displayName} — ${amount}` : `${mark} ${item.displayName}`);
    }
  }
  return lines.join('\n');
}
