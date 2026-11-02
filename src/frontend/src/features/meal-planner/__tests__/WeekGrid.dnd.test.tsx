import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { WeekGrid } from '../components/WeekGrid';
import { item, week } from './fixtures';

const plan = week({
  'MONDAY/LUNCH': [item({ id: 5, recipeName: 'Phở bò' }), item({ id: 7, recipeName: 'Rau muống' })],
  'TUESDAY/LUNCH': item({ id: 6, recipeName: 'Cá kho' }),
});

function renderGrid() {
  const onMove = vi.fn();
  render(
    <MemoryRouter>
      <WeekGrid week={plan} enableDrag onAdd={vi.fn()} onOpenItem={vi.fn()} onMove={onMove} onClearDay={vi.fn()} />
    </MemoryRouter>,
  );
  return { onMove };
}

async function press(element: Element, key: string, code = key) {
  await act(async () => {
    fireEvent.keyDown(element, { key, code });
    // the keyboard sensor starts listening on the next task
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
}

const handle = (name: string) => screen.getByRole('button', { name: `Kéo để chuyển: ${name}` });
const live = () => document.querySelector('[aria-live]')?.textContent ?? '';

describe('WeekGrid drag and drop (T060, T115)', () => {
  it('moves one dish to an empty meal with Space, arrows and Space (US3-1, NFR-003)', async () => {
    const { onMove } = renderGrid();
    const drag = handle('Phở bò');
    drag.focus();

    await press(drag, ' ', 'Space');
    expect(live()).toContain('Đã nhấc “Phở bò”');
    await press(drag, 'ArrowDown');
    await press(drag, ' ', 'Space');

    expect(onMove).toHaveBeenCalledWith(5, { day: 'MONDAY', meal: 'DINNER' });
    expect(live()).toContain('Đã thả “Phở bò” vào Thứ Hai, bữa tối');
  });

  it('drops on a meal that has dishes: the dish joins its end (US3-2)', async () => {
    const { onMove } = renderGrid();
    const drag = handle('Rau muống');
    drag.focus();

    await press(drag, ' ', 'Space');
    await press(drag, 'ArrowRight');
    await press(drag, ' ', 'Space');

    expect(onMove).toHaveBeenCalledWith(7, { day: 'TUESDAY', meal: 'LUNCH' });
  });

  it('does nothing when cancelled with Esc or dropped on its own meal (edge case)', async () => {
    const { onMove } = renderGrid();
    const drag = handle('Phở bò');
    drag.focus();

    await press(drag, ' ', 'Space');
    await press(drag, 'ArrowDown');
    await press(drag, 'Escape');
    expect(live()).toContain('Đã hủy chuyển “Phở bò”');

    await press(drag, ' ', 'Space');
    await press(drag, ' ', 'Space');
    expect(onMove).not.toHaveBeenCalled();
  });
});
