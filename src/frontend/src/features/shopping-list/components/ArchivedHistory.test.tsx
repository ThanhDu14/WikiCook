import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { makeGroup, makeItem, makeList } from '../test/fixtures';
import { ArchivedHistory } from './ArchivedHistory';

const BASE = '/api/v1/shopping-lists';

const archived = [
  makeList({
    id: 7,
    title: 'Đi chợ 20/10',
    status: 'ARCHIVED',
    totalItems: 18,
    checkedItems: 16,
    aisleGroups: [makeGroup([makeItem({ displayName: 'Thịt bò', displayQuantity: 1, displayUnit: 'kg' })])],
  }),
  makeList({ id: 6, title: 'Tiệc sinh nhật', status: 'ARCHIVED', totalItems: 31, checkedItems: 29 }),
];

function setup() {
  const onRestored = vi.fn();
  server.use(http.get(`${BASE}/history`, () => HttpResponse.json(archived)));
  render(<ArchivedHistory onRestored={onRestored} />);
  return { onRestored, user: userEvent.setup() };
}

describe('ArchivedHistory (SL-07)', () => {
  it('lists archived trips with item and purchased counts', async () => {
    setup();

    const row = (await screen.findByText('Đi chợ 20/10')).closest('tr')!;
    expect(within(row).getByText('18 món · đã mua 16')).toBeInTheDocument();
    expect(screen.getByText('Tiệc sinh nhật')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Khôi phục/ })).toHaveLength(2);
  });

  it('shows an empty state when nothing is archived', async () => {
    server.use(http.get(`${BASE}/history`, () => HttpResponse.json([])));
    render(<ArchivedHistory onRestored={vi.fn()} />);

    expect(await screen.findByText('Chưa có danh sách nào được lưu trữ')).toBeInTheDocument();
  });

  it('shows a retryable error when history cannot load', async () => {
    server.use(http.get(`${BASE}/history`, () => HttpResponse.error()));
    render(<ArchivedHistory onRestored={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
  });

  it('opens an archived list read-only with "Xem"', async () => {
    const { user } = setup();

    const row = (await screen.findByText('Đi chợ 20/10')).closest('tr')!;
    await user.click(within(row).getByRole('button', { name: 'Xem' }));

    const drawer = screen.getByRole('dialog', { name: 'Đi chợ 20/10' });
    expect(within(drawer).getByText('Thịt bò')).toBeInTheDocument();
    expect(within(drawer).getByText('1 kg')).toBeInTheDocument();
  });

  it('asks for confirmation before restoring', async () => {
    const { user } = setup();

    const row = (await screen.findByText('Đi chợ 20/10')).closest('tr')!;
    await user.click(within(row).getByRole('button', { name: 'Khôi phục' }));

    const dialog = screen.getByRole('dialog', { name: 'Khôi phục “Đi chợ 20/10”?' });
    expect(
      within(dialog).getByText(
        'Danh sách hiện tại sẽ được lưu trữ. 18 món được đưa về chưa mua, giữ nguyên số lượng và quầy.',
      ),
    ).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: 'Hủy' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('restores the list and reports back', async () => {
    let restored = false;
    server.use(
      http.post(`${BASE}/7/restore`, () => {
        restored = true;
        return HttpResponse.json(makeList({ id: 7, title: 'Đi chợ 20/10' }));
      }),
    );
    const { onRestored, user } = setup();

    const row = (await screen.findByText('Đi chợ 20/10')).closest('tr')!;
    await user.click(within(row).getByRole('button', { name: 'Khôi phục' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Khôi phục' }));

    await waitFor(() => expect(onRestored).toHaveBeenCalled());
    expect(restored).toBe(true);
  });

  it('keeps the dialog open and shows an error when restore fails', async () => {
    server.use(http.post(`${BASE}/7/restore`, () => HttpResponse.json({ status: 404 }, { status: 404 })));
    const { onRestored, user } = setup();

    const row = (await screen.findByText('Đi chợ 20/10')).closest('tr')!;
    await user.click(within(row).getByRole('button', { name: 'Khôi phục' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Khôi phục' }));

    expect(await screen.findByText('Không khôi phục được danh sách. Vui lòng thử lại.')).toBeInTheDocument();
    expect(onRestored).not.toHaveBeenCalled();
  });
});
