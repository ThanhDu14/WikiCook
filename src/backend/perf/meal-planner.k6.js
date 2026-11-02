// T103 – NFR-001 của spec 030: 200 người dùng ảo; GET tuần p95 < 2 s, thao tác ghi p95 < 1 s.
//
// Chạy (backend bật dev stub để mỗi VU là một user riêng qua header X-Dev-User-Id, chưa có login của 010):
//   SPRING_PROFILES_ACTIVE=local WIKICOOK_DEV_STUBS_ENABLED=true mvn -pl wikicook-app spring-boot:run
//   k6 run -e BASE_URL=http://localhost:8080 -e RECIPE_IDS=1,2,3,4,5 src/backend/perf/meal-planner.k6.js
//
// RECIPE_IDS: id các công thức PUBLISHED, không phải món ăn vặt. USER_BASE: id user đầu tiên (mặc định 900000),
// VU thứ n dùng USER_BASE + n. WEEK: thứ Hai của tuần thử (mặc định 2026-11-02).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WEEK = __ENV.WEEK || '2026-11-02';
const USER_BASE = Number(__ENV.USER_BASE || 900000);
const RECIPES = (__ENV.RECIPE_IDS || '').split(',').filter(Boolean).map(Number);
const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const MEALS = ['BREAKFAST', 'LUNCH', 'DINNER'];

export const options = {
  scenarios: {
    planners: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '15s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{op:getWeek}': ['p(95)<2000'],
    'http_req_duration{op:addItem}': ['p(95)<1000'],
    'http_req_duration{op:patchServings}': ['p(95)<1000'],
    'http_req_duration{op:moveItem}': ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  if (RECIPES.length === 0) {
    throw new Error('Set RECIPE_IDS to published recipe ids, e.g. -e RECIPE_IDS=1,2,3');
  }
}

const pick = (list) => list[Math.floor(Math.random() * list.length)];

function params(op) {
  return {
    headers: { 'Content-Type': 'application/json', 'X-Dev-User-Id': String(USER_BASE + __VU) },
    tags: { op },
    // 409 MEAL_FULL / SLOT_CHANGED là kết quả hợp lệ khi bữa đã đủ 4 món, không tính là lỗi.
    responseCallback: http.expectedStatuses(200, 201, 204, 409),
  };
}

export default function () {
  const week = `${BASE_URL}/api/v1/meal-plans/${WEEK}`;

  const res = http.get(week, params('getWeek'));
  check(res, { 'week 200': (r) => r.status === 200 });

  const day = pick(DAYS);
  const meal = pick(MEALS);
  const added = http.post(`${week}/meals/${day}/${meal}/items`,
    JSON.stringify({ recipeId: pick(RECIPES) }), params('addItem'));
  check(added, { 'add 201 or 409': (r) => r.status === 201 || r.status === 409 });
  if (added.status !== 201) {
    sleep(1);
    return;
  }
  let item = added.json('item');

  const patched = http.patch(`${week}/items/${item.id}/servings`,
    JSON.stringify({ servings: 1 + Math.floor(Math.random() * 6), version: item.version }),
    params('patchServings'));
  check(patched, { 'servings 200': (r) => r.status === 200 });
  if (patched.status === 200) {
    item = patched.json('item');
  }

  const moved = http.post(`${week}/items/${item.id}/move`,
    JSON.stringify({ day: pick(DAYS), meal: pick(MEALS), version: item.version }), params('moveItem'));
  check(moved, { 'move 200 or 409': (r) => r.status === 200 || r.status === 409 });

  // Xóa món vừa thêm để các bữa không đầy dần; version đọc lại từ tuần.
  const current = http.get(week, params('getWeek'));
  for (const d of current.json('days') || []) {
    for (const s of d.slots) {
      const mine = s.items.find((i) => i.id === item.id);
      if (mine) {
        http.del(`${week}/items/${mine.id}?version=${mine.version}`, null, params('deleteItem'));
      }
    }
  }
  sleep(1);
}
