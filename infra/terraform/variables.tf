variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}

variable "api_cpu" {
  description = "Fargate vCPU units (512 = 0.5 vCPU)"
  type        = number
  default     = 512
}

variable "api_memory" {
  description = "Memory in MiB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "rds_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "rds_username" {
  type    = string
  default = "titlerate_admin"
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS. Leave empty for HTTP-only."
  type        = string
  default     = ""
}
