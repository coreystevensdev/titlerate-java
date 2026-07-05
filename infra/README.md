# AWS ECS Deployment

Single Fargate service running the Spring Boot JAR on port 8080, backed by RDS PostgreSQL 17 on a private subnet. All traffic enters via an Application Load Balancer.

## Prerequisites

- AWS CLI v2: `brew install awscli`
- Terraform >= 1.9: `brew install terraform`

## First Deploy

### 1. Enable GitHub OIDC (one time per account)

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

### 2. Provision infrastructure

```bash
cd infra/terraform
terraform init
terraform plan -out=tfplan
terraform apply tfplan
terraform output
```

### 3. Set GitHub Actions secrets

| Secret | Source |
|---|---|
| `AWS_ROLE_ARN` | `terraform output github_actions_role_arn` |
| `ECR_REPO` | `terraform output ecr_repo_url` |
| `ECS_CLUSTER` | `terraform output ecs_cluster_name` |
| `ECS_SERVICE` | `terraform output ecs_service_name` |
| `ECS_TASK_FAMILY` | `terraform output ecs_task_family` |
| `ALB_URL` | `http://$(terraform output -raw alb_dns_name)` |

### 4. Push to main

CI runs tests, builds the JAR, builds the Docker image, pushes to ECR, and deploys to ECS automatically.

## Rollback

ECS deployment circuit breaker auto-rolls back on health-check failure. Manual rollback:

```bash
# List task definition revisions
aws ecs list-task-definitions \
  --family-prefix titlerate-java-prod-api \
  --sort DESC

# Roll back to a prior revision
aws ecs update-service \
  --cluster titlerate-java-prod-cluster \
  --service titlerate-java-prod-service \
  --task-definition titlerate-java-prod-api:<PREVIOUS_REVISION>
```

## Cost (us-east-1)

| Resource | Monthly (approx.) |
|---|---|
| ECS Fargate (512 CPU / 1024 MB) | ~$7 |
| RDS db.t3.micro | ~$15 (free tier yr 1) |
| ALB | ~$16 |
| NAT Gateway | ~$4 |
| **Total** | **~$42/mo** |
