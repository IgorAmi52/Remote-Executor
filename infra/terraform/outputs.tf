output "sqs_queue_urls" {
  description = "URLs of the created SQS queues"
  value       = module.sqs.queue_urls
}

output "sqs_queue_arns" {
  description = "ARNs of the created SQS queues"
  value       = module.sqs.queue_arns
}
