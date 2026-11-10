import { render, screen } from '@testing-library/react';
import { DraftBar } from '../components/DraftBar';
import { strings } from '../strings.vi';
import { draftResponse } from './fixtures';

describe('DraftBar (T036)', () => {
  it('always shows the disclaimer word for word on the draft (US1-7, FR-028)', () => {
    render(<DraftBar draft={draftResponse()} />);

    expect(screen.getByText('Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng')).toBeInTheDocument();
    expect(strings.disclaimer).toBe('Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng');
  });

  it('shows the AI mode, the number of new dishes and of meals left to choose', () => {
    render(<DraftBar draft={draftResponse()} />);

    expect(screen.getByText('Bản nháp AI · chưa lưu')).toBeInTheDocument();
    expect(screen.getByText('AI')).toBeInTheDocument();
    expect(screen.getByText(/2 món mới/)).toHaveTextContent('2 món mới · 1 ô cần bạn chọn');
  });

  it('says a backup draft was made without AI', () => {
    render(<DraftBar draft={draftResponse({ mode: 'AUTO', notice: 'AUTO_NO_AI' })} />);

    expect(screen.getByText('Tạo tự động, không dùng AI')).toBeInTheDocument();
    expect(screen.getByText('Auto')).toBeInTheDocument();
  });

  it('says the AI is out of turns for today', () => {
    render(<DraftBar draft={draftResponse({ mode: 'AUTO', notice: 'AI_DAILY_CAP' })} />);

    expect(screen.getByText('AI đã hết lượt hôm nay')).toBeInTheDocument();
  });
});
