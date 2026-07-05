output "alb_dns_name" {
  description = "Public endpoint for the API"
  value       = aws_lb.main.dns_name
}

output "ecr_repo_url" {
  description = "ECR repository URL (add to GitHub secret ECR_REPO)"
  value       = aws_ecr_repository.api.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name (add to GitHub secret ECS_CLUSTER)"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name (add to GitHub secret ECS_SERVICE)"
  value       = aws_ecs_service.api.name
}

output "ecs_task_family" {
  description = "ECS task definition family (add to GitHub secret ECS_TASK_FAMILY)"
  value       = "${local.name_prefix}-api"
}

output "github_actions_role_arn" {
  description = "IAM role ARN for GitHub Actions OIDC (add to GitHub secret AWS_ROLE_ARN)"
  value       = aws_iam_role.github_actions.arn
}

output "rds_endpoint" {
  description = "RDS host (informational; actual URL is in Secrets Manager)"
  value       = aws_db_instance.main.address
}
