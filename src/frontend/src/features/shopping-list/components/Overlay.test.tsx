import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { Overlay } from './Overlay';

/** Re-renders with a new inline `onClose` on every keystroke, like RecipeIngredientsPanel does. */
function Host() {
  const [open, setOpen] = useState(true);
  const [text, setText] = useState('');
  return (
    <>
      <button type="button">Opener</button>
      {open && (
        <Overlay title="Hộp thoại" onClose={() => setOpen(false)}>
          <input aria-label="Ô nhập" value={text} onChange={(e) => setText(e.target.value)} />
          <button type="button">Lưu</button>
        </Overlay>
      )}
    </>
  );
}

describe('Overlay', () => {
  it('keeps focus inside when the parent re-renders with a new onClose', async () => {
    const user = userEvent.setup();
    render(<Host />);

    const input = screen.getByLabelText('Ô nhập');
    await user.click(input);
    await user.type(input, 'abc');

    expect(input).toHaveFocus();
    expect(input).toHaveValue('abc');
  });

  it('cycles Tab and Shift+Tab inside the dialog', async () => {
    const user = userEvent.setup();
    render(<Host />);

    await user.click(screen.getByRole('button', { name: 'Lưu' }));
    await user.tab();
    expect(screen.getByRole('button', { name: 'Đóng' })).toHaveFocus();

    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Lưu' })).toHaveFocus();
  });

  it('closes on Escape with the latest onClose', async () => {
    const user = userEvent.setup();
    render(<Host />);

    await user.type(screen.getByLabelText('Ô nhập'), 'x');
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
