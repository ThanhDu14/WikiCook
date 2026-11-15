import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { server } from '@/test/server';
import { ExcludedIngredientPicker } from '../components/ExcludedIngredientPicker';
import type { NamedItem } from '../types';

function Harness({ initial = [] as NamedItem[] }) {
  const [selected, setSelected] = useState(initial);
  return (
    <ExcludedIngredientPicker
      selected={selected}
      onAdd={(item) => setSelected((current) => [...current, item])}
      onRemove={(id) => setSelected((current) => current.filter((item) => item.id !== id))}
    />
  );
}

const catalogue: NamedItem[] = [
  { id: 1, name: 'Ngò rí' },
  { id: 2, name: 'Ngò gai' },
];

describe('ExcludedIngredientPicker (spec 011 T037)', () => {
  it('asks nothing for fewer than two characters (FR-011)', async () => {
    const user = userEvent.setup();
    let calls = 0;
    server.use(
      http.get('/api/v1/ingredients/suggestions', () => {
        calls += 1;
        return HttpResponse.json(catalogue);
      }),
    );
    render(<Harness />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'n');
    await act(() => new Promise((resolve) => setTimeout(resolve, 250)));

    expect(calls).toBe(0);
    expect(screen.queryByText('Ngò rí')).not.toBeInTheDocument();
  });

  it('suggests from the catalogue, adds a pick as a chip and clears the box (US1-3)', async () => {
    const user = userEvent.setup();
    let asked = '';
    server.use(
      http.get('/api/v1/ingredients/suggestions', ({ request }) => {
        asked = new URL(request.url).searchParams.get('q') ?? '';
        return HttpResponse.json(catalogue);
      }),
    );
    render(<Harness />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'ngo');
    await user.click(await screen.findByRole('button', { name: 'Ngò rí' }));

    expect(asked).toBe('ngo');
    expect(screen.getByRole('button', { name: 'Bỏ Ngò rí' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nguyên liệu không ăn')).toHaveValue('');
    expect(screen.getByText('1/50')).toBeInTheDocument();
  });

  it('removes a chip', async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ id: 7, name: 'Rau răm' }]} />);

    await user.click(screen.getByRole('button', { name: 'Bỏ Rau răm' }));

    expect(screen.queryByRole('button', { name: 'Bỏ Rau răm' })).not.toBeInTheDocument();
    expect(screen.getByText('0/50')).toBeInTheDocument();
  });

  it('does not offer an ingredient that is already picked', async () => {
    const user = userEvent.setup();
    server.use(http.get('/api/v1/ingredients/suggestions', () => HttpResponse.json(catalogue)));
    render(<Harness initial={[catalogue[0]]} />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'ngo');

    expect(await screen.findByRole('button', { name: 'Ngò gai' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ngò rí' })).not.toBeInTheDocument();
  });

  it('only what the catalogue returns can be added: free text is not an ingredient', async () => {
    const user = userEvent.setup();
    server.use(http.get('/api/v1/ingredients/suggestions', () => HttpResponse.json([])));
    render(<Harness />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'khongcotronglist{Enter}');

    expect(await screen.findByText('Không tìm thấy nguyên liệu phù hợp')).toBeInTheDocument();
    expect(screen.getByText('0/50')).toBeInTheDocument();
  });

  it('waits 150 ms after the last key and cancels the older request (NFR-001)', async () => {
    const user = userEvent.setup();
    const asked: string[] = [];
    server.use(
      http.get('/api/v1/ingredients/suggestions', ({ request }) => {
        asked.push(new URL(request.url).searchParams.get('q') ?? '');
        return HttpResponse.json(catalogue);
      }),
    );
    render(<Harness />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'ngoi'); // four quick keys

    await screen.findByRole('button', { name: 'Ngò rí' });
    expect(asked).toEqual(['ngoi']); // not one request per key
  });

  it('stops at 50 ingredients with an explanation (Edge: 51st ingredient)', async () => {
    const fifty = Array.from({ length: 50 }, (_, index) => ({ id: index + 1, name: `Nguyên liệu ${index + 1}` }));
    render(<Harness initial={fifty} />);

    expect(screen.getByLabelText('Nguyên liệu không ăn')).toBeDisabled();
    expect(screen.getByRole('alert')).toHaveTextContent('Đã đủ 50 nguyên liệu');
    expect(screen.getByText('50/50')).toBeInTheDocument();
  });

  it('says when the suggestions cannot be loaded', async () => {
    const user = userEvent.setup();
    server.use(http.get('/api/v1/ingredients/suggestions', () => HttpResponse.error()));
    render(<Harness />);

    await user.type(screen.getByLabelText('Nguyên liệu không ăn'), 'ngo');

    expect(await screen.findByRole('alert')).toHaveTextContent('Không kết nối được máy chủ');
  });

  it('works with the keyboard: Tab reaches the suggestion and Enter adds it; Escape clears the box (NFR-006)', async () => {
    const user = userEvent.setup();
    server.use(http.get('/api/v1/ingredients/suggestions', () => HttpResponse.json(catalogue)));
    render(<Harness />);
    const input = screen.getByLabelText('Nguyên liệu không ăn');

    await user.type(input, 'ngo');
    await screen.findByRole('button', { name: 'Ngò rí' });
    await user.tab();
    expect(screen.getByRole('button', { name: 'Ngò rí' })).toHaveFocus();
    await user.keyboard('{Enter}');

    expect(screen.getByRole('button', { name: 'Bỏ Ngò rí' })).toBeInTheDocument();

    await user.type(input, 'abc{Escape}');
    expect(input).toHaveValue('');
  });
});
