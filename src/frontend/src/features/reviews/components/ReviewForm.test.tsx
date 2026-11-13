import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/shared/api/client';
import { makeCooksnap, makeFile, makeReview, problem } from '../__tests__/fixtures';
import type { UpdateReviewPayload } from '../types';
import { ReviewForm } from './ReviewForm';

const user = () => userEvent.setup({ applyAccept: false });
const MB = 1024 * 1024;

beforeAll(() => {
  if (typeof URL.createObjectURL !== 'function') {
    Object.assign(URL, { createObjectURL: () => 'blob:preview', revokeObjectURL: () => {} });
  }
});

function renderForm(props: Partial<Parameters<typeof ReviewForm>[0]> = {}) {
  const onSubmit = vi.fn<(payload: UpdateReviewPayload) => Promise<void>>().mockResolvedValue(undefined);
  const onCancel = vi.fn();
  render(<ReviewForm recipeTitle="Canh chua cá lóc" onSubmit={onSubmit} onCancel={onCancel} {...props} />);
  return { onSubmit, onCancel };
}

const comment = () => screen.getByRole('textbox', { name: 'Nhận xét' });
const tip = () => screen.getByRole('textbox', { name: 'Mẹo nấu / thay thế nguyên liệu' });
const fileInput = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

describe('ReviewForm (T058, T061, US1, US2, W-RV02)', () => {
  it('opens as the "Viết đánh giá" dialog with no star selected (US1-AS1, FR-003)', () => {
    renderForm();

    expect(screen.getByRole('dialog', { name: 'Viết đánh giá' })).toBeInTheDocument();
    expect(screen.getByText('Canh chua cá lóc')).toBeInTheDocument();
    const stars = within(screen.getByRole('radiogroup', { name: 'Bạn chấm mấy sao?' })).getAllByRole('radio');
    expect(stars).toHaveLength(5);
    stars.forEach((star) => expect(star).toHaveAttribute('aria-checked', 'false'));
    expect(tip()).toHaveAttribute('placeholder', 'Không bắt buộc');
    expect(screen.getByText('0/2000')).toBeInTheDocument();
    expect(screen.getByText('0/500')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Gửi đánh giá' })).toBeInTheDocument();
  });

  it('shows the missing star and empty comment errors without submitting (US1-AS6)', async () => {
    const { onSubmit } = renderForm();

    await user().click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(screen.getByText('Vui lòng chọn số sao')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng viết nhận xét')).toBeInTheDocument();
    expect(comment()).toHaveAttribute('aria-invalid', 'true');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('rejects a comment under 10 characters with a live counter (US1-AS5, FR-004)', async () => {
    const { onSubmit } = renderForm();
    const u = user();

    await u.click(screen.getByRole('radio', { name: '4 sao' }));
    await u.type(comment(), 'Ngon lắm');
    expect(screen.getByText('8/2000')).toBeInTheDocument();
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(screen.getByText('Nhận xét phải có ít nhất 10 ký tự')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('limits the comment to 2000 and the tip to 500 characters (FR-004, FR-005)', () => {
    renderForm();

    expect(comment()).toHaveAttribute('maxLength', '2000');
    expect(tip()).toHaveAttribute('maxLength', '500');
  });

  it('selects the rating with the arrow keys (NFR-005)', async () => {
    renderForm();
    const u = user();

    await u.click(screen.getByRole('radio', { name: '2 sao' }));
    await u.keyboard('{ArrowRight}{ArrowRight}');

    expect(screen.getByRole('radio', { name: '4 sao' })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: '4 sao' })).toHaveFocus();
  });

  it('submits rating, trimmed comment, tip and the Cooksnap files (US1-AS2, US2)', async () => {
    const { onSubmit } = renderForm();
    const u = user();
    const photo = makeFile('pho.jpg', 'image/jpeg', 2 * MB);

    await u.click(screen.getByRole('radio', { name: '5 sao' }));
    await u.type(comment(), '  Món này rất ngon, gia vị vừa vặn!  ');
    await u.type(tip(), 'Thay me bằng sấu');
    await u.upload(fileInput(), photo);
    expect(screen.getByRole('img', { name: 'Ảnh Cooksnap 1' })).toBeInTheDocument();
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(onSubmit).toHaveBeenCalledWith({
      rating: 5,
      comment: 'Món này rất ngon, gia vị vừa vặn!',
      substitutionTip: 'Thay me bằng sấu',
      cooksnapFiles: [photo],
    });
  });

  it('sends a null tip and no files when they are left empty', async () => {
    const { onSubmit } = renderForm();
    const u = user();

    await u.click(screen.getByRole('radio', { name: '3 sao' }));
    await u.type(comment(), 'Công thức rõ ràng, dễ làm theo.');
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(onSubmit).toHaveBeenCalledWith({ rating: 3, comment: 'Công thức rõ ràng, dễ làm theo.', substitutionTip: null });
  });

  it('rejects a photo over 5 MB and an unsupported format (US2-AS3, US2-AS4)', async () => {
    renderForm();
    const u = user();

    await u.upload(fileInput(), makeFile('big.jpg', 'image/jpeg', 6 * MB));
    expect(screen.getByText('Ảnh vượt quá 5 MB. Vui lòng chọn ảnh nhỏ hơn.')).toBeInTheDocument();

    await u.upload(fileInput(), makeFile('anim.gif', 'image/gif'));
    expect(screen.getByText('Định dạng không hỗ trợ. Vui lòng chọn JPEG, PNG hoặc WebP.')).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: /Ảnh Cooksnap/ })).not.toBeInTheDocument();
  });

  it('keeps at most 3 photos and disables "Thêm ảnh" with a tooltip at 3 (US2-AS5, FR-006)', async () => {
    renderForm();
    const u = user();
    const files = ['a.jpg', 'b.png', 'c.webp', 'd.jpg'].map((n, i) =>
      makeFile(n, ['image/jpeg', 'image/png', 'image/webp', 'image/jpeg'][i]),
    );

    await u.upload(fileInput(), files);

    expect(screen.getAllByRole('img', { name: /Ảnh Cooksnap \d/ })).toHaveLength(3);
    expect(screen.getByText('Tối đa 3 ảnh cho mỗi đánh giá.', { selector: '.wk-error' })).toBeInTheDocument();
    const add = screen.getByRole('button', { name: 'Thêm ảnh' });
    expect(add).toBeDisabled();
    expect(add).toHaveAttribute('title', 'Tối đa 3 ảnh cho mỗi đánh giá.');

    await u.click(screen.getByRole('button', { name: 'Xóa ảnh 2' }));
    expect(screen.getAllByRole('img', { name: /Ảnh Cooksnap \d/ })).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Thêm ảnh' })).toBeEnabled();
  });

  it('pre-fills the edit form and sends removeCooksnapIds (US3-AS2, US3-AS3)', async () => {
    const snaps = [makeCooksnap({ id: 1201 }), makeCooksnap({ id: 1202 })];
    const review = makeReview({
      id: 789,
      isOwner: true,
      rating: 3,
      comment: 'Nước dùng ngon, lần sau sẽ bớt đường.',
      substitutionTip: 'Bớt đường',
      cooksnaps: snaps,
    });
    const { onSubmit } = renderForm({ review });
    const u = user();

    expect(screen.getByRole('dialog', { name: 'Chỉnh sửa đánh giá' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: '3 sao' })).toHaveAttribute('aria-checked', 'true');
    expect(comment()).toHaveValue('Nước dùng ngon, lần sau sẽ bớt đường.');
    expect(tip()).toHaveValue('Bớt đường');
    expect(screen.getAllByRole('img', { name: /Ảnh Cooksnap \d/ })).toHaveLength(2);

    await u.click(screen.getByRole('radio', { name: '5 sao' }));
    await u.click(screen.getByRole('button', { name: 'Xóa ảnh 1' }));
    await u.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    expect(onSubmit).toHaveBeenCalledWith({
      rating: 5,
      comment: 'Nước dùng ngon, lần sau sẽ bớt đường.',
      substitutionTip: 'Bớt đường',
      removeCooksnapIds: [1201],
    });
  });

  it('counts kept photos against the limit when editing', async () => {
    const review = makeReview({ isOwner: true, cooksnaps: [makeCooksnap(), makeCooksnap(), makeCooksnap()] });
    renderForm({ review });

    expect(screen.getByRole('button', { name: 'Thêm ảnh' })).toBeDisabled();
  });

  it('shows the API error of a rejected submit and keeps the text (409, 429)', async () => {
    const { onSubmit } = renderForm();
    onSubmit.mockRejectedValueOnce(new ApiError(problem(409, 'review-already-exists')));
    const u = user();

    await u.click(screen.getByRole('radio', { name: '4 sao' }));
    await u.type(comment(), 'Món này rất ngon, gia vị vừa vặn!');
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có.',
    );
    expect(comment()).toHaveValue('Món này rất ngon, gia vị vừa vặn!');
  });

  it('shows the field errors of a 400 next to the fields', async () => {
    const { onSubmit } = renderForm();
    onSubmit.mockRejectedValueOnce(
      new ApiError({ ...problem(400, 'validation-error'), invalidParams: [{ name: 'comment', reason: 'Nhận xét không hợp lệ' }] }),
    );
    const u = user();

    await u.click(screen.getByRole('radio', { name: '4 sao' }));
    await u.type(comment(), 'Món này rất ngon, gia vị vừa vặn!');
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(await screen.findByText('Nhận xét không hợp lệ')).toBeInTheDocument();
  });

  it('offers "Thử lại" after a network failure while uploading photos (edge case 2)', async () => {
    const { onSubmit } = renderForm();
    onSubmit.mockRejectedValueOnce(new TypeError('Failed to fetch'));
    const u = user();

    await u.click(screen.getByRole('radio', { name: '4 sao' }));
    await u.type(comment(), 'Món này rất ngon, gia vị vừa vặn!');
    await u.upload(fileInput(), makeFile('a.jpg', 'image/jpeg'));
    await u.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Tải ảnh thất bại. Kiểm tra kết nối mạng và thử lại.');
    await u.click(screen.getByRole('button', { name: 'Thử lại' }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(2));
    expect(onSubmit.mock.calls[1][0]).toMatchObject({ rating: 4, comment: 'Món này rất ngon, gia vị vừa vặn!' });
  });

  it('cancels with "Hủy"', async () => {
    const { onCancel } = renderForm();

    await user().click(screen.getByRole('button', { name: 'Hủy' }));

    expect(onCancel).toHaveBeenCalled();
  });
});
