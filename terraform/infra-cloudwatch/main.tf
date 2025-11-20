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
  region = "eu-north-1"
}

# SNS-topic som brukes av alarmen
resource "aws_sns_topic" "alerts" {
  name = "sentiment-alerts"
}

# CloudWatch dashboard som viser de viktigste Micrometer-metrikkene
resource "aws_cloudwatch_dashboard" "sentiment" {
  dashboard_name = "sentiment-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      # total antall analyser per minutt
      {
        "type" : "metric",
        "x" : 0,
        "y" : 0,
        "width" : 12,
        "height" : 6,
        "properties" : {
          "title" : "Total sentiment analyses (per minute)",
          "metrics" : [
            ["SentimentApp", "sentiment.analysis.total", { "stat" : "Sum" }]
          ],
          "region" : "eu-north-1",
          "view" : "timeSeries",
          "stat" : "Sum",
          "period" : 60
        }
      },

      # gjennomsnittlig responstid i ms
      {
        "type" : "metric",
        "x" : 12,
        "y" : 0,
        "width" : 12,
        "height" : 6,
        "properties" : {
          "title" : "Average analysis duration (ms)",
          "metrics" : [
            ["SentimentApp", "sentiment.analysis.duration", { "stat" : "Average" }]
          ],
          "region" : "eu-north-1",
          "view" : "timeSeries",
          "stat" : "Average",
          "period" : 60
        }
      }
    ]
  })
}

# Alarm på høy responstid fra Bedrock-analysen
resource "aws_cloudwatch_metric_alarm" "high_latency" {
  alarm_name         = "sentiment-analysis-high-latency"
  alarm_description  = "Alarm if average sentiment analysis duration is above 2 seconds for 3 consecutive minutes"
  namespace          = "SentimentApp"
  metric_name        = "sentiment.analysis.duration"
  statistic          = "Average"
  period             = 60
  evaluation_periods = 3
  threshold          = 2000        # 2000 ms = 2 sekunder
  comparison_operator = "GreaterThanThreshold"

  alarm_actions = [aws_sns_topic.alerts.arn]
}
