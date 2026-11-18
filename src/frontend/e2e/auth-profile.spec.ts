import { expect, test, type Page } from '@playwright/test';
import { AVATAR_URL, BANNED, COOK, FakeAuthApi, SUSPENDED } from './fakeAuthApi';

/** Installs the fake backend and opens `path`. */
async function open(page: Page, api: FakeAuthApi, path: string) {
  await api.install(page);
  await page.goto(path);
  return api;
}

const PNG = { name: 'me.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64') };

const header = (page: Page) => page.locator('header.wx-nav');

test.describe('US2 (010) – register, then check the mailbox', () => {
  test('registers and lands on "Kiểm tra hộp thư" with the address', async ({ page }) => {
    const api = await open(page, new FakeAuthApi(), '/register');

    await page.getByLabel('Tên hiển thị').fill('Thanh Du');
    await page.getByLabel('Email').fill('thanhdu@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('matkhau123');
    await page.getByLabel('Nhập lại mật khẩu').fill('matkhau123');
    await page.getByRole('button', { name: 'Tạo tài khoản' }).click();

    await expect(page).toHaveURL(/\/check-email$/);
    await expect(page.getByRole('heading', { name: 'Kiểm tra hộp thư' })).toBeVisible();
    await expect(page.getByText('thanhdu@gmail.com')).toBeVisible();
    expect(api.calls).toContain('POST /auth/register');
  });

  test('an e-mail that is already registered stays on the form and offers to sign in', async ({ page }) => {
    await open(page, new FakeAuthApi(), '/register');

    await page.getByLabel('Tên hiển thị').fill('Trùng');
    await page.getByLabel('Email').fill('trung@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('matkhau123');
    await page.getByLabel('Nhập lại mật khẩu').fill('matkhau123');
    await page.getByRole('button', { name: 'Tạo tài khoản' }).click();

    await expect(page.getByText('Email này đã được đăng ký.')).toBeVisible();
    await expect(page).toHaveURL(/\/register$/);
  });
});

test.describe('US1 (010) – sign in and out', () => {
  test('signs in and goes back to the page that asked for it', async ({ page }) => {
    const api = await open(page, new FakeAuthApi(), '/profile');

    await expect(page).toHaveURL(/\/login\?redirect=%2Fprofile$/); // a guest is sent to sign in first
    await page.getByLabel('Email').fill('thanhdu@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('matkhau123');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page).toHaveURL(/\/profile$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Thanh Du' })).toBeVisible();
    await expect(header(page).getByRole('link', { name: 'Tài khoản của tôi' })).toBeVisible();
    expect(api.calls).toContain('POST /auth/login');
  });

  test('a wrong password shows one message and keeps the page', async ({ page }) => {
    const api = new FakeAuthApi();
    await api.install(page);
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill({ status: 401, contentType: 'application/problem+json', json: { status: 401, title: 'x', code: 'INVALID_CREDENTIALS' } }),
    );
    await page.goto('/login');

    await page.getByLabel('Email').fill('thanhdu@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('sai');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page.getByText('Email hoặc mật khẩu không đúng')).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('signs out from the account menu: the server session ends and the header asks to sign in', async ({ page }) => {
    const api = await open(page, new FakeAuthApi({ signedIn: true }), '/profile');

    await page.getByRole('button', { name: 'Đăng xuất' }).click();

    await expect(page).toHaveURL(/localhost:\d+\/$/);
    await expect(header(page).getByRole('link', { name: 'Đăng nhập' })).toBeVisible();
    expect(api.calls).toContain('POST /auth/logout');
    expect(api.account).toBeNull();
  });
});

