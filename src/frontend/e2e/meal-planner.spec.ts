import { expect, test, type Page } from '@playwright/test';
import { FakeApi, THIS_WEEK } from './fakeApi';

/** Opens the planner on a fake backend holding `api`'s weeks. */
async function open(page: Page, api: FakeApi, path = '/meal-planner') {
  await api.install(page);
  await page.goto(path);
  return api;
}

async function pick(page: Page, dialogName: string, recipe: string) {
  const picker = page.getByRole('dialog', { name: dialogName });
  await picker.getByRole('listitem').filter({ hasText: recipe }).getByRole('button', { name: 'Chọn' }).click();
  await expect(picker).toBeHidden();
}

const NEXT_WEEK = '2026-10-26';

test.describe('US1 – plan the week', () => {
  test('adds dishes to an empty meal, up to the end of the meal (US1-2, US1-11)', async ({ page }) => {
    const api = await open(page, new FakeApi());

    await expect(page.getByRole('heading', { level: 1, name: '19/10 – 25/10/2026' })).toBeVisible();
    await expect(page.getByRole('columnheader').nth(2)).toHaveAttribute('aria-current', 'date');
    await page.getByRole('button', { name: 'Thêm món: Thứ Tư, bữa trưa' }).first().click();
    await pick(page, 'Chọn món · Trưa T4', 'Canh chua cá lóc');
    await page.getByRole('button', { name: 'Thêm món: Thứ Tư, bữa trưa' }).click();
    await pick(page, 'Chọn món · Trưa T4', 'Rau muống xào tỏi');

    await expect(page.getByRole('group', { name: 'Thứ Tư, bữa trưa, Canh chua cá lóc' })).toBeVisible();
    await expect(page.getByRole('group', { name: 'Thứ Tư, bữa trưa, Rau muống xào tỏi' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'WEDNESDAY/LUNCH')).toEqual(['Canh chua cá lóc', 'Rau muống xào tỏi']);
  });

  test('changes the servings of a dish from its menu (US1-7)', async ({ page }) => {
    await open(page, new FakeApi({ [THIS_WEEK]: { 'MONDAY/DINNER': [{ recipeId: 42, servings: 4 }] } }));

    await page.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }).click();
    const menu = page.getByRole('dialog', { name: 'Canh chua cá lóc' });
    await menu.getByRole('button', { name: 'Thêm' }).click();
    await expect(menu.getByRole('status')).toHaveText('5 người');
    await menu.getByRole('button', { name: 'Đóng' }).click();

    await expect(page.getByText('40 phút · 5 phần')).toBeVisible();
    await page.reload();
    await expect(page.getByText('40 phút · 5 phần')).toBeVisible();
  });
});

test.describe('US3 – rearrange, replace and remove', () => {
  const seeded = () =>
    new FakeApi({ [THIS_WEEK]: { 'MONDAY/LUNCH': [{ recipeId: 42 }, { recipeId: 8 }], 'TUESDAY/DINNER': [{ recipeId: 7 }] } });

  test('moves a dish to another meal with the keyboard only (US3-3, NFR-003)', async ({ page }) => {
    const api = await open(page, seeded());

    await page.getByRole('button', { name: 'Tùy chọn: Cá kho tộ' }).focus();
    await page.keyboard.press('Enter');
    await page.getByRole('dialog', { name: 'Cá kho tộ' }).getByRole('button', { name: 'Chuyển sang…' }).focus();
    await page.keyboard.press('Enter');
    const move = page.getByRole('dialog', { name: 'Chuyển “Cá kho tộ”' });
    await move.getByRole('button', { name: 'Thứ Sáu, bữa tối, trống' }).focus();
    await page.keyboard.press('Enter');
    await move.getByRole('button', { name: 'Chuyển đến Tối T6' }).focus();
    await page.keyboard.press('Enter');

    await expect(page.getByRole('group', { name: 'Thứ Sáu, bữa tối, Cá kho tộ' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'FRIDAY/DINNER')).toEqual(['Cá kho tộ']);
    expect(api.names(THIS_WEEK, 'TUESDAY/DINNER')).toEqual([]);
  });

  test('moves a dish up inside its meal (US3-4)', async ({ page }) => {
    const api = await open(page, seeded());

    await page.getByRole('button', { name: 'Tùy chọn: Rau muống xào tỏi' }).click();
    await page.getByRole('button', { name: 'Lên trên' }).click();

    await expect.poll(() => api.names(THIS_WEEK, 'MONDAY/LUNCH')).toEqual(['Rau muống xào tỏi', 'Canh chua cá lóc']);
  });

  test('replaces a dish and keeps its place, then removes one (US3-5, US3-6)', async ({ page }) => {
    const api = await open(page, seeded());

    await page.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }).click();
    await page.getByRole('button', { name: 'Đổi món' }).click();
    await pick(page, 'Chọn món · Trưa T2', 'Phở gà');
    await expect.poll(() => api.names(THIS_WEEK, 'MONDAY/LUNCH')).toEqual(['Phở gà', 'Rau muống xào tỏi']);

    await page.getByRole('button', { name: 'Tùy chọn: Rau muống xào tỏi' }).click();
    await page.getByRole('button', { name: 'Xóa khỏi thực đơn' }).click();
    await expect(page.getByRole('group', { name: /Rau muống/ })).toHaveCount(0);
    expect(api.names(THIS_WEEK, 'MONDAY/LUNCH')).toEqual(['Phở gà']);
  });

  test('clears a whole day after asking (US3-7)', async ({ page }) => {
    const api = await open(page, seeded());

    await page.getByRole('button', { name: 'Xóa cả ngày: Thứ Hai' }).focus();
    await page.keyboard.press('Enter');
    await page.getByRole('alertdialog', { name: 'Xóa mọi món của Thứ Hai?' }).getByRole('button', { name: 'Xóa cả ngày' }).click();

    await expect(page.getByRole('group', { name: 'Thứ Hai, bữa trưa, trống' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'MONDAY/LUNCH')).toEqual([]);
  });
});

