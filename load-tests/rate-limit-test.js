import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 8,
  duration: '30s',
};

const BASE_URL = __ENV.BASE_URL;
const ORDER_ID = __ENV.ORDER_ID;

export default function () {
  const res = http.get(`${BASE_URL}/orders/${ORDER_ID}`);

  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
  });
}
