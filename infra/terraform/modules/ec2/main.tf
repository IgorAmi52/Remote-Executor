resource "aws_instance" "executor" {
  ami           = var.ami_id
  instance_type = var.instance_type

  tags = merge(
    var.tags,
    {
      Name = var.instance_name
    }
  )
}
