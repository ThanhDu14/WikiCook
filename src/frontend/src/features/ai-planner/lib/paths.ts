/** The shopping list import of a week (spec 040, route of the contract; ⛓ D). */
export const shoppingImportPath = (weekStart: string) => `/shopping-list/import?weekStart=${encodeURIComponent(weekStart)}`;
