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

![image](https://user-images.githubusercontent.com/27693622/228086206-499a178c-237a-4689-a6c4-afdb829c74c6.png)

The application that we will deploy is a simple Hello World app. This is the Dockerfile for the deployment:

```dockerfile
FROM eclipse-temurin:17-jre

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```
This Dockerfile downloads Java 17, builds the application to a Docker image with an entry point to the jar file.
We build the jar file with:

```bash
./gradlew clean build
```

We then build the docker image:
```bash
docker build -t hello-world-app .
```
Here we give the docker image the name hello-world-app.
We check for our docker image with the follow grep on docker image ls:
```bash
tom@tom-ubuntu:~/Projects/stratospheric-dev$ docker image ls | grep hello-world-app
hello-world-app   latest    f1307dc16f7e   48 seconds ago   288MB
```
We can then run the application and expose the app to port 8080 on our host computer:
```bash
docker run -p 8080:8080 hello-world-app
```
We can now see the application on port 8080:
![image](https://user-images.githubusercontent.com/27693622/228090110-97714941-4bde-4d15-975b-61afe94a347f.png)

We are now going to create a docker repository on Amazon ECR. We now go to Amazon Elastic Container Registry on AWS.
We should enable tag immutability to avoid changing tags later.
![image](https://user-images.githubusercontent.com/27693622/228090443-13b96768-ad9e-4ce8-ba4d-ccc8d8a909b7.png)

We then create the repository.
![image](https://user-images.githubusercontent.com/27693622/228090503-74083a52-8014-4cba-986e-3091288e6884.png)

For the moment it has no Docker images:
![image](https://user-images.githubusercontent.com/27693622/228090543-70e8f754-d374-4f72-8990-e9779697c472.png)

We now tag our image locally with the url of our new docker repository and the tags latest and v1:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ docker tag hello-world-app 706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app:latest
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ docker tag hello-world-app 706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app:v1
```
We can check the tags by looking for our images in our local repository:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ docker image ls | grep hello-world-app
706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app   latest    e4d2957745e5   3 minutes ago   288MB
706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app   v1        e4d2957745e5   3 minutes ago   288MB
hello-world-app                                                latest    e4d2957745e5   3 minutes ago   288MB
```
We now have two images with the url of our ECR repository and tagged with latest and v1 respectively.
We now log into the docker repository to publish our image. We use the AWS cli and login to docker with our aws repository:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 706054169063.dkr.ecr.us-east-1.amazonaws.com 
WARNING! Your password will be stored unencrypted in /home/tom/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
```

We then push the latest image to our repository:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ docker push 706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app:latest
```
We can now see the image in our ECR repository:
![image](https://user-images.githubusercontent.com/27693622/228092241-08562c73-46e7-40fa-836a-16d7073425a2.png)

We also push v1:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/helloWorld$ docker push 706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app:v1
The push refers to repository [706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app]
8e12efddf5f4: Layer already exists 
5ff725d720ec: Layer already exists 
3b028c775252: Layer already exists 
f3a12c51479f: Layer already exists 
b93c1bd012ab: Layer already exists 
v1: digest: sha256:5b1075ab16f8bfdd00930963af1bfaca318fd73715aaccdb1e77c865d6dd1d40 size: 1372
```

We now have the tag of v1 and latest in our repository:
![image](https://user-images.githubusercontent.com/27693622/228092462-bae26fb7-0156-4c5f-9ea5-0876cd72e99c.png)

So far we have created a Docker repository with ECR. We create one Docker repository per app. We can also pull and push to the ECR repository
from the AWS CLI.

#### CloudFormation
CloudFormation is the AWS service for infrastructure as code. AWS creates the resources that we declare in our CloudFormation file.
We now look at resources and stacks. We will look at how to create a stack template in yaml and how to create a stack from
a template file. We will also find our stack on the AWS web console.

![image](https://user-images.githubusercontent.com/27693622/228094645-7efebfc7-b999-4940-9677-8b8f2780ecb5.png)

