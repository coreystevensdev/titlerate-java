# AWS ECS Deployment, a path not taken

**This is not how titlerate-java is deployed.** The live service is a container
started by `docker run` over SSM on a shared `t3.small`, bound to
`127.0.0.1:8081` behind Caddy, with no load balancer and no Fargate. See the
root `README.md` and `.github/workflows/deploy.yml` for the real path.

What follows describes an ECS architecture that was written and then not used.
Only two things in `terraform/` are consumed by the live deployment: the ECR
repository and the GitHub OIDC role. Everything else, the VPC, the two subnets,
the Application Load Balancer, RDS and the Fargate service, exists here and
runs nowhere.

Applying it is not free. On a paid account it bills a NAT gateway, an ALB, an
RDS instance and Fargate capacity for an architecture the system does not have,
and `main.tf` leaves the S3 backend commented out, so there is no shared state
to stop a second apply creating a duplicate set.

The description below is kept because the Terraform is real and reviewable, not
because it is a runbook to follow.

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

### 4. Push to main

CI runs tests, builds the JAR, builds the Docker image and pushes to ECR. It then deploys with `docker run` over SSM to the shared instance. Nothing in the committed workflows touches ECS.

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