test.describe('US4 – weeks and the empty week', () => {
  test('goes to next week, shows the empty state, adds a dish and finds it again (quickstart Q13, Q14)', async ({ page }) => {
    const api = await open(page, new FakeApi({ [THIS_WEEK]: { 'MONDAY/LUNCH': [{ recipeId: 42 }] } }));

    await page.getByRole('button', { name: 'Tuần sau' }).click();
    await expect(page).toHaveURL(/\?week=2026-10-26$/);
    await expect(page.getByRole('heading', { level: 1, name: '26/10 – 01/11/2026' })).toBeVisible();
    const empty = page.getByRole('region', { name: 'Tuần này chưa có món' });
    await expect(empty).toBeVisible();
    await expect(page.getByRole('button', { name: /Tạo bằng AI/ })).toHaveCount(0);

    await empty.getByRole('button', { name: 'Thêm món' }).click();
    await pick(page, 'Chọn món · Sáng T2', 'Bánh mì trứng');
    await expect(empty).toBeHidden();

    await page.getByRole('button', { name: 'Tuần này' }).click();
    await expect(page).toHaveURL(/\/meal-planner$/);
    await expect(page.getByRole('group', { name: 'Thứ Hai, bữa trưa, Canh chua cá lóc' })).toBeVisible();
    await page.getByRole('button', { name: 'Tuần sau' }).click();
    await expect(page.getByRole('group', { name: 'Thứ Hai, bữa sáng, Bánh mì trứng' })).toBeVisible();
    expect(api.names(NEXT_WEEK, 'MONDAY/BREAKFAST')).toEqual(['Bánh mì trứng']);
  });

  test('opens the week of ?week= directly, and a past week can still be edited (US4-5)', async ({ page }) => {
    const api = await open(page, new FakeApi({ '2026-09-28': { 'FRIDAY/DINNER': [{ recipeId: 7 }] } }), '/meal-planner?week=2026-09-28');

    await expect(page.getByRole('heading', { level: 1, name: '28/09 – 04/10/2026' })).toBeVisible();
    await page.getByRole('button', { name: 'Thêm món: Thứ Sáu, bữa tối' }).click();
    await pick(page, 'Chọn món · Tối T6', 'Rau muống xào tỏi');
    await expect.poll(() => api.names('2026-09-28', 'FRIDAY/DINNER')).toEqual(['Cá kho tộ', 'Rau muống xào tỏi']);
  });
});

test.describe('US5 – shopping list', () => {
  test('is disabled for a week without available dishes (US5-2)', async ({ page }) => {
    await open(page, new FakeApi());

    await expect(page.getByRole('button', { name: 'Đi chợ' })).toBeDisabled();
  });

  test('says how many dishes are left out, then opens the import (US5-1, US5-3)', async ({ page }) => {
    const api = await open(
      page,
      new FakeApi({ [THIS_WEEK]: { 'MONDAY/LUNCH': [{ recipeId: 42 }], 'TUESDAY/DINNER': [{ recipeId: 99, available: false, name: 'Bò lúc lắc' }] } }),
    );

    await page.getByRole('button', { name: 'Đi chợ' }).click();
    const ask = page.getByRole('alertdialog', { name: '1 món không còn khả dụng sẽ không được đưa vào danh sách đi chợ.' });
    await ask.getByRole('button', { name: 'Tiếp tục' }).click();

    await expect(page).toHaveURL(new RegExp(`/shopping-list/import\\?weekStart=${THIS_WEEK}$`));
    expect(api.calls).toContain(`GET /meal-plans/${THIS_WEEK}/shopping-handoff`);
  });
});

