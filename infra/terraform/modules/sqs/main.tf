resource "aws_sqs_queue" "queues" {
  for_each = toset(var.queue_names)

  name                       = each.value
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600  # 4 days
  max_message_size           = 262144  # 256 KB
  delay_seconds              = 0
  receive_wait_time_seconds  = 0

  tags = merge(
    var.tags,
    {
      Name = each.value
    }
  )
}
