### Spring Boot and AWS



### Introduction
In this article we will learn about everything that is required for deploying a Spring Boot application
to AWS. We will deploy a Docker container and learn how to use AWS CloudFormation and the AWS Cloud Development kit (CDK)
to automate deployments and build a continuous deployment pipeline with Github Actions.

We will also learn about several AWS services that we can use for common tasks. We will build a user registration and login by
leveraging Amazon Cognito. We will connect our Spring Boot application with a relational database.
We will also build a feature to share todos between users fully managed by AWS.

We will then look at some of the important aspects for running an application in production. We will learn how to use Amazon CloudWatch
to view logs and metrics. By actively monitoring our application and creating alerts we will be able to detect failures quickly.

Useful links:
https://github.com/stratospheric-dev/stratospheric/tree/main/chapters
https://stratospheric.dev/


### Spring Boot and AWS
Spring Boot is the leading framework for building applications with Java. With Spring Boot we can quickly build
production-ready software. AWS is the leading cloud platform and we can make use of a vast array of AWS cloud services
to help us architect, build and deploy software applications. Netflix and Atlassian use AWS and run all of their 
Software as a Service applications on AWS infrastructure. This article will help you to integrate Spring Boot applications with
AWS. Operational work can seem daunting as a developer particularly since we are used to depending on operations teams to
deploy our code. However, the control that is offered by becoming a Devops engineer is empowering and learning a few AWS services
and tips and tricks can help take your devops skills to the next level.

In this article we will learn hands-on how to get a Spring Boot application into the cloud and operate it on the cloud.
We will build a continuous deployment pipeline, access AWS services from the Spring Boot application and learn how to monitor 
the application on the cloud. 

### Deploying with AWS
This link is useful for setting up the aws cli:
https://docs.aws.amazon.com/cli/latest/reference/

These are commands to create a vpc:
```bash
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region ap-southeast-2
aws ec2 describe-vpcs --region ap-southeast-2
aws ec2 delete-vpc --region ap-southeast-2 --vpc-id=<VPC-ID>
aws ec2 describe-vpcs --region ap-southeast-2
```
This is an example vpc configuration using CloudFormation. This file contains a stack of resources.
This file describes a stack:
```yaml
AWSTemplateFormatVersion: '2010-09-09'

Description: Deploys a VPC

Resources:

  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: '10.0.0.0/16'

Outputs:

  VPCId:
    Description: The ID of the VPC that this stack is deployed in
    Value: !Ref 'VPC'
```
We can use these commands to create and delete a CloudFormation stack:
```bash
aws cloudformation deploy --template-file vpc-stack.yml --stack-name vpc-stack --region ap-southeast-2
aws cloudformation delete-stack --stack-name vpc-stack --region ap-southeast-2
aws cloudformation describe-stacks --region ap-southeast-2
```
We can also use the CDK to deploy a stack:

```java

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();
        
        new CdkStack(app, "CdkStack", StackProps.builder()
                .env(Environment.builder()
                .account("221875718260")
                .region("ap-southeast-2")
                .build())
                .build());
        
        app.synth();
    }
}

```

This is the Stack for the CdkApp:

```java
public class CdkStack extends Stack {
    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }
    
    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        new Vpc(this, "vpc", VpcProps.builder()
                .cidr("10.0.0.0/24")
                .build());
    }
}
```

The above stack creates VPC. CDK calls the CloudFormation API. This allows us to build our own construct libraries.
This makes the configuration more useful. In order to run the CDK code we should use the CDK command line tool which we
can install with the following:
```bash
npm install -g aws-cdk             # install latest version

```

AWS allows us to use the services via the web console, AWS CLI, CloudFormation and CDK.
CloudFormation allows us to declare resources and deploy them with a single command. CDK allows
us to combine resources to "constructs" and share them as Java libraries.

### Using AWS (setup)
- [AWS Sign up](https://aws.amazon.com/free/?all-free-tier.sort-by=item.additionalFields.SortRank&all-free-tier.sort-order=asc&awsf.Free%20Tier%20Types=*all&awsf.Free%20Tier%20Categories=*all)
- [AWS Sign in](https://aws.amazon.com/)
- [Guide to install the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- AWS CLI commands
```bash 
aws --version
aws configure
aws configure --profile stratospheric
export AWS_PROFILE=stratospheric
aws ec2 describe-vpcs --profile stratospheric
```
This is how to check your configuration:
```bash
tom@tom-ubuntu:~/.aws$ aws configure --profile default
AWS Access Key ID [****************HJEB]: 
AWS Secret Access Key [****************mdFI]: 
Default region name [eu-west-2]: 
Default output format [None]: 
```

We can update our cli with:
```bash
pip3 install awscli --upgrade --user
```
We should add our configuration for a different profile with the following:
```bash
aws configure --profile stratospheric
```
We then add the credentials and configuration we wish to use with the profile. The profile information
is kept in the .aws folder.
In this section we have looked at creating a "free" AWS account. We have installed the AWS CLI and we can use profiles
if we want to manage multiple AWS accounts with the AWS CLI.

### Elastic Container Registry
Here we will learn how to wrap an app into a Docker image. We will also learn how to create a Docker repository with Amazon
Elastic Container Registry (ECR) and how to publish a Docker image to an Elastic Container Registry (ECR)
repository.