test.describe('US6 – recipes no longer available', () => {
  test('keeps the name, explains the removal and offers Thay món / Xóa (US6-1, US6-2)', async ({ page }) => {
    const api = await open(page, new FakeApi({ [THIS_WEEK]: { 'TUESDAY/DINNER': [{ recipeId: 99, available: false, name: 'Bò lúc lắc' }] } }));

    const dish = page.getByRole('group', { name: 'Thứ Ba, bữa tối, Bò lúc lắc' });
    await expect(dish.getByText('Không còn khả dụng')).toBeVisible();
    await expect(dish.getByRole('link')).toHaveCount(0);
    await dish.getByRole('button', { name: 'Bò lúc lắc', exact: true }).click();
    await expect(dish.getByRole('status')).toHaveText('Công thức này đã bị gỡ khỏi WikiCook. Hãy thay bằng món khác hoặc xóa.');

    await dish.getByRole('button', { name: 'Thay món: Bò lúc lắc' }).click();
    await pick(page, 'Chọn món · Tối T3', 'Cá kho tộ');
    await expect(page.getByRole('group', { name: 'Thứ Ba, bữa tối, Cá kho tộ' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'TUESDAY/DINNER')).toEqual(['Cá kho tộ']);
  });
});

test.describe('US7 – connection', () => {
  test('replaces the calendar while offline and reloads it when the connection returns (US7-3, US7-5)', async ({ page, context }) => {
    const api = await open(page, new FakeApi({ [THIS_WEEK]: { 'MONDAY/LUNCH': [{ recipeId: 42 }] } }));
    await expect(page.getByRole('table', { name: 'Thực đơn' })).toBeVisible();

    await context.setOffline(true);
    await page.evaluate(() => window.dispatchEvent(new Event('offline')));
    await expect(page.getByRole('heading', { name: 'Cần kết nối mạng' })).toBeVisible();
    await expect(page.getByRole('table')).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Tùy chọn/ })).toHaveCount(0);

    const loads = api.calls.filter((c) => c === `GET /meal-plans/${THIS_WEEK}`).length;
    await context.setOffline(false);
    await page.evaluate(() => window.dispatchEvent(new Event('online')));
    await expect(page.getByRole('table', { name: 'Thực đơn' })).toBeVisible();
    expect(api.calls.filter((c) => c === `GET /meal-plans/${THIS_WEEK}`).length).toBe(loads + 1);
    expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  });

  test('a save that fails for the network is undone and shows the offline notice (US7-4)', async ({ page }) => {
    const api = new FakeApi({ [THIS_WEEK]: { 'MONDAY/LUNCH': [{ recipeId: 42 }] } });
    await open(page, api);
    await page.route('**/api/v1/meal-plans/*/items/*', (route) => route.abort('internetdisconnected'));

    await page.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }).click();
    await page.getByRole('button', { name: 'Xóa khỏi thực đơn' }).click();

    await expect(page.getByText('Thay đổi chưa được lưu. Vui lòng thử lại.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Cần kết nối mạng' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'MONDAY/LUNCH')).toEqual(['Canh chua cá lóc']);
  });
});

test.describe('US7 – narrow screen @phone', () => {
  test('shows one day at a time, today first, and moves between days (US7-1, FR-026)', async ({ page }) => {
    await open(page, new FakeApi({ [THIS_WEEK]: { 'WEDNESDAY/LUNCH': [{ recipeId: 42 }], 'THURSDAY/DINNER': [{ recipeId: 7 }] } }));

    await expect(page.getByRole('heading', { level: 2, name: 'Thứ Tư, 21/10' })).toBeVisible();
    await expect(page.getByRole('table')).toHaveCount(0);
    await expect(page.getByRole('group', { name: 'Thứ Tư, bữa trưa, Canh chua cá lóc' })).toBeVisible();
    await page.getByRole('button', { name: 'Ngày sau' }).tap();
    await expect(page.getByRole('group', { name: 'Thứ Năm, bữa tối, Cá kho tộ' })).toBeVisible();
    await page.getByRole('button', { name: 'Chủ nhật 25/10' }).tap();
    await expect(page.getByRole('heading', { level: 2, name: 'Chủ nhật, 25/10' })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 3)).toBe(true);
  });

  test('moves a dish with "Chuyển sang…" from the day view, with 44 px controls (US7-1, NFR-002)', async ({ page }) => {
    const api = await open(page, new FakeApi({ [THIS_WEEK]: { 'WEDNESDAY/LUNCH': [{ recipeId: 42 }] } }));

    const options = page.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' });
    const box = await options.boundingBox();
    expect(box && Math.min(box.width, box.height)).toBeGreaterThanOrEqual(44);
    for (const day of await page.getByRole('group', { name: 'Ngày trong tuần' }).getByRole('button').all()) {
      expect((await day.boundingBox())!.width).toBeGreaterThanOrEqual(44);
    }
    await options.tap();
    await page.getByRole('button', { name: 'Chuyển sang…' }).tap();
    const move = page.getByRole('dialog', { name: 'Chuyển “Canh chua cá lóc”' });
    await move.getByRole('button', { name: 'Thứ Tư, bữa tối, trống' }).tap();
    await move.getByRole('button', { name: 'Chuyển đến Tối T4' }).tap();

    await expect(page.getByRole('group', { name: 'Thứ Tư, bữa tối, Canh chua cá lóc' })).toBeVisible();
    expect(api.names(THIS_WEEK, 'WEDNESDAY/DINNER')).toEqual(['Canh chua cá lóc']);
  });
});
