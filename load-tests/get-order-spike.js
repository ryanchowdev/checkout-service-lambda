import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const status200 = new Counter('status_200');
const status400 = new Counter('status_400');
const status403 = new Counter('status_403');
const status404 = new Counter('status_404');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status502 = new Counter('status_502');
const status503 = new Counter('status_503');
const status504 = new Counter('status_504');
const statusOther = new Counter('status_other');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 10 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<4000'],
  },
};

const BASE_URL = __ENV.BASE_URL;
const ORDER_ID = __ENV.ORDER_ID;

export default function () {
  const res = http.get(`${BASE_URL}/orders/${ORDER_ID}`);

  if (res.status === 200) status200.add(1);
  else if (res.status === 400) status400.add(1);
  else if (res.status === 403) status403.add(1);
  else if (res.status === 404) status404.add(1);
  else if (res.status === 429) status429.add(1);
  else if (res.status === 500) status500.add(1);
  else if (res.status === 502) status502.add(1);
  else if (res.status === 503) status503.add(1);
  else if (res.status === 504) status504.add(1);
  else statusOther.add(1);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}