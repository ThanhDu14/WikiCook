import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AvatarPicker } from '../components/AvatarPicker';
import { AVATAR_MAX_BYTES, checkAvatar } from '../lib/avatarRules';

describe('avatar rules (spec 011 FR-003)', () => {
  it('accepts JPEG, PNG and WebP up to 5 MB', () => {
    for (const type of ['image/jpeg', 'image/png', 'image/webp']) {
      expect(checkAvatar({ type, size: AVATAR_MAX_BYTES })).toBeNull();
    }
  });

  it('refuses another type and a file over 5 MB', () => {
    expect(checkAvatar({ type: 'image/gif', size: 10 })).toBe('type');
    expect(checkAvatar({ type: 'application/pdf', size: 10 })).toBe('type');
    expect(checkAvatar({ type: 'image/png', size: AVATAR_MAX_BYTES + 1 })).toBe('size');
  });
});

describe('AvatarPicker (spec 011 T060)', () => {
  it('previews a chosen picture as a data: URL, not blob: (CSP), and reports it', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AvatarPicker displayName="Thanh Du" onChange={onChange} />);
    const file = new File(['x'], 'me.png', { type: 'image/png' });

    await user.upload(screen.getByTestId('avatar-input'), file);

    const image = await screen.findByAltText('Ảnh đại diện');
    expect(image.getAttribute('src')).toMatch(/^data:image\/png/);
    expect(onChange).toHaveBeenCalledWith({ file, remove: false });
  });

  it('refuses a wrong type or a big file with the rule, and reports nothing (SC-003)', async () => {
    const user = userEvent.setup({ applyAccept: false });
    const onChange = vi.fn();
    render(<AvatarPicker displayName="Thanh Du" onChange={onChange} />);

    await user.upload(screen.getByTestId('avatar-input'), new File(['x'], 'doc.pdf', { type: 'application/pdf' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Ảnh phải là JPEG, PNG hoặc WebP, tối đa 5 MB.');

    const big = new File(['x'], 'big.png', { type: 'image/png' });
    Object.defineProperty(big, 'size', { value: AVATAR_MAX_BYTES + 1 });
    await user.upload(screen.getByTestId('avatar-input'), big);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
    expect(screen.queryByAltText('Ảnh đại diện')).not.toBeInTheDocument();
  });

  it('shows initials when there is no picture, and "Xóa ảnh" only when there is one', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const { rerender } = render(<AvatarPicker displayName="Thanh Du" onChange={onChange} />);
    expect(screen.getByText('TD')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xóa ảnh' })).not.toBeInTheDocument();

    rerender(<AvatarPicker displayName="Thanh Du" currentUrl="/api/v1/media/avatars/a.jpg" onChange={onChange} />);
    await user.click(screen.getByRole('button', { name: 'Xóa ảnh' }));

    expect(onChange).toHaveBeenCalledWith({ file: null, remove: true });
    await waitFor(() => expect(screen.getByText('TD')).toBeInTheDocument());
  });

  it('shows the server error for the picture', () => {
    render(<AvatarPicker displayName="Thanh Du" onChange={vi.fn()} serverError="Ảnh vượt quá 5 MB." />);

    expect(screen.getByRole('alert')).toHaveTextContent('Ảnh vượt quá 5 MB.');
  });
});
