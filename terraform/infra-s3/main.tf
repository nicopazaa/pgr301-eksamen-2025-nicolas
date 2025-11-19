terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "pgr301-terraform-state"
    key    = "infra-s3/terraform.tfstate"
    region = "eu-west-1"
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_s3_bucket" "analysis" {
  bucket = var.bucket_name

  tags = {
    Purpose = "analysis-results"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "lc" {
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
