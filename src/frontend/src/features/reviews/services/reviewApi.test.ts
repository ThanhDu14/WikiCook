import { http, HttpResponse } from 'msw';
import { ApiError } from '@/shared/api/client';
import { server } from '@/test/server';
import { makeFile, makeList, makeReview, problem } from '../__tests__/fixtures';
import { COOKSNAP_REPORT_URL, HELPFUL_URL, REVIEW_REPORT_URL, REVIEW_URL, REVIEWS_URL, listHandler } from '../__tests__/handlers';
import { reviewApi } from './reviewApi';

describe('reviewApi (T050, contract openapi.yaml)', () => {
  it('lists reviews with page, size, sort, cooksnap filter and star filter', async () => {
    const calls: URL[] = [];
    server.use(listHandler(makeList(), calls));

    await reviewApi.list(105, { page: 2, size: 10, sort: 'MOST_HELPFUL', hasCooksnapOnly: true, rating: 4 });

    expect(calls[0].pathname).toBe('/api/v1/recipes/105/reviews');
    expect(Object.fromEntries(calls[0].searchParams)).toEqual({
      page: '2',
      size: '10',
      sort: 'MOST_HELPFUL',
      hasCooksnapOnly: 'true',
      rating: '4',
    });
  });

  it('omits the cooksnap and star filters when they are off', async () => {
    const calls: URL[] = [];
    server.use(listHandler(makeList(), calls));

    await reviewApi.list(105, { page: 0, size: 10, sort: 'NEWEST', hasCooksnapOnly: false, rating: null });

    expect(calls[0].searchParams.has('hasCooksnapOnly')).toBe(false);
    expect(calls[0].searchParams.has('rating')).toBe(false);
  });

  it('creates a review as JSON when there is no photo', async () => {
    let contentType = '';
    let body: unknown;
    server.use(
      http.post(REVIEWS_URL, async ({ request }) => {
        contentType = request.headers.get('content-type') ?? '';
        body = await request.json();
        return HttpResponse.json(makeReview({ isOwner: true }), { status: 201 });
      }),
    );

    await reviewApi.create(105, { rating: 5, comment: 'Ngon tuyệt vời luôn!', substitutionTip: null });

    expect(contentType).toContain('application/json');
    expect(body).toEqual({ rating: 5, comment: 'Ngon tuyệt vời luôn!', substitutionTip: null });
  });

  it('creates a review as multipart/form-data with cooksnapFiles', async () => {
    let form: FormData | undefined;
    server.use(
      http.post(REVIEWS_URL, async ({ request }) => {
        form = await request.formData();
        return HttpResponse.json(makeReview(), { status: 201 });
      }),
    );

    await reviewApi.create(105, {
      rating: 4,
      comment: 'Ngon tuyệt vời luôn!',
      substitutionTip: 'Thay me bằng sấu',
      cooksnapFiles: [makeFile('a.jpg', 'image/jpeg'), makeFile('b.png', 'image/png')],
    });

    expect(form?.get('rating')).toBe('4');
    expect(form?.get('comment')).toBe('Ngon tuyệt vời luôn!');
    expect(form?.get('substitutionTip')).toBe('Thay me bằng sấu');
    expect(form?.getAll('cooksnapFiles')).toHaveLength(2);
  });

  it('updates a review as JSON with removeCooksnapIds', async () => {
    let method = '';
    let body: unknown;
    server.use(
      http.put(REVIEW_URL, async ({ request, params }) => {
        method = `${request.method} ${params.reviewId}`;
        body = await request.json();
        return HttpResponse.json(makeReview());
      }),
    );

    await reviewApi.update(789, { rating: 3, comment: 'Lần hai nấu ngon hơn', substitutionTip: null, removeCooksnapIds: [1201] });

    expect(method).toBe('PUT 789');
    expect(body).toEqual({ rating: 3, comment: 'Lần hai nấu ngon hơn', substitutionTip: null, removeCooksnapIds: [1201] });
  });

  it('updates a review as multipart when new photos are added', async () => {
    let form: FormData | undefined;
    server.use(
      http.put(REVIEW_URL, async ({ request }) => {
        form = await request.formData();
        return HttpResponse.json(makeReview());
      }),
    );

    await reviewApi.update(789, {
      rating: 3,
      comment: 'Lần hai nấu ngon hơn',
      removeCooksnapIds: [1201, 1202],
      cooksnapFiles: [makeFile('c.webp', 'image/webp')],
    });

    expect(form?.getAll('removeCooksnapIds')).toEqual(['1201', '1202']);
    expect(form?.getAll('cooksnapFiles')).toHaveLength(1);
    expect(form?.has('substitutionTip')).toBe(false);
  });

  it('deletes a review (204)', async () => {
    let deleted = '';
    server.use(
      http.delete(REVIEW_URL, ({ params }) => {
        deleted = String(params.reviewId);
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await expect(reviewApi.remove(789)).resolves.toBeUndefined();
    expect(deleted).toBe('789');
  });

  it('toggles the helpful vote', async () => {
    server.use(http.post(HELPFUL_URL, () => HttpResponse.json({ reviewId: 789, helpfulCount: 15, hasVoted: true })));

    await expect(reviewApi.toggleHelpful(789)).resolves.toEqual({ reviewId: 789, helpfulCount: 15, hasVoted: true });
  });

  it('reports a review and a cooksnap', async () => {
    const bodies: unknown[] = [];
    const paths: string[] = [];
    const respond = async ({ request }: { request: Request }) => {
      paths.push(new URL(request.url).pathname);
      bodies.push(await request.json());
      return HttpResponse.json({ reportId: 1, targetType: 'REVIEW', targetId: 789, status: 'OPEN', createdAt: '' }, { status: 201 });
    };
    server.use(http.post(REVIEW_REPORT_URL, respond), http.post(COOKSNAP_REPORT_URL, respond));

    await reviewApi.reportReview(789, { reason: 'SPAM', details: null });
    await reviewApi.reportCooksnap(1201, { reason: 'OFFENSIVE', details: 'Ảnh phản cảm' });

    expect(paths).toEqual(['/api/v1/reviews/789/reports', '/api/v1/cooksnaps/1201/reports']);
    expect(bodies).toEqual([{ reason: 'SPAM', details: null }, { reason: 'OFFENSIVE', details: 'Ảnh phản cảm' }]);
  });

  it('throws ApiError with the ProblemDetail of a duplicate report', async () => {
    server.use(
      http.post(REVIEW_REPORT_URL, () => HttpResponse.json(problem(409, 'report-already-exists'), { status: 409 })),
    );

    const error = await reviewApi.reportReview(789, { reason: 'SPAM' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(409);
  });

  it('builds the media URL of a cooksnap', () => {
    expect(reviewApi.mediaUrl('cs_1.webp')).toBe('/api/v1/media/cooksnaps/cs_1.webp');
    expect(reviewApi.mediaUrl('cs_1.webp', true)).toBe('/api/v1/media/cooksnaps/cs_1.webp?thumbnail=true');
  });
});
