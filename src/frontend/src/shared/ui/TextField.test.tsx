import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PasswordField } from './PasswordField';
import { TextField } from './TextField';

describe('TextField (spec 010 T027)', () => {
  it('has a visible label tied to the input and the wk- classes of the screens', () => {
    const { container } = render(<TextField label="Email" defaultValue="du@example.com" />);

    const input = screen.getByLabelText('Email');
    expect(input).toHaveValue('du@example.com');
    expect(input).toHaveClass('wk-input');
    expect(container.querySelector('.wk-field')).toBeInTheDocument();
    expect(container.querySelector('label.wk-label')).toHaveTextContent('Email');
  });

  it('links the error to the input with aria-describedby and marks the field', () => {
    const { container } = render(<TextField label="Email" error="Email không hợp lệ" helper="Nhập email" counter="3/50" />);

    const input = screen.getByLabelText('Email');
    const error = screen.getByText('Email không hợp lệ');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input.getAttribute('aria-describedby')).toContain(error.id);
    expect(error).toHaveClass('wk-error');
    expect(container.querySelector('.wk-field')).toHaveClass('is-error');
    expect(screen.getByText('3/50')).toBeInTheDocument();
  });

  it('does not mark a field without an error', () => {
    render(<TextField label="Tên" />);

    expect(screen.getByLabelText('Tên')).not.toHaveAttribute('aria-invalid');
  });
});

describe('PasswordField (spec 010 T027)', () => {
  it('hides the password until the show button is used, with the keyboard too', async () => {
    const user = userEvent.setup();
    render(<PasswordField label="Mật khẩu" defaultValue="bep12345" />);

    const input = screen.getByLabelText('Mật khẩu');
    expect(input).toHaveAttribute('type', 'password');

    await user.tab(); // input
    await user.tab(); // the show button
    expect(screen.getByRole('button', { name: 'Hiện mật khẩu' })).toHaveFocus();
    await user.keyboard('{Enter}');

    expect(input).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: 'Ẩn mật khẩu' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('shows its error next to the field', () => {
    render(<PasswordField label="Mật khẩu" error="Mật khẩu chưa đạt yêu cầu" />);

    expect(screen.getByLabelText('Mật khẩu')).toHaveAccessibleDescription('Mật khẩu chưa đạt yêu cầu');
  });
});
