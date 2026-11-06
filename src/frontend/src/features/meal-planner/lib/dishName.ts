/**
 * The dish of a recipe title, without the "how to" part many titles carry after a colon
 * ("Bánh cống Sóc Trăng: cách làm giòn rụm tại nhà" → "Bánh cống Sóc Trăng"). Used for what the planner shows;
 * the full title stays in the `title` tooltip.
 */
export function dishName(title: string): string {
  const full = title.trim();
  const dish = full.split(':', 1)[0].trim();
  return dish || full;
}
