import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { aiPlannerApi, AiPlannerApiError } from '../api/aiPlannerApi';
import type { GenerateRequest } from '../types';
import { draftResponse, options, WEEK } from './fixtures';

const request: GenerateRequest = {
  requestId: '0b8c1a8e-6f6e-4a51-9d55-1f0a5a1c2b3d',
  days: ['MONDAY'],
  meals: ['BREAKFAST'],
  constraints: options().defaults,
};

describe('aiPlannerApi (T014)', () => {
  it('reads the options of a week', async () => {
    server.use(http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options())));

    await expect(aiPlannerApi.getOptions(WEEK)).resolves.toEqual(options());
  });

  it('posts the generation with its body and returns the draft', async () => {
    let body: unknown;
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async ({ request: req }) => {
        body = await req.json();
        return HttpResponse.json(draftResponse());
      }),
    );

    await expect(aiPlannerApi.generate(WEEK, request)).resolves.toEqual(draftResponse());
    expect(body).toEqual(request);
  });

  it('passes the AbortSignal: aborting rejects with an aborted error', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 200));
        return HttpResponse.json(draftResponse());
      }),
    );
    const controller = new AbortController();
    const call = aiPlannerApi.generate(WEEK, request, controller.signal);
    controller.abort();

    await expect(call).rejects.toMatchObject({ code: 'ABORTED' });
  });

  it('turns a ProblemDetail into its code', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () =>
        HttpResponse.json({ status: 429, code: 'RATE_LIMITED', title: 'Too many', nextAvailableAt: '2026-11-16T10:00:00Z' }, { status: 429 }),
      ),
    );

    const error = await aiPlannerApi.generate(WEEK, request).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(AiPlannerApiError);
    expect(error).toMatchObject({ status: 429, code: 'RATE_LIMITED' });
  });

  it('reports a network failure as NETWORK', async () => {
    server.use(http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.error()));

    await expect(aiPlannerApi.getOptions(WEEK)).rejects.toMatchObject({ status: 0, code: 'NETWORK' });
  });

  it('cancels with DELETE /ai-planner/generations/{requestId}', async () => {
    let called = false;
    server.use(
      http.delete('/api/v1/ai-planner/generations/:id', ({ params }) => {
        called = params.id === request.requestId;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await aiPlannerApi.cancel(request.requestId);
    expect(called).toBe(true);
  });
});
