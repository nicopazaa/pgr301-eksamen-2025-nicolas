output "dashboard_name" {
  description = "CloudWatch dashboard name"
  value       = aws_cloudwatch_dashboard.sentiment.dashboard_name
}

output "alarm_arn" {
  description = "ARN of the high-latency CloudWatch alarm"
  value       = aws_cloudwatch_metric_alarm.high_latency.arn
}

output "sns_topic_arn" {
  description = "ARN of the SNS topic for alerts"
  value       = aws_sns_topic.alerts.arn
}
