import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { AiGenerateDialog } from '../components/AiGenerateDialog';
import { options, WEEK } from './fixtures';

// 15:20 in Vietnam (UTC+7)
const NEXT = '2026-11-16T08:20:00Z';

describe('AiGenerateDialog at the hourly limit (T070, US5-2, W-AI07)', () => {
  it('no generation left: no "Tạo", says when the next one opens', async () => {
    const onClose = vi.fn();
    render(<AiGenerateDialog weekStart={WEEK} options={options({ remaining: 0, nextAvailableAt: NEXT })} onDraft={vi.fn()} onClose={onClose} />);

    expect(screen.getByRole('heading', { name: 'Bạn đã dùng hết 5 lượt tạo' })).toBeInTheDocument();
    expect(screen.getByText('Lượt tiếp theo mở lại lúc 15:20. Trong lúc chờ, bạn vẫn tự thêm món vào thực đơn được.')).toBeInTheDocument();
    expect(screen.getByText('“Tạo lại” cũng tính là một lượt. Bản nháp dự phòng khi AI lỗi không tính.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tạo thực đơn' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Về thực đơn' }));
    expect(onClose).toHaveBeenCalled();
  });

  it('a 429 on "Tạo" turns the dialog into the same state, with the time from the answer', async () => {
    let calls = 0;
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () => {
        calls++;
        return HttpResponse.json({ status: 429, code: 'RATE_LIMITED', nextAvailableAt: NEXT }, { status: 429, headers: { 'Retry-After': '600' } });
      }),
    );
    const onDraft = vi.fn();
    render(<AiGenerateDialog weekStart={WEEK} options={options({ remaining: 1 })} onDraft={onDraft} onClose={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByRole('heading', { name: 'Bạn đã dùng hết 5 lượt tạo' })).toBeInTheDocument();
    expect(screen.getByText(/mở lại lúc 15:20\./)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tạo thực đơn' })).not.toBeInTheDocument();
    expect(onDraft).not.toHaveBeenCalled();
    expect(calls).toBe(1);
  });

  it('a 429 without a time still shows the limit', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () => HttpResponse.json({ status: 429, code: 'RATE_LIMITED' }, { status: 429 })),
    );
    render(<AiGenerateDialog weekStart={WEEK} options={options()} onDraft={vi.fn()} onClose={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByRole('heading', { name: 'Bạn đã dùng hết 5 lượt tạo' })).toBeInTheDocument();
    expect(screen.getByText('Lượt tiếp theo mở lại trong vòng 60 phút. Trong lúc chờ, bạn vẫn tự thêm món vào thực đơn được.')).toBeInTheDocument();
  });

  it('with generations left the form and "Tạo" stay', () => {
    render(<AiGenerateDialog weekStart={WEEK} options={options({ remaining: 1 })} onDraft={vi.fn()} onClose={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Tạo thực đơn' })).toBeEnabled();
    expect(screen.getByText('Còn 1/5 lượt tạo trong 60 phút.')).toBeInTheDocument();
  });
});
