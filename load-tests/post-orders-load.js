import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  iterations: 50,
  thresholds: {
    http_req_failed: ['rate<0.20'],
    http_req_duration: ['p(95)<2000', 'p(99)<4000'],
  },
};

const BASE_URL = __ENV.BASE_URL;

export function setup() {
  const itemId = `post-load-item-${Date.now()}`;

  const createItemRes = http.post(`${BASE_URL}/items`, JSON.stringify({
    item_id: itemId,
    name: 'Post Load Test Item',
    available_quantity: 100,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (createItemRes.status !== 201) {
    throw new Error(`Failed to create item: ${createItemRes.status} ${createItemRes.body}`);
  }

  return { itemId };
}

export default function (data) {
  const key = `post-order-${Date.now()}-${__VU}-${__ITER}`;

  const payload = JSON.stringify({
    customer_id: `customer-${__VU}`,
    item_id: data.itemId,
    quantity: 1,
  });

  const res = http.post(`${BASE_URL}/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': key,
    },
  });

  check(res, {
    'order created': (r) => r.status === 201,
  });
}