test.describe('FR-013 (010) – suspended and banned accounts', () => {
  test('a suspended account is not signed in: /account-status shows the end date and the reason', async ({ page }) => {
    await open(page, new FakeAuthApi({ login: 'suspended' }), '/login');

    await page.getByLabel('Email').fill('thanhdu@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('matkhau123');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page).toHaveURL(/\/account-status$/);
    const status = page.getByRole('alert');
    await expect(status).toContainText('Tài khoản bị đình chỉ đến');
    await expect(status).toContainText(`Lý do: ${SUSPENDED.reason}`);
  });

  test('a banned account sees the ban and the reason, with no end date', async ({ page }) => {
    await open(page, new FakeAuthApi({ login: 'banned' }), '/login');

    await page.getByLabel('Email').fill('thanhdu@gmail.com');
    await page.getByLabel('Mật khẩu', { exact: true }).fill('matkhau123');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();

    await expect(page).toHaveURL(/\/account-status$/);
    const status = page.getByRole('alert');
    await expect(status).toContainText('Tài khoản đã bị cấm');
    await expect(status).toContainText(`Lý do: ${BANNED.reason}`);
    await expect(page.getByText(/đình chỉ đến/)).toBeHidden();
  });

  test('Google sends a suspended person to /account-status with the reason in the URL fragment, which is then removed', async ({ page }) => {
    await open(page, new FakeAuthApi(), '/account-status#code=ACCOUNT_SUSPENDED&reason=Spam+qu%E1%BA%A3ng+c%C3%A1o&endsAt=2026-10-30T00%3A00%3A00Z');

    await expect(page.getByText('Lý do: Spam quảng cáo')).toBeVisible();
    await expect.poll(() => new URL(page.url()).hash).toBe('');
  });
});

test.describe('US2 (011) – edit the public profile', () => {
  test('saves the name, the bio and a picture; the header shows the new picture at once', async ({ page }) => {
    const api = await open(page, new FakeAuthApi({ signedIn: true }), '/profile/edit');

    await expect(header(page).getByRole('link', { name: 'Tài khoản của tôi' }).getByText('TD')).toBeVisible(); // initials first
    await page.getByLabel('Tên hiển thị').fill('Du Nguyễn');
    await page.getByLabel('Giới thiệu').fill('Mới nấu ăn');
    await page.getByTestId('avatar-input').setInputFiles(PNG);
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click();

    await expect(page.getByText('Đã lưu hồ sơ')).toBeVisible();
    expect(api.saved).toMatchObject([{ displayName: 'Du Nguyễn', bio: 'Mới nấu ăn', removeAvatar: false, avatar: true }]);
    await expect(header(page).getByRole('link', { name: 'Tài khoản của tôi' }).locator('img')).toHaveAttribute('src', AVATAR_URL);
    await expect(page.getByRole('navigation', { name: 'Tài khoản' }).locator('img')).toHaveAttribute('src', AVATAR_URL);
  });

  test('removing the picture brings the initials back', async ({ page }) => {
    const api = await open(page, new FakeAuthApi({ signedIn: true }), '/profile/edit');
    await page.getByTestId('avatar-input').setInputFiles(PNG);
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
    await expect(header(page).getByRole('link', { name: 'Tài khoản của tôi' }).locator('img')).toBeVisible();

    await page.getByRole('button', { name: 'Xóa ảnh' }).click();
    await page.getByRole('button', { name: 'Lưu thay đổi' }).click();

    await expect(header(page).getByRole('link', { name: 'Tài khoản của tôi' }).getByText('TD')).toBeVisible();
    expect(api.saved.at(-1)).toMatchObject({ removeAvatar: true, avatar: false });
  });
});

test.describe('US1 (011) – the dietary profile', () => {
  test('a new profile starts empty; allergens, diet, household and cuisines are saved in one request', async ({ page }) => {
    const api = await open(page, new FakeAuthApi({ signedIn: true }), '/profile/dietary');

    await page.getByRole('button', { name: 'Bắt đầu' }).click();
    await page.getByRole('button', { name: 'Đậu phộng' }).click();
    await page.getByLabel('Chế độ ăn').selectOption('VEGAN');
    await page.getByLabel('Số người ăn').fill('4');
    await page.getByRole('button', { name: 'Việt Nam' }).click();
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();

    await expect(page.getByText('Đã lưu hồ sơ ăn uống')).toBeVisible();
    expect(api.dietarySaved).toMatchObject({ allergenIds: [1], dietType: 'VEGAN', householdSize: 4, preferredCuisineIds: [10] });
  });

  test('the account page summarises it afterwards', async ({ page }) => {
    await open(page, new FakeAuthApi({ signedIn: true }), '/profile/dietary');
    await page.getByRole('button', { name: 'Bắt đầu' }).click();
    await page.getByRole('button', { name: 'Đậu phộng' }).click();
    await page.getByLabel('Số người ăn').fill('4');
    await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
    await expect(page.getByText('Đã lưu hồ sơ ăn uống')).toBeVisible();

    await page.getByRole('navigation', { name: 'Tài khoản' }).getByRole('link', { name: 'Tổng quan' }).click();

    await expect(page.getByText('Dị ứng đậu phộng · 4 người')).toBeVisible();
  });
});

test.describe('US2-6 (011) – the public profile', () => {
  test('a guest reads the name, bio and recipe count of a person, and nothing private', async ({ page }) => {
    await open(page, new FakeAuthApi(), `/profiles/${COOK.id}`);

    await expect(page.getByRole('heading', { level: 1, name: COOK.displayName })).toBeVisible();
    await expect(page.getByText(COOK.bio)).toBeVisible();
    await expect(page.getByText('12 công thức đã đăng')).toBeVisible();
    await expect(page).toHaveURL(new RegExp(`/profiles/${COOK.id}$`)); // no sign-in was asked for
    await expect(page.getByText(/@/)).toBeHidden(); // no e-mail
  });

  test('an unknown, deleted or banned person is the same "not found" page', async ({ page }) => {
    await open(page, new FakeAuthApi(), '/profiles/999');

    await expect(page.getByRole('heading', { name: 'Không tìm thấy hồ sơ' })).toBeVisible();
    await page.getByRole('link', { name: 'Về trang chủ' }).click();
    await expect(page).toHaveURL(/localhost:\d+\/$/);
  });

  test('"Xem hồ sơ công khai" on the account page opens my own public page', async ({ page }) => {
    await open(page, new FakeAuthApi({ signedIn: true }), '/profile');

    await page.getByRole('link', { name: 'Xem hồ sơ công khai' }).click();

    await expect(page).toHaveURL(/\/profiles\/1$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Thanh Du' })).toBeVisible();
    await expect(page.getByText('3 công thức đã đăng')).toBeVisible();
  });
});

test.describe('FR-024a (010) – role change notice', () => {
  // The backend sends X-WikiCook-Role-Notice and `roleNotice` in /auth/me (RoleNoticeIT), but the app has no
  // RoleChangeNotice yet (integration.md: AppLayout reloads /auth/me, shows it once, calls /auth/role-notice/ack).
  test.fixme('shows the notice once after the role changed, and acknowledges it', async () => {
    // to write when the component exists: the fake answers /auth/me with roleNotice { previousRole, newRole }
  });
});

/** 360 x 640: the sign-in and profile screens fit the width, and their main buttons can be tapped (NFR-006). */
test.describe('360 px @phone', () => {
  test.use({ viewport: { width: 360, height: 640 } });

  /** No horizontal scroll; when there is one, the message names the elements that stick out. */
  async function fits(page: Page) {
    await page.waitForLoadState('networkidle');
    const { overflow, offenders } = await page.evaluate(() => {
      const width = document.documentElement.clientWidth;
      const wide = Array.from(document.body.querySelectorAll<HTMLElement>('*'))
        .filter((el) => el.getBoundingClientRect().right > width + 0.5)
        .slice(0, 6)
        .map((el) => `${el.tagName.toLowerCase()}.${String(el.className).split(' ').join('.')} (right ${Math.round(el.getBoundingClientRect().right)})`);
      return { overflow: document.documentElement.scrollWidth - width, offenders: wide };
    });
    expect(overflow, `horizontal scroll; wider than the screen: ${offenders.join(' | ')}`).toBeLessThanOrEqual(0);
  }

  const screens: Array<{ name: string; path: string; signedIn: boolean; button?: string }> = [
    { name: 'login', path: '/login', signedIn: false, button: 'Đăng nhập' },
    { name: 'register', path: '/register', signedIn: false, button: 'Tạo tài khoản' },
    { name: 'account-status', path: '/account-status#code=ACCOUNT_BANNED&reason=Vi+ph%E1%BA%A1m', signedIn: false },
    { name: 'account', path: '/profile', signedIn: true, button: 'Đăng xuất' },
    { name: 'edit-profile', path: '/profile/edit', signedIn: true, button: 'Lưu thay đổi' },
    { name: 'dietary', path: '/profile/dietary', signedIn: true },
    { name: 'public-profile', path: `/profiles/${COOK.id}`, signedIn: false },
  ];

  for (const screen of screens) {
    test(`${screen.name} fits 360 px and its button works @phone`, async ({ page }, testInfo) => {
      await open(page, new FakeAuthApi({ signedIn: screen.signedIn }), screen.path);
      await expect(page.locator('h1, h2').first()).toBeVisible();

      await fits(page);
      if (screen.button) {
        const button = page.getByRole('button', { name: screen.button, exact: true }).first();
        await expect(button).toBeVisible();
        if (await button.isEnabled()) await button.click({ trial: true }); // nothing covers it and it can be tapped
      }
      await page.screenshot({ path: testInfo.outputPath(`${screen.name}-360.png`), fullPage: true });
      await testInfo.attach(`${screen.name}-360`, { path: testInfo.outputPath(`${screen.name}-360.png`), contentType: 'image/png' });
    });
  }

  test('the side menu of the account pages does not break at 360 px @phone', async ({ page }, testInfo) => {
    await open(page, new FakeAuthApi({ signedIn: true }), '/profile');

    const menu = page.getByRole('navigation', { name: 'Tài khoản' });
    await expect(menu).toBeVisible();
    const box = await menu.boundingBox();
    expect(box!.x).toBeGreaterThanOrEqual(0);
    expect(box!.x + box!.width).toBeLessThanOrEqual(360);
    await fits(page);
    await page.screenshot({ path: testInfo.outputPath('account-menu-360.png'), fullPage: true });
  });
});
