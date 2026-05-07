import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 5 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL;
const ORDER_ID = __ENV.ORDER_ID;

export default function () {
  const res = http.get(`${BASE_URL}/orders/${ORDER_ID}`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
