terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state for this part of the infra
  backend "s3" {
    bucket = "pgr301-terraform-state-nicopazaa-01"
    key    = "infra-s3/terraform.tfstate"
    region = "eu-north-1"
  }
}

provider "aws" {
  region = var.aws_region
}

# S3 bucket for analysis results
resource "aws_s3_bucket" "analysis" {
  bucket = var.bucket_name

  tags = {
    Name        = "analysis-results-bucket"
    Purpose     = "analysis-results"
    Environment = "exam"
  }
}

# Lifecycle: objects under midlertidig/ -> Glacier -> slettes
resource "aws_s3_bucket_lifecycle_configuration" "analysis_lifecycle" {
  bucket = aws_s3_bucket.analysis.id

  rule {
    id     = "midlertidig-expire"
    status = "Enabled"

    filter {
      prefix = "midlertidig/"
    }

    transition {
      days          = var.transition_days
      storage_class = "GLACIER"
    }

    expiration {
      days = var.expiration_days
    }
  }
}
