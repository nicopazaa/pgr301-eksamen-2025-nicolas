provider "aws" {
  region = "eu-north-1"
}

resource "aws_sns_topic" "alerts" {
  name = "sentiment-alerts"
}

resource "aws_cloudwatch_dashboard" "dash" {
  dashboard_name = "sentiment-dashboard"
  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          metrics = [["SentimentApp","analysis_count"]]
          period  = 60
          stat    = "Sum"
          title   = "Antall analyser"
        }
      }
    ]
  })
}

resource "aws_cloudwatch_metric_alarm" "alarm" {
  alarm_name          = "low-analysis-rate"
  metric_name         = "analysis_count"
  namespace           = "SentimentApp"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "LessThanOrEqualToThreshold"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
