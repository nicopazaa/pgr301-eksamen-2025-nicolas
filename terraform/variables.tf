variable "aws_region" {
  description = "AWS region to deploy resources in"
  type        = string
  default     = "eu-west-1"
}

variable "sam_artifacts_bucket_name" {
  description = "Globally unique S3 bucket name for SAM artifacts"
  type        = string
}

