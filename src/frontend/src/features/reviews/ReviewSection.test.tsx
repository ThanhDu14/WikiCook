import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { installMemoryStorage } from '@/shared/api/memoryStorage.testutil';
import { setDevUserId } from '@/shared/api/session';
import { server } from '@/test/server';
import { emptyList, makeCooksnap, makeList, makeReview, problem } from './__tests__/fixtures';
import { HELPFUL_URL, REVIEW_REPORT_URL, REVIEW_URL, REVIEWS_URL, listHandler } from './__tests__/handlers';
import { ReviewSection } from './ReviewSection';

function LoginPage() {
  const location = useLocation();
  return <p>Trang đăng nhập {new URLSearchParams(location.search).get('redirect')}</p>;
}

function renderSection(props: Partial<Parameters<typeof ReviewSection>[0]> = {}) {
  return render(
    <MemoryRouter initialEntries={['/recipes/105']}>
      <Routes>
        <Route path="/recipes/:id" element={<ReviewSection recipeId={105} recipeTitle="Canh chua cá lóc" {...props} />} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  installMemoryStorage();
  setDevUserId(1);
});

describe('ReviewSection (integration point for the 060 recipe page)', () => {
  it('shows a loading state, then the summary, the write button and the reviews (US4-AS1)', async () => {
    server.use(listHandler(makeList({ items: [makeReview({ comment: 'Nước dùng chua thanh, cả nhà khen.' })] })));
    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải đánh giá…');
    expect(await screen.findByText('Nước dùng chua thanh, cả nhà khen.')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Đánh giá công thức' })).toBeInTheDocument();
    expect(screen.getByText('4,6')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Viết đánh giá' })).toBeInTheDocument();
  });

  it('refetches with the chosen sort, filter and page (FR-012, FR-013)', async () => {
    const calls: URL[] = [];
    server.use(listHandler(makeList({ totalPages: 2 }), calls));
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Sắp xếp' }), 'Hữu ích nhất');
    await waitFor(() => expect(calls[calls.length - 1].searchParams.get('sort')).toBe('MOST_HELPFUL'));
    await userEvent.click(screen.getByRole('button', { name: 'Chỉ có Cooksnap' }));
    await waitFor(() => expect(calls[calls.length - 1].searchParams.get('hasCooksnapOnly')).toBe('true'));
    await userEvent.click(screen.getByRole('button', { name: 'Lọc đánh giá 5 sao' }));
    await waitFor(() => expect(calls[calls.length - 1].searchParams.get('rating')).toBe('5'));
    await userEvent.click(await screen.findByRole('button', { name: '2' }));
    await waitFor(() => expect(calls[calls.length - 1].searchParams.get('page')).toBe('1'));
  });

  it('shows the empty state with the write CTA (edge case 6, W-RV06)', async () => {
    server.use(listHandler(emptyList()));
    renderSection();

    expect(await screen.findByRole('heading', { name: 'Chưa có đánh giá nào' })).toBeInTheDocument();
    expect(screen.getByText('Hãy là người đầu tiên chia sẻ trải nghiệm!')).toBeInTheDocument();
    expect(screen.getByText('Chưa có điểm')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Viết đánh giá' })).toBeInTheDocument();
    expect(screen.queryByText('Khách chưa đăng nhập')).not.toBeInTheDocument();
  });

  it('shows the error state and retries', async () => {
    let fail = true;
    server.use(
      http.get(REVIEWS_URL, () =>
        fail ? HttpResponse.json(problem(500, 'internal'), { status: 500 }) : HttpResponse.json(makeList()),
      ),
    );
    renderSection();

    expect(await screen.findByText('Không tải được đánh giá.')).toBeInTheDocument();
    expect(screen.getByText('Máy chủ đang gặp sự cố. Vui lòng thử lại sau.')).toBeInTheDocument();
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(await screen.findAllByRole('article')).toHaveLength(1);
  });

  it('creates a review, reloads the list and marks it "Mới" (US1-AS2)', async () => {
    let created = false;
    let body: unknown;
    const mine = makeReview({ id: 900, isOwner: true, comment: 'Món này rất ngon, gia vị vừa vặn!', rating: 4 });
    server.use(
      listHandler(() => makeList({ items: created ? [mine] : [makeReview()] })),
      http.post(REVIEWS_URL, async ({ request }) => {
        body = await request.json();
        created = true;
        return HttpResponse.json(mine, { status: 201 });
      }),
    );
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Viết đánh giá' }));
    const dialog = screen.getByRole('dialog', { name: 'Viết đánh giá' });
    await userEvent.click(within(dialog).getByRole('radio', { name: '4 sao' }));
    await userEvent.type(within(dialog).getByRole('textbox', { name: 'Nhận xét' }), 'Món này rất ngon, gia vị vừa vặn!');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Gửi đánh giá' }));

    expect(body).toEqual({ rating: 4, comment: 'Món này rất ngon, gia vị vừa vặn!', substitutionTip: null });
    expect(await screen.findByText('Mới')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByText('Đã gửi đánh giá. Cảm ơn bạn đã chia sẻ!')).toBeInTheDocument();
    // one review per recipe: the write button is gone once the own review is listed (US1-AS4, FR-002)
    expect(screen.queryByRole('button', { name: 'Viết đánh giá' })).not.toBeInTheDocument();
  });

  it('edits the own review with a PUT pre-filled from the card (US3-AS2, US3-AS3)', async () => {
    let body: unknown;
    const mine = makeReview({ id: 789, isOwner: true, rating: 3, comment: 'Nước dùng ngon, lần sau sẽ bớt đường.' });
    server.use(
      listHandler(makeList({ items: [mine] })),
      http.put(REVIEW_URL, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...mine, rating: 5 });
      }),
    );
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn đánh giá' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Chỉnh sửa' }));
    const dialog = screen.getByRole('dialog', { name: 'Chỉnh sửa đánh giá' });
    await userEvent.click(within(dialog).getByRole('radio', { name: '5 sao' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Lưu thay đổi' }));

    await waitFor(() =>
      expect(body).toEqual({ rating: 5, comment: 'Nước dùng ngon, lần sau sẽ bớt đường.', substitutionTip: null }),
    );
    expect(await screen.findByText('Đã lưu thay đổi.')).toBeInTheDocument();
  });

  it('deletes the own review only after the confirmation (US3-AS4, W-RV04b)', async () => {
    let deleted = false;
    const mine = makeReview({ id: 789, isOwner: true });
    server.use(
      listHandler(() => (deleted ? emptyList() : makeList({ items: [mine] }))),
      http.delete(REVIEW_URL, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn đánh giá' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Xóa' }));
    const dialog = screen.getByRole('dialog', { name: 'Xóa đánh giá' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Hủy' }));
    expect(deleted).toBe(false);

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn đánh giá' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Xóa' }));
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Xóa đánh giá' })).getByRole('button', { name: 'Xóa' }));

    expect(await screen.findByText('Đã xóa đánh giá.')).toBeInTheDocument();
    expect(deleted).toBe(true);
    expect(await screen.findByRole('heading', { name: 'Chưa có đánh giá nào' })).toBeInTheDocument();
  });

  it('reports a review, then shows the duplicate message on a second report (US5-AS2, US5-AS3)', async () => {
    let reports = 0;
    server.use(
      listHandler(makeList({ items: [makeReview({ id: 789 })] })),
      http.post(REVIEW_REPORT_URL, () => {
        reports += 1;
        return reports === 1
          ? HttpResponse.json({ reportId: 1, targetType: 'REVIEW', targetId: 789, status: 'OPEN', createdAt: '' }, { status: 201 })
          : HttpResponse.json(problem(409, 'report-already-exists'), { status: 409 });
      }),
    );
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Báo cáo' }));
    await userEvent.click(screen.getByRole('button', { name: 'Gửi báo cáo' }));
    expect(await screen.findByText('Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét.')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Báo cáo' }));
    await userEvent.click(screen.getByRole('button', { name: 'Gửi báo cáo' }));
    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent(
      'Bạn đã báo cáo nội dung này rồi. Chúng tôi đang xem xét.',
    );
  });

  it('rolls the helpful vote back when the server fails', async () => {
    server.use(
      listHandler(makeList({ items: [makeReview({ id: 789, helpfulCount: 3, hasVotedHelpful: false })] })),
      http.post(HELPFUL_URL, () => HttpResponse.json(problem(500, 'internal'), { status: 500 })),
    );
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Bình chọn hữu ích, 3 lượt hữu ích' }));

    expect(await screen.findByText('Không lưu được bình chọn. Vui lòng thử lại.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bình chọn hữu ích, 3 lượt hữu ích' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('opens the lightbox from a thumbnail and closes it with Escape (US2-AS6)', async () => {
    const snaps = [makeCooksnap(), makeCooksnap()];
    server.use(listHandler(makeList({ items: [makeReview({ cooksnaps: snaps })] })));
    renderSection();
    await screen.findAllByRole('article');

    await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh Cooksnap 2 trên 2' }));
    expect(screen.getByRole('dialog', { name: 'Ảnh Cooksnap' })).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');

    expect(screen.queryByRole('dialog', { name: 'Ảnh Cooksnap' })).not.toBeInTheDocument();
  });

  it('replaces the write button with the author notice on the own recipe (US1-AS7)', async () => {
    server.use(listHandler(makeList()));
    renderSection({ isRecipeAuthor: true });
    await screen.findAllByRole('article');

    expect(screen.queryByRole('button', { name: 'Viết đánh giá' })).not.toBeInTheDocument();
    expect(
      screen.getByText('Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.'),
    ).toBeInTheDocument();
  });

  describe('as a guest (US4-AS5, FR-017, FR-023)', () => {
    beforeEach(() => setDevUserId('guest'));

    it('can read and open photos, but writing asks to sign in and comes back to the page', async () => {
      server.use(listHandler(makeList({ items: [makeReview({ cooksnaps: [makeCooksnap()] })] })));
      renderSection();
      await screen.findAllByRole('article');

      await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh Cooksnap 1 trên 1' }));
      expect(screen.getByRole('dialog', { name: 'Ảnh Cooksnap' })).toBeInTheDocument();
      await userEvent.click(screen.getByRole('button', { name: 'Đóng' }));

      await userEvent.click(screen.getByRole('button', { name: 'Viết đánh giá' }));
      const prompt = screen.getByRole('dialog', { name: 'Mời đăng nhập' });
      expect(within(prompt).getByRole('heading', { name: 'Đánh giá “Canh chua cá lóc”?' })).toBeInTheDocument();
      expect(within(prompt).getByRole('button', { name: 'Tạo tài khoản' })).toBeInTheDocument();
      await userEvent.click(within(prompt).getByRole('button', { name: 'Đăng nhập' }));

      expect(await screen.findByText('Trang đăng nhập /recipes/105')).toBeInTheDocument();
    });

    it('asks to sign in before voting or reporting, without calling the API', async () => {
      let calls = 0;
      server.use(
        listHandler(makeList()),
        http.post(HELPFUL_URL, () => {
          calls += 1;
          return HttpResponse.json({});
        }),
      );
      renderSection();
      await screen.findAllByRole('article');

      await userEvent.click(screen.getByRole('button', { name: /Bình chọn hữu ích/ }));
      expect(screen.getByRole('heading', { name: 'Bình chọn hữu ích?' })).toBeInTheDocument();
      await userEvent.click(screen.getByRole('button', { name: 'Để sau' }));

      await userEvent.click(screen.getByRole('button', { name: 'Báo cáo' }));
      expect(screen.getByRole('heading', { name: 'Báo cáo nội dung này?' })).toBeInTheDocument();
      expect(calls).toBe(0);
    });

    it('shows the guest panel in the empty state (W-RV06)', async () => {
      server.use(listHandler(emptyList()));
      renderSection();

      expect(await screen.findByText('Khách chưa đăng nhập')).toBeInTheDocument();
      expect(screen.getByText('Đăng nhập để chấm sao và gửi ảnh Cooksnap.')).toBeInTheDocument();
      await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập để đánh giá' }));
      expect(await screen.findByText('Trang đăng nhập /recipes/105')).toBeInTheDocument();
    });
  });
});
