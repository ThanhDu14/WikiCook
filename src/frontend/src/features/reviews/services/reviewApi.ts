/**
 * API client of feature 070 (T050) over the shared HTTP client, which adds the session/auth headers and turns
 * RFC 7807 errors into `ApiError`. Source: specs/070-review-cooksnap/contracts/openapi.yaml.
 */

import { API_BASE, api, fetchJson } from '@/shared/api/client';
import type {
  CreateReportPayload,
  CreateReviewPayload,
  HelpfulVoteResponse,
  ReportResponse,
  Review,
  ReviewListQuery,
  ReviewListResponse,
  UpdateReviewPayload,
} from '../types';

function listParams(query: ReviewListQuery): string {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.size), sort: query.sort });
  if (query.hasCooksnapOnly) params.set('hasCooksnapOnly', 'true');
  if (query.rating !== null) params.set('rating', String(query.rating));
  return params.toString();
}

/** multipart/form-data body: scalar fields, then repeated `removeCooksnapIds` and `cooksnapFiles`. */
function toForm(payload: UpdateReviewPayload): FormData {
  const form = new FormData();
  form.append('rating', String(payload.rating));
  form.append('comment', payload.comment);
  if (payload.substitutionTip) form.append('substitutionTip', payload.substitutionTip);
  payload.removeCooksnapIds?.forEach((id) => form.append('removeCooksnapIds', String(id)));
  payload.cooksnapFiles?.forEach((file) => form.append('cooksnapFiles', file, file.name));
  return form;
}

/** JSON body (`CreateReviewRequest` / `UpdateReviewRequest`) without the files. */
function toJson({ cooksnapFiles: _files, ...rest }: UpdateReviewPayload) {
  void _files;
  return rest;
}

const hasFiles = (payload: CreateReviewPayload) => (payload.cooksnapFiles?.length ?? 0) > 0;

export const reviewApi = {
  /** GET /recipes/{recipeId}/reviews (public). */
  list: (recipeId: number, query: ReviewListQuery) =>
    api.get<ReviewListResponse>(`/recipes/${recipeId}/reviews?${listParams(query)}`),

  /** POST /recipes/{recipeId}/reviews: multipart with Cooksnap files, JSON otherwise. */
  create: (recipeId: number, payload: CreateReviewPayload) => {
    const path = `/recipes/${recipeId}/reviews`;
    return hasFiles(payload) ? api.upload<Review>(path, toForm(payload)) : api.post<Review>(path, toJson(payload));
  },

  /** PUT /reviews/{reviewId}: multipart when new Cooksnap files are added, JSON otherwise. */
  update: (reviewId: number, payload: UpdateReviewPayload) => {
    const path = `/reviews/${reviewId}`;
    return hasFiles(payload)
      ? fetchJson<Review>(API_BASE + path, { method: 'PUT', body: toForm(payload) })
      : api.put<Review>(path, toJson(payload));
  },

  /** DELETE /reviews/{reviewId} (owner hard delete, 204). */
  remove: (reviewId: number) => api.delete(`/reviews/${reviewId}`),

  /** POST /reviews/{reviewId}/helpful (toggle). */
  toggleHelpful: (reviewId: number) => api.post<HelpfulVoteResponse>(`/reviews/${reviewId}/helpful`, {}),

  /** POST /reviews/{reviewId}/reports. */
  reportReview: (reviewId: number, payload: CreateReportPayload) =>
    api.post<ReportResponse>(`/reviews/${reviewId}/reports`, payload),

  /** POST /cooksnaps/{cooksnapId}/reports. */
  reportCooksnap: (cooksnapId: number, payload: CreateReportPayload) =>
    api.post<ReportResponse>(`/cooksnaps/${cooksnapId}/reports`, payload),

  /** GET /media/cooksnaps/{storageKey}: used as an <img> src (the DTO already carries `url`/`thumbnailUrl`). */
  mediaUrl: (storageKey: string, thumbnail = false) =>
    `${API_BASE}/media/cooksnaps/${encodeURIComponent(storageKey)}${thumbnail ? '?thumbnail=true' : ''}`,
};
