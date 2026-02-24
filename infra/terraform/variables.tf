variable "aws_region" {
  description = "AWS region (set via TF_VAR_aws_region or AWS_REGION env var)"
  type        = string
  default     = null
}

variable "aws_access_key" {
  description = "AWS access key (set via TF_VAR_aws_access_key or AWS_ACCESS_KEY_ID env var)"
  type        = string
  sensitive   = true
  default     = null
}

variable "aws_secret_key" {
  description = "AWS secret key (set via TF_VAR_aws_secret_key or AWS_SECRET_ACCESS_KEY env var)"
  type        = string
  sensitive   = true
  default     = null
}

variable "aws_endpoint" {
  description = "Custom AWS endpoint URL (set via TF_VAR_aws_endpoint or AWS_ENDPOINT env var)"
  type        = string
  default     = null
}

variable "use_localstack" {
  description = "Whether to use LocalStack (skips credential validation and uses custom endpoints)"
  type        = bool
  default     = false
}

variable "queue_names" {
  description = "List of SQS queue names to create"
  type        = list(string)
}

variable "tags" {
  description = "Common tags to apply to all resources"
  type        = map(string)
}
