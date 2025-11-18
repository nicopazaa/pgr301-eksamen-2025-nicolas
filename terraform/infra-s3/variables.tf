
variable "aws_region" { default = "eu-west-1" }
variable "bucket_name" { type = string }
variable "transition_days" { default = 7 }
variable "expiration_days" { default = 30 }
