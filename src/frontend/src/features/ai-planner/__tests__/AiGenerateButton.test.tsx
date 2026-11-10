import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { AiGenerateButton } from '../components/AiGenerateButton';
import { clearAllAiDrafts, useAiDraft } from '../hooks/useAiDraft';
import { draftResponse, options, WEEK } from './fixtures';

afterEach(() => clearAllAiDrafts());

function DraftProbe() {
  const { draftItems } = useAiDraft(WEEK);
  return <p data-testid="probe">{draftItems.length}</p>;
}

describe('AiGenerateButton (T037)', () => {
  it('is enabled when the week has empty slots and opens the dialog for its week (FR-001)', async () => {
    server.use(http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options())));
    render(<AiGenerateButton weekStart={WEEK} />);

    const button = await screen.findByRole('button', { name: 'Tạo bằng AI' });
    expect(button).toBeEnabled();
    await userEvent.click(button);

    expect(await screen.findByRole('dialog', { name: 'Tạo thực đơn bằng AI' })).toBeInTheDocument();
  });

  it('is disabled with a hint when the week has no empty slot (full week)', async () => {
    server.use(http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options({ emptySlots: [] }))));
    render(<AiGenerateButton weekStart={WEEK} />);

    expect(await screen.findByRole('button', { name: 'Tạo bằng AI' })).toBeDisabled();
    expect(screen.getByText('Tuần này không còn ô trống')).toBeInTheDocument();
  });

  it('is not shown for a past week', async () => {
    let asked = false;
    server.use(
      http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => {
        asked = true;
        return HttpResponse.json(options({ selectableDays: [], emptySlots: [] }));
      }),
    );
    const { container } = render(<AiGenerateButton weekStart={WEEK} />);

    await vi.waitFor(() => expect(asked).toBe(true));
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(container).toBeEmptyDOMElement();
  });

  it('is not shown when the options cannot be read (NFR-006)', async () => {
    server.use(http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json({ status: 500 }, { status: 500 })));
    const { container } = render(<AiGenerateButton weekStart={WEEK} />);

    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(container).toBeEmptyDOMElement();
  });

  it('puts the generated draft on the week and closes the dialog', async () => {
    server.use(
      http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options())),
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () => HttpResponse.json(draftResponse())),
    );
    render(
      <>
        <AiGenerateButton weekStart={WEEK} />
        <DraftProbe />
      </>,
    );
    await userEvent.click(await screen.findByRole('button', { name: 'Tạo bằng AI' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Tạo thực đơn' }));

    await vi.waitFor(() => expect(screen.getByTestId('probe')).toHaveTextContent('3'));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
