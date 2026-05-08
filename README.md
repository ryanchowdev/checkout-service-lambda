
# checkout-service-lambda

Serverless checkout/order service deployed on AWS Lambda.

This repository contains the serverless implementation of a checkout system used for comparing an AWS Lambda architecture against an EC2-based architecture. The service supports item creation, order creation, inventory decrement, idempotent order retries, order lookup, rate limiting, and failure testing.

## High-Level Deployment Description

The application is deployed as a serverless AWS stack using AWS SAM and CloudFormation.

The public entry point is Amazon API Gateway. Requests pass through AWS WAF for rate limiting, then API Gateway forwards requests to a Java 17 AWS Lambda function. The Lambda function contains the checkout logic and stores data in DynamoDB tables. CloudWatch is used to collect logs and metrics for testing, scaling analysis, and failure evaluation.

High-level request flow:

```text
Client / Load Test Tool
        |
        v
AWS WAF
        |
        v
Amazon API Gateway
        |
        v
AWS Lambda
        |
        v
Amazon DynamoDB
```

## Cloud Services Used

|Service|Purpose|
|---|---|
|AWS WAF|IP-based rate limiting before requests reach the API.|
|Amazon API Gateway|Public HTTP entry layer for the service.|
|AWS Lambda|Serverless compute layer running the Java checkout service.|
|Amazon DynamoDB|Persistent storage for items, orders, and idempotency records.|
|Amazon CloudWatch|Metrics and logs for observability and evaluation.|
|AWS IAM|Execution role and permissions for Lambda to access DynamoDB.|
|AWS CloudFormation / SAM|Infrastructure deployment and management.|

## API Endpoints

|Method|Endpoint|Description|
|---|---|---|
|`GET`|`/health`|Checks if the service is running.|
|`POST`|`/items`|Creates an item with available inventory.|
|`GET`|`/items/{itemId}`|Gets item details and current inventory.|
|`POST`|`/orders`|Creates an order and atomically decrements inventory.|
|`GET`|`/orders/{orderId}`|Gets an order by order ID.|

## Scaling Demonstration

Scaling was tested using k6 load tests against the deployed API.

A baseline test was first run against `GET /orders/{orderId}` with low traffic. Then a controlled load test increased traffic up to 10 virtual users. CloudWatch Lambda metrics were collected during both runs.

Summary of scaling results:

|Metric|Baseline|Controlled Load|
|---|---|---|
|Requests|430|3,216|
|Throughput|3.57 req/s|17.85 req/s|
|Failure rate|0%|0%|
|P95 latency|123.83 ms|86.01 ms|
|P99 latency|281.98 ms|132.28 ms|
|Max Lambda concurrency|4|9|
|Lambda errors|0|0|
|Lambda throttles|0|0|

This demonstrated that Lambda increased concurrent executions as load increased while maintaining zero errors and zero throttles during the controlled test.

## Failure Demonstrations

Three failure scenarios were tested.

### 1. Lambda Concurrency Throttling

An aggressive high-load k6 test sent over 127,000 requests to `GET /orders/{orderId}`. The load exceeded the available Lambda concurrency.

Observed behavior:

```
Total requests: 127,978
Successful responses: 14,571
API Gateway 5XX responses: 113,407
Lambda errors: 0
Lambda throttles: high
Max Lambda concurrency: 10
```

This showed that excess requests were throttled by Lambda before the handler executed.

### 2. Oversell Prevention

An item was created with inventory `3`, then 20 concurrent order attempts were sent using k6.

Observed behavior:

```
HTTP 201 Created: 3
HTTP 409 Conflict: 17
HTTP 500 errors: 0
Final inventory: 0
```

This demonstrated that DynamoDB transactions and conditional updates prevented overselling.

### 3. WAF Rate Limiting

AWS WAF was configured with a low rate limit, then repeated `GET /orders/{orderId}` requests were sent.

Observed behavior:

```
Total curl requests: 120
Allowed requests: 82
Blocked requests: 39
HTTP 429 responses: 39
Lambda errors: 0
```

This showed that WAF blocked excessive traffic before it reached Lambda.

## Running the Project

Build the project:

```
mvn clean package
```

Deploy with SAM:

```
sam build
sam deploy --guided
```

For future deployments:

```
mvn clean package
sam build
sam deploy
```

After deployment, set the API URL:

```
export BASE_URL="https://<api-id>.execute-api.<region>.amazonaws.com/Prod"
```

Test the health endpoint:

```
curl -i "$BASE_URL/health"
```

## Load Testing

Load test scripts are stored in:

```
load-tests/
```

Example controlled load test:

```
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e ORDER_ID="$ORDER_ID" \
  --summary-export load-tests/results/get-order-controlled-load.json \
  load-tests/get-order-controlled-load.js
```

You may want to temporarily raise the rate limit inside `template.yaml`:

```yaml
Statement:
	RateBasedStatement:
		# Control rate limit here
		# Set to 10 to demonstrate rate limiting behavior
		# Set to 9999999 to not get in the way of load tests
		Limit: 9999999
		AggregateKeyType: IP
		EvaluationWindowSec: 60
```
## Cleanup

To remove deployed AWS resources:

```
sam delete --stack-name checkout-service-lambda --region <region>
```

Example:

```
sam delete --stack-name checkout-service-lambda --region us-west-1
```
