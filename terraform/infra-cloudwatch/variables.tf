variable "aws_region" {
  description = "AWS region for CloudWatch resources"
  type        = string
  default     = "eu-north-1"
}

variable "alert_email" {
  description = "Email address that will receive CloudWatch alarm notifications via SNS"
  type        = string
}

variable "dashboard_name" {
  description = "Name of the CloudWatch dashboard"
  type        = string
  default     = "sentiment-dashboard"
}

variable "alarm_name" {
  description = "Name of the CloudWatch high-latency alarm"
  type        = string
  default     = "sentiment-analysis-high-latency"
}
