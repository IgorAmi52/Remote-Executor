variable "queue_names" {
  description = "List of SQS queue names to create"
  type        = list(string)
}

variable "tags" {
  description = "Tags to apply to SQS queues"
  type        = map(string)
  default     = {}
}
