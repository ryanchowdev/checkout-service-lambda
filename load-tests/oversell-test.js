import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const status201 = new Counter('status_201');
const status409 = new Counter('status_409');
const status400 = new Counter('status_400');
const status500 = new Counter('status_500');
const statusOther = new Counter('status_other');

export const options = {
  scenarios: {
    oversell: {
      executor: 'shared-iterations',
      vus: 8,
      iterations: 20,
      maxDuration: '30s',
    },
  },
};

const BASE_URL = __ENV.BASE_URL;
const ITEM_ID = __ENV.ITEM_ID;

export default function () {
  const uniqueKey = `oversell-${Date.now()}-${__VU}-${__ITER}`;

  const payload = JSON.stringify({
    customer_id: `customer-${__VU}`,
    item_id: ITEM_ID,
    quantity: 1,
  });

  const res = http.post(`${BASE_URL}/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': uniqueKey,
    },
  });

  if (res.status === 201) status201.add(1);
  else if (res.status === 409) status409.add(1);
  else if (res.status === 400) status400.add(1);
  else if (res.status === 500) status500.add(1);
  else statusOther.add(1);

  check(res, {
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });
}
