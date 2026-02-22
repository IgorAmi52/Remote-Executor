output "sqs_queue_urls" {
  description = "URLs of the created SQS queues"
  value       = module.sqs.queue_urls
}

output "sqs_queue_arns" {
  description = "ARNs of the created SQS queues"
  value       = module.sqs.queue_arns
}

output "ec2_instance_id" {
  description = "ID of the EC2 instance"
  value       = module.ec2.instance_id
}

output "ec2_instance_private_ip" {
  description = "Private IP of the EC2 instance"
  value       = module.ec2.instance_private_ip
}
