output "sam_artifacts_bucket_name" {
  description = "Name of the S3 bucket used for SAM artifacts"
  value       = aws_s3_bucket.sam_artifacts.bucket
}

