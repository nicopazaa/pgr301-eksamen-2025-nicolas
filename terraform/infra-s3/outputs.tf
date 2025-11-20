output "analysis_bucket_name" {
  description = "Name of the analysis S3 bucket"
  value       = aws_s3_bucket.analysis.bucket
}

output "analysis_bucket_region" {
  description = "Region of the analysis S3 bucket"
  value       = var.aws_region
}
