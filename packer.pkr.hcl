# 변수 선언: 기본값을 비워두어 실행 시 주입을 강제함
variable "aws_region" {
    type = string
    default = "ap-northeast-2"
}
variable "aws_account_id" { type = string }
variable "ecr_repo_name" { type = string }
variable "image_tag" { type = string }
variable "packer_ec2_role" { type = string }
variable "core_api_ami_name" {
    type = string
    default = "greenlight-core-api-live-ami-latest"
}
variable "ami_ssm_parameter" {
    type = string
    default = "greenlight-core-api-live-ami-id"
}
packer {
  required_plugins {
    amazon = {
      version = ">= 1.2.8"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

source "amazon-ebs" "al2023_golden" {
  region        = var.aws_region
  ami_name      = var.core_api_ami_name
  instance_type = "t3.small"
  ssh_username  = "ec2-user"

  force_deregister      = true
  force_delete_snapshot = true

  source_ami_filter {
    filters = {
      name                = "al2023-ami-2023.*-x86_64"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["amazon"]
  }

  # Packer가 빌드용 임시 인스턴스를 띄울 때 사용할 IAM Profile
  iam_instance_profile = var.packer_ec2_role
}

build {
  sources = ["source.amazon-ebs.al2023_golden"]

  provisioner "shell" {
    inline = [
      "sudo dnf update -y",
      "sudo dnf install -y docker",
      "sudo systemctl enable docker",
      "sudo systemctl start docker",
      "sudo usermod -aG docker ec2-user",
      
      # 변수를 사용하여 ECR 로그인 및 Pull 수행
      "aws ecr get-login-password --region ${var.aws_region} | sudo docker login --username AWS --password-stdin ${var.aws_account_id}.dkr.ecr.${var.aws_region}.amazonaws.com",
      "sudo docker pull ${var.aws_account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/${var.ecr_repo_name}:latest",
      
      "sudo docker pull grafana/promtail:3.5",
      "sudo docker pull prom/node-exporter:v1.9.1"
    ]
  }

  # 빌드 성공 후 SSM Parameter Update (AWS CLI 활용)
  post-processor "shell-local" {
    inline = [
      "AMI_ID=$(aws ec2 describe-images --filters 'Name=name,Values=${var.core_api_ami_name}' --query 'Images[0].ImageId' --output text --region ${var.aws_region})",
      "aws ssm put-parameter --name '${var.ami_ssm_parameter}' --value $AMI_ID --type String --data-type aws:ec2:image --overwrite --region ${var.aws_region}"
    ]
  }
}