variable "aws_region" {
  description = "Region for the analysis S3 bucket"
  type        = string
  default     = "eu-north-1"
}

variable "bucket_name" {
  description = "Name of the S3 bucket for analysis results"
  type        = string
}

variable "transition_days" {
  description = "Days before objects in midlertidig/ transition to GLACIER"
  type        = number
  default     = 7
}

variable "expiration_days" {
  description = "Days before objects in midlertidig/ are permanently deleted"
  type        = number
  default     = 30
}
