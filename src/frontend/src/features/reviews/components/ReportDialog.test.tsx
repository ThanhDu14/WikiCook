import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/shared/api/client';
import { makeCooksnap, makeReview, problem } from '../__tests__/fixtures';
import type { CreateReportPayload, ReportTarget } from '../types';
import { ReportDialog } from './ReportDialog';

const review = makeReview({ rating: 1, author: { id: 9, displayName: 'Khách lạ', isDeleted: false } });

function renderDialog(target: ReportTarget = { type: 'REVIEW', review }) {
  const onSubmit = vi.fn<(payload: CreateReportPayload) => Promise<void>>().mockResolvedValue(undefined);
  const onClose = vi.fn();
  render(<ReportDialog target={target} onSubmit={onSubmit} onClose={onClose} />);
  return { onSubmit, onClose };
}

describe('ReportDialog (T079, US5, W-RV05)', () => {
  it('lists the five contract reasons as radios with the review subtitle (US5-AS1)', () => {
    renderDialog();

    expect(screen.getByRole('dialog', { name: 'Báo cáo đánh giá' })).toBeInTheDocument();
    expect(screen.getByText('Đánh giá của “Khách lạ” · 1 sao')).toBeInTheDocument();
    const radios = screen.getAllByRole('radio');
    expect(radios.map((r) => r.closest('label')?.textContent)).toEqual([
      'Ngôn ngữ thô tục',
      'Spam / quảng cáo',
      'Nội dung không phù hợp',
      'Vi phạm bản quyền',
      'Khác',
    ]);
    expect(screen.getByRole('radio', { name: 'Spam / quảng cáo' })).toBeChecked();
    expect(screen.getByRole('textbox', { name: 'Mô tả thêm' })).toHaveAttribute('maxLength', '1000');
    expect(screen.getByText('0/1000')).toBeInTheDocument();
  });

  it('submits the reason and the trimmed details (US5-AS2)', async () => {
    const { onSubmit } = renderDialog();

    await userEvent.click(screen.getByRole('radio', { name: 'Ngôn ngữ thô tục' }));
    await userEvent.type(screen.getByRole('textbox', { name: 'Mô tả thêm' }), ' Bình luận xúc phạm ');
    await userEvent.click(screen.getByRole('button', { name: 'Gửi báo cáo' }));

    expect(onSubmit).toHaveBeenCalledWith({ reason: 'OFFENSIVE', details: 'Bình luận xúc phạm' });
  });

  it('sends null details when left empty', async () => {
    const { onSubmit } = renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Gửi báo cáo' }));

    expect(onSubmit).toHaveBeenCalledWith({ reason: 'SPAM', details: null });
  });

  it('shows the duplicate report message and stays open (US5-AS3, FR-016)', async () => {
    const { onSubmit, onClose } = renderDialog();
    onSubmit.mockRejectedValueOnce(new ApiError(problem(409, 'report-already-exists')));

    await userEvent.click(screen.getByRole('button', { name: 'Gửi báo cáo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Bạn đã báo cáo nội dung này rồi. Chúng tôi đang xem xét.');
    expect(onClose).not.toHaveBeenCalled();
  });

  it('titles a photo report "Báo cáo ảnh Cooksnap"', () => {
    renderDialog({ type: 'COOKSNAP', review, cooksnap: makeCooksnap() });

    expect(screen.getByRole('dialog', { name: 'Báo cáo ảnh Cooksnap' })).toBeInTheDocument();
  });

  it('closes with "Hủy"', async () => {
    const { onClose } = renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));

    expect(onClose).toHaveBeenCalled();
  });
});
