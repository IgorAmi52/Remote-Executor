terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region     = var.aws_region
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key

  # LocalStack-specific settings
  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack

  dynamic "endpoints" {
    for_each = var.use_localstack && var.aws_endpoint != "" ? [1] : []
    content {
      sqs = var.aws_endpoint
      ec2 = var.aws_endpoint
    }
  }
}

module "sqs" {
  source = "./modules/sqs"

  queue_names = var.queue_names
  tags        = var.tags
}

module "ec2" {
  source = "./modules/ec2"

  instance_name = var.instance_name
  instance_type = var.instance_type
  ami_id        = var.ami_id
  tags          = var.tags
}
