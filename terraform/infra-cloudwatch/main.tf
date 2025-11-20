terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# SNS topic for varsler
resource "aws_sns_topic" "alerts" {
  name = "sentiment-alerts"
}

# E-post-subscription til SNS-topicen
resource "aws_sns_topic_subscription" "alerts_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# CloudWatch dashboard som viser to metrikker:
# - sentiment.analysis.total
# - sentiment.analysis.duration
resource "aws_cloudwatch_dashboard" "sentiment" {
  dashboard_name = var.dashboard_name

  dashboard_body = jsonencode({
    widgets = [
      {
        "type"       = "metric",
        "x"          = 0,
        "y"          = 0,
        "width"      = 12,
        "height"     = 6,
        "properties" = {
          "title"   = "Total sentiment analyses (per minute)",
          "region"  = var.aws_region,
          "metrics" = [
            # [Namespace, MetricName, {optional dimensjoner/stat}]
            ["SentimentApp", "sentiment.analysis.total", { "stat" : "Sum" }]
          ],
          "view"   = "timeSeries",
          "stacked" = false,
          "period" = 60
        }
      },
      {
        "type"       = "metric",
        "x"          = 12,
        "y"          = 0,
        "width"      = 12,
        "height"     = 6,
        "properties" = {
          "title"   = "Average analysis duration (ms)",
          "region"  = var.aws_region,
          "metrics" = [
            ["SentimentApp", "sentiment.analysis.duration", { "stat" : "Average" }]
          ],
          "view"   = "timeSeries",
          "stacked" = false,
          "period" = 60
        }
      }
    ]
  })
}

# Alarm på høy latency for sentiment-analysis
resource "aws_cloudwatch_metric_alarm" "high_latency" {
  alarm_name          = var.alarm_name
  alarm_description   = "Triggers when average sentiment analysis duration is high."
  namespace           = "SentimentApp"
  metric_name         = "sentiment.analysis.duration"
  statistic           = "Average"
  period              = 60          # 1 min
  evaluation_periods  = 3           # 3 datapunkter på rad
  threshold           = 2000        # 2000 ms (2 sek)
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [
    aws_sns_topic.alerts.arn
  ]
}
