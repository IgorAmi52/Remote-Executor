output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.executor.id
}

output "instance_private_ip" {
  description = "Private IP of the EC2 instance"
  value       = aws_instance.executor.private_ip
}

output "instance_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_instance.executor.public_ip
}

output "instance" {
  description = "The complete EC2 instance object"
  value       = aws_instance.executor
}
