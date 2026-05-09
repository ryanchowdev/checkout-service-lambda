# IaaS vs. Serverless E-Commerce Checkout Service

Serverless checkout/order service deployed on AWS EC2 and AWS Lambda.

We compare an AWS Lambda-based serverless implementation of a checkout system against an EC2-based IaaS architecture. The service supports item creation, order creation, inventory decrement, idempotent order retries, order lookup, rate limiting, scaling tests, and failure demonstrations.

The EC2-based implementation is located here: [https://github.com/fanzhang-code/orderServiceFan](https://github.com/fanzhang-code/orderServiceFan "https://github.com/fanzhang-code/orderServiceFan").

The Lambda-based implementation is located here: [https://github.com/ryanchowdev/checkout-service-lambda](https://github.com/ryanchowdev/checkout-service-lambda "https://github.com/ryanchowdev/checkout-service-lambda").

## High-Level Deployment Description

This project compares two AWS cloud deployment models for the same checkout/order service:

1. **Serverless deployment** using AWS WAF, API Gateway, Lambda, DynamoDB, IAM, CloudWatch, and AWS SAM.
2. **IaaS deployment** using Application Load Balancer, EC2 instances, Auto Scaling Group, DynamoDB, IAM, CloudWatch, and CloudFormation/EC2 deployment configuration.

The Lambda version in this repository is deployed as a serverless AWS stack using AWS SAM and CloudFormation. The public entry point is Amazon API Gateway. Requests pass through AWS WAF for rate limiting, then API Gateway forwards requests to a Java 17 AWS Lambda function. The Lambda function contains the checkout logic and stores data in DynamoDB tables. CloudWatch is used to collect logs and metrics for testing, scaling analysis, and failure evaluation.

The EC2 version uses an Application Load Balancer as the public entry point. The load balancer forwards traffic to EC2 instances running the checkout API. The EC2 instances are managed by an Auto Scaling Group, which launches additional instances when load increases. The EC2 application also uses DynamoDB for persistent storage and CloudWatch for monitoring.

## Architecture Overview

### Lambda / Serverless Request Flow

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

### EC2 / IaaS Request Flow

```text
Client / Load Test Tool
        |
        v
Application Load Balancer
        |
        v
Auto Scaling Group
        |
        v
EC2 Instances
        |
        v
Amazon DynamoDB
```

Both implementations use separate DynamoDB tables for:

```text
checkout-items
checkout-orders
checkout-idempotency
```

This keeps the comparison focused mainly on the compute and scaling model rather than using different storage backends.

## Cloud Services Used

| Service | Used By | Purpose |
|---|---|---|
| AWS WAF | Lambda deployment | IP-based rate limiting before requests reach API Gateway and Lambda. |
| Amazon API Gateway | Lambda deployment | Public HTTP entry layer for the serverless service. |
| AWS Lambda | Lambda deployment | Serverless compute layer running the Java checkout service. |
| Application Load Balancer | EC2 deployment | Public entry layer that distributes traffic across EC2 instances. |
| Amazon EC2 | EC2 deployment | IaaS compute layer running the checkout service. |
| EC2 Auto Scaling Group | EC2 deployment | Dynamically adds or removes EC2 instances based on load. |
| Amazon DynamoDB | Both deployments | Persistent storage for items, orders, and idempotency records. |
| Amazon CloudWatch | Both deployments | Metrics and logs for observability and evaluation. |
| AWS IAM | Both deployments | Execution roles and permissions for compute resources to access AWS services. |
| AWS CloudFormation / SAM | Lambda deployment | Infrastructure deployment and management for the serverless stack. |

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks if the service is running. |
| `POST` | `/items` | Creates an item with available inventory. |
| `GET` | `/items/{itemId}` | Gets item details and current inventory. |
| `POST` | `/orders` | Creates an order and atomically decrements inventory. |
| `GET` | `/orders/{orderId}` | Gets an order by order ID. |

## Scaling Demonstrations

Scaling was tested using k6 load tests against the deployed APIs.

### Lambda Scaling Demonstration

For the Lambda deployment, the baseline test `load-tests/get-order-baseline.js` was first run against `GET /orders/{orderId}` with low traffic. Then the controlled load test `load-tests/get-order-controlled-load.js` increased traffic up to 10 virtual users. CloudWatch Lambda metrics were collected during both runs.

### EC2 Scaling Demonstration

For the EC2 deployment, scaling was tested using k6 load tests against the Application Load Balancer endpoint. The Auto Scaling Group was configured with a target CPU utilization of 50%.

To run a controlled load test, use `k6 run load-test-get-order.js`.
It should verify that the EC2 deployment could handle moderate traffic with stable latency and zero failures.

To run a heavy load test for scaling behaviour, use `k6 run .\load-test-heavy-get-order.js`.

## Failure Demonstrations

Three Lambda-side failure scenarios were tested.

### 1. Lambda Concurrency Throttling

An aggressive high-load k6 test `load-tests/get-order-spike.js` sent over 127,000 requests to `GET /orders/{orderId}`. The load exceeded the available Lambda concurrency.

Result: Excess requests were throttled by Lambda before the handler executed.

### 2. Oversell Prevention

For the oversell test `load-tests/oversell-test.js`, an item was created with inventory `3`, then 20 concurrent order attempts were sent using k6.

Result: DynamoDB transactions and conditional updates prevented overselling.

### 3. WAF Rate Limiting

AWS WAF was configured with a low rate limit, then repeated `GET /orders/{orderId}` requests were sent.

```bash
for i in {1..120}; do  
	echo -n "Request $i: "  
	curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/orders/$ORDER_ID"  
done
```

Result: WAF blocked excessive traffic before it reached Lambda.

### EC2 Failure Demonstrations

The failure test mainly focus on Oversell Prevention. In this test, firstly we create an item with limit inventory, and then let 8 virtual users send concurrent POST requests to create orders. The requests were sent to the DNS address of ALB, each of which used a unique idempotency key. And every request asked for 1 quantity of that item. The goal is to verify that the EC2 based Order Service could prevent the stock from going below zero, especially when multiple users attempted to purchase the same item.

Run `k6 run .\over-selling-check.js`.

Successful orders were expected to return `HTTP 201`, while requests after the item was sold out would get failure responses with error message `Not enough stock or item does not exist.` 

## Running the Lambda Project

Build the project:

```bash
mvn clean package
```

Deploy with SAM:

```bash
sam build
sam deploy --guided
```

For future deployments:

```bash
mvn clean package
sam build
sam deploy
```

After deployment, set the API URL:

```bash
export BASE_URL="https://<api-id>.execute-api.<region>.amazonaws.com/Prod"
```

Test the health endpoint:

```bash
curl -i "$BASE_URL/health"
```

## Running the EC2 Project

Build the project:

```bash
docker build -t order-service .
docker compose up --build
```

Test the health endpoint:

```bash
curl -i "$BASE_URL/health"
```

## Load Testing

Load test scripts are stored in:

```text
load-tests/
```

Example controlled load test:

```bash
k6 run \
  -e BASE_URL="$BASE_URL" \
  -e ORDER_ID="$ORDER_ID" \
  --summary-export load-tests/results/get-order-controlled-load.json \
  load-tests/get-order-controlled-load.js
```

You may want to temporarily raise the WAF rate limit inside `template.yaml` before running load tests:

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

For the WAF rate-limiting demo, set the limit back to a low value such as:

```yaml
Limit: 10
```

Then redeploy:

```bash
mvn clean package
sam build
sam deploy
```

## Division of Work

| Team Member  | Responsibilities                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Ryan Chow    | Implemented the AWS Lambda/serverless version of the checkout service. This included the Java Lambda handler, API Gateway integration, DynamoDB table design, item endpoints, order endpoints, idempotent order creation, inventory decrement logic, DynamoDB transaction handling, AWS WAF rate limiting, SAM/CloudFormation deployment, Lambda-side load testing, CloudWatch metric collection, failure demonstrations, cost analysis, and security/trust-boundary documentation for the serverless architecture. |
| Fan Zhang    | Implemented the EC2/IaaS version of the checkout service. This included the EC2 application deployment, Application Load Balancer setup, Auto Scaling Group configuration, EC2-side scaling tests, EC2-side failure demonstrations, CloudWatch metric collection, infrastructure/security configuration for the IaaS deployment, and comparison data for the EC2 architecture.                                                                                                                                      |
| Both Members | Collaborated on defining the shared API behavior, comparing EC2 and Lambda trade-offs, preparing the final report, preparing the presentation, collecting evaluation evidence, and analyzing architectural differences in scalability, failure handling, cost, security, and operational overhead.                                                                                                                                                                                                                  |

## Cleanup

To remove deployed Lambda-side AWS resources:

```bash
sam delete --stack-name checkout-service-lambda --region <region>
```

Example:

```bash
sam delete --stack-name checkout-service-lambda --region us-west-1
```

To remove deployed EC2-based AWS resources:

```bash
# Terminate EC2 instances
aws ec2 terminate-instances --instance-ids <instance-id>

# Delete Auto Scaling Group
aws autoscaling delete-auto-scaling-group \
  --auto-scaling-group-name <asg-name> \
  --force-delete

# Delete Launch Template
aws ec2 delete-launch-template \
  --launch-template-name <launch-template-name>

# Delete Application Load Balancer
aws elbv2 delete-load-balancer \
  --load-balancer-arn <alb-arn>

# Delete Target Group
aws elbv2 delete-target-group \
  --target-group-arn <target-group-arn>
```

