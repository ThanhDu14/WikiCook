import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DietaryNotice, DietaryAllHiddenState } from './DietaryNotice';
import type { DietaryNotice as DietaryNoticeType } from '../types';

describe('DietaryNotice (T037, US3, SR-02, SR-05, SR-06)', () => {
  it('renders nothing when profileAvailable is false (guest user)', () => {
    const notice: DietaryNoticeType = {
      profileAvailable: false,
      applied: false,
      hiddenCount: 0,
      showingHidden: false,
    };
    const { container } = render(
      <DietaryNotice notice={notice} onShowHidden={vi.fn()} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when profileAvailable is true but applied is false and not showingHidden', () => {
    const notice: DietaryNoticeType = {
      profileAvailable: true,
      applied: false,
      hiddenCount: 0,
      showingHidden: false,
    };
    const { container } = render(
      <DietaryNotice notice={notice} onShowHidden={vi.fn()} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders SR-02 info banner when recipes are hidden by profile', async () => {
    const handleShowHidden = vi.fn();
    const notice: DietaryNoticeType = {
      profileAvailable: true,
      applied: true,
      hiddenCount: 6,
      showingHidden: false,
    };

    render(<DietaryNotice notice={notice} onShowHidden={handleShowHidden} />);

    expect(screen.getByText(/Đã ẩn 6 món theo hồ sơ ăn uống/i)).toBeInTheDocument();

    const showButton = screen.getByRole('button', { name: /Hiện cho lần tìm này/i });
    expect(showButton).toBeInTheDocument();

    await userEvent.click(showButton);
    expect(handleShowHidden).toHaveBeenCalledTimes(1);
  });

  it('renders SR-06 warning banner when showing hidden recipes with conflicts', () => {
    const notice: DietaryNoticeType = {
      profileAvailable: true,
      applied: false,
      hiddenCount: 0,
      showingHidden: true,
    };

    render(
      <DietaryNotice
        notice={notice}
        conflictingCount={4}
        onShowHidden={vi.fn()}
      />,
    );

    expect(screen.getByText(/Đang hiện 4 món không hợp hồ sơ/i)).toBeInTheDocument();
    expect(
      screen.getByText(/Chỉ áp dụng cho lần tìm này\. Tìm mới sẽ ẩn lại\./i),
    ).toBeInTheDocument();
  });

  describe('DietaryAllHiddenState (SR-05)', () => {
    it('renders all-hidden empty state with title, description, and action buttons', async () => {
      const handleShowHidden = vi.fn();
      const handleEditProfile = vi.fn();

      render(
        <DietaryAllHiddenState
          hiddenCount={4}
          keyword="goi ga"
          onShowHidden={handleShowHidden}
          onEditProfile={handleEditProfile}
        />,
      );

      expect(
        screen.getByRole('heading', { level: 2, name: /Cả 4 món đều bị ẩn/i }),
      ).toBeInTheDocument();
      expect(
        screen.getByText(/Mọi kết quả cho “goi ga” chứa những thứ có trong hồ sơ ăn uống của bạn\./i),
      ).toBeInTheDocument();

      const showBtn = screen.getByRole('button', { name: /Hiện cho lần tìm này/i });
      expect(showBtn).toBeInTheDocument();
      await userEvent.click(showBtn);
      expect(handleShowHidden).toHaveBeenCalledTimes(1);

      const editBtn = screen.getByRole('button', { name: /Sửa hồ sơ ăn uống/i });
      expect(editBtn).toBeInTheDocument();
      await userEvent.click(editBtn);
      expect(handleEditProfile).toHaveBeenCalledTimes(1);
    });
  });
});
