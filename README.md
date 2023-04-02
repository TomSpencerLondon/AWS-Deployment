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
We declare the resources we need in a stack template. These might include a VPC, Load balancer and a database. We declare the
resources in a stack file and CloudFormation deploys the resources for us. The CloudFormation docs list all the resources
we can deploy with CloudFormation:
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html

For each of the resources we need to refer to the documentation for each resource. A stack is a group of resources that belong
together. All the resources in the stack are created updated and deleted together.
We are now going to create a CloudFormation stack to create an ECR repository:

We will create a Docker repository stack:
![image](https://user-images.githubusercontent.com/27693622/228096362-770b5794-ecf3-4cea-830b-30305b5414d1.png)

We first need to look at the CloudFormation docs and ECR repository in particular:
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ecr-repository.html

This shows us the options that we can use for our stack:
```yaml
Type: AWS::ECR::Repository
Properties: 
  EncryptionConfiguration: 
    EncryptionConfiguration
  ImageScanningConfiguration: 
    ImageScanningConfiguration
  ImageTagMutability: String
  LifecyclePolicy: 
    LifecyclePolicy
  RepositoryName: String
  RepositoryPolicyText: Json
  Tags: 
    - Tag

```
Each of the options are documented on the page. We can use the configuration details given on the page to create ecr.yml page:
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Deploys a Docker container registry.
Parameters:
  RegistryName:
    Type: String
    Description: The name of the container registry
Resources:
  Registry:
    Type: AWS::ECR::Repository
    Properties:
      RepositoryName: !Ref 'RegistryName'
      LifecyclePolicy:
        LifecyclePolicyText: |
          {
              "rules": [
                  {
                      "rulePriority": 10,
                      "description": "Limit to 10 images",
                      "selection": {
                         "tagStatus": "any",
                         "countType": "imageCountMoreThan",
                         "countNumber": 10
                     },
                      "action": {
                          "type": "expire"
                      }
                  }
              ]
          }
      RepositoryPolicyText:
        Version: "2012-10-17"
        Statement:
          - Sid: AllowPushPull
            Effect: Allow
            Principal:
              AWS:
                - "arn:aws:iam::706054169063:user/tom-developer"
            Action:
              - "ecr:GetDownloadUrlForLayer"
              - "ecr:BatchGetImage"
              - "ecr:BatchCheckLayerAvailability"
              - "ecr:PutImage"
              - "ecr:InitiateLayerUpload"
              - "ecr:UploadLayerPart"
              - "ecr:CompleteLayerUpload"
```
The parameters section:
```yaml
Parameters:
  RegistryName:
    Type: String
    Description: The name of the container registry
```
means that when we deploy the stack we can add a parameter with the registry name. This means we can
reuse the stack. We set the name of the repository (RepositoryName) with the input parameter we added earlier:
```yaml
Resources:
  Registry:
    Type: AWS::ECR::Repository
    Properties:
      RepositoryName: !Ref 'RegistryName'
```
Here the lifecycle policy sets a limit of 10 images and will delete images when the number of images is above that number:
```yaml
LifecyclePolicy:
        LifecyclePolicyText: |
          {
              "rules": [
                  {
                      "rulePriority": 10,
                      "description": "Limit to 10 images",
                      "selection": {
                         "tagStatus": "any",
                         "countType": "imageCountMoreThan",
                         "countNumber": 10
                     },
                      "action": {
                          "type": "expire"
                      }
                  }
              ]
          }
```
The repository policy text specifies the allowed actions on the repository for the Principal users that we define.
The actions include sending images to the repository and downloading images.
We deploy the stack with the following command:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/cloudformation$ aws cloudformation deploy \
> --stack-name=hello-world-docker-repository \
> --template-file ecr.yml \
> --parameter-overrides RegistryName=hello-world-app \
> --profile stratospheric

Waiting for changeset to be created..
Waiting for stack create/update to complete
Successfully created/updated stack - hello-world-docker-repository
```
This has created a Docker ECR repository in the eu-west-2 region:
![image](https://user-images.githubusercontent.com/27693622/228100382-2dd3dff3-bb87-4a1d-9bba-189381557cc1.png)

If we use the same stack-name we can change the properties of the same stack.
We can also see the CloudFormation stack on the CloudFormation page:
![image](https://user-images.githubusercontent.com/27693622/228100696-a5b4bf0f-ce29-4e1b-9743-d4959a05fd85.png)

We can also look at the hollow-world-docker-repository and the actions and events for the CloudFormation stack:
![image](https://user-images.githubusercontent.com/27693622/228100840-c14277fe-43b8-42ac-bf80-fba78d2c0fed.png)

So far we have declared resources in a stack file. CloudFormation has taken care of the lifecycle of the resources that we
defined. We have parameterised the stack file so that we can use the stack file for multiple deployments with different parameters.

### Deploying a Network Stack with Cloud Formation
In this section we will look at why we use separate CloudFormation stacks for Network deployments.
We will look at the networking resources AWS provides. We will also learn more about the CloudFormation stack file
syntax.


![image](https://user-images.githubusercontent.com/27693622/228186447-f3c90300-de92-42a2-b26b-31dd7c79ccc0.png)

The above network stack contains all the resources we need so that our users can access the internet. It also includes
internal networking so that our application can access the database for example. We deploy a network stack which can be deployed
once. The application is deployed in the service stack which includes the compute resources that we require for our application.
There will be different requirements in different contexts.

![image](https://user-images.githubusercontent.com/27693622/228189708-39fefa05-6763-4115-839e-16bfcbfd6029.png)

The above is an overview of the main resources that our stack will contain. This is what the network stack yaml file will
look like:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: A basic network stack that creates a VPC with a single public subnet and some ECS resources that we need to start a Docker container within this subnet.
Resources:

  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: '10.0.0.0/16'

  PublicSubnet:
    Type: AWS::EC2::Subnet
    Properties:
      AvailabilityZone:
        Fn::Select:
          - 0
          - Fn::GetAZs: {Ref: 'AWS::Region'}
      VpcId: !Ref 'VPC'
      CidrBlock: '10.0.1.0/24'
      MapPublicIpOnLaunch: true

  InternetGateway:
    Type: AWS::EC2::InternetGateway

  GatewayAttachment:
    Type: AWS::EC2::VPCGatewayAttachment
    Properties:
      VpcId: !Ref 'VPC'
      InternetGatewayId: !Ref 'InternetGateway'

  PublicRouteTable:
    Type: AWS::EC2::RouteTable
    Properties:
      VpcId: !Ref 'VPC'

  PublicSubnetRouteTableAssociation:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet
      RouteTableId: !Ref PublicRouteTable

  PublicRoute:
    Type: AWS::EC2::Route
    DependsOn: GatewayAttachment
    Properties:
      RouteTableId: !Ref 'PublicRouteTable'
      DestinationCidrBlock: '0.0.0.0/0'
      GatewayId: !Ref 'InternetGateway'

  ECSCluster:
    Type: AWS::ECS::Cluster

  ECSSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Access to the ECS containers
      VpcId: !Ref 'VPC'

  ECSSecurityGroupIngressFromAnywhere:
    Type: AWS::EC2::SecurityGroupIngress
    Properties:
      Description: Allows inbound traffic from anywhere
      GroupId: !Ref 'ECSSecurityGroup'
      IpProtocol: -1
      CidrIp: 0.0.0.0/0

  ECSRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: [ecs.amazonaws.com]
            Action: ['sts:AssumeRole']
      Path: /
      Policies:
        - PolicyName: ecs-service
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  # Rules which allow ECS to attach network interfaces to instances
                  # on your behalf in order for awsvpc networking mode to work right
                  - 'ec2:AttachNetworkInterface'
                  - 'ec2:CreateNetworkInterface'
                  - 'ec2:CreateNetworkInterfacePermission'
                  - 'ec2:DeleteNetworkInterface'
                  - 'ec2:DeleteNetworkInterfacePermission'
                  - 'ec2:Describe*'
                  - 'ec2:DetachNetworkInterface'
                Resource: '*'

  ECSTaskExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: [ecs-tasks.amazonaws.com]
            Action: ['sts:AssumeRole']
      Path: /
      Policies:
        - PolicyName: AmazonECSTaskExecutionRolePolicy
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  # Allow the ECS Tasks to download images from ECR
                  - 'ecr:GetAuthorizationToken'
                  - 'ecr:BatchCheckLayerAvailability'
                  - 'ecr:GetDownloadUrlForLayer'
                  - 'ecr:BatchGetImage'

                  # Allow the ECS tasks to upload logs to CloudWatch
                  - 'logs:CreateLogStream'
                  - 'logs:PutLogEvents'
                Resource: '*'

Outputs:
  ClusterName:
    Description: The name of the ECS cluster
    Value: !Ref 'ECSCluster'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'ClusterName' ] ]
  ECSRole:
    Description: The ARN of the ECS role
    Value: !GetAtt 'ECSRole.Arn'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'ECSRole' ] ]
  ECSTaskExecutionRole:
    Description: The ARN of the ECS role
    Value: !GetAtt 'ECSTaskExecutionRole.Arn'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'ECSTaskExecutionRole' ] ]
  VPCId:
    Description: The ID of the VPC that this stack is deployed in
    Value: !Ref 'VPC'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'VPCId' ] ]
  PublicSubnet:
    Description: Public subnet one
    Value: !Ref 'PublicSubnet'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'PublicSubnet' ] ]
  ECSSecurityGroup:
    Description: A security group used to allow ECS containers to receive traffic
    Value: !Ref 'ECSSecurityGroup'
    Export:
      Name: !Join [ ':', [ !Ref 'AWS::StackName', 'ECSSecurityGroup' ] ]
```

This link is useful for considering the built in functions that we can use with AWS CloudFormation:
https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html
In the above stack we are using two CloudFormation intrinsic functions in particular ```Fn::Select``` and ```Fn::GetAZs```.
```Fn::Select``` returns a single object from a list of objects by index. ```Fn::GetAZs``` returns
an array that lists Availability Zones for a specified region in alphabetical order. So the following section of the above
CloudFormation stack:
```yaml
        Fn::Select:
          - 0
          - Fn::GetAZs: {Ref: 'AWS::Region'}
```
selects the first Availability Zone returned from the region inputted by the command calling the above CloudFormation template.
This doc is useful for [AWS::EC2::VPC](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-vpc.html).
The command specifies a virtual private cloud (VPC). In the above example we have specified a CIDR block for our VPC:
```yaml
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: '10.0.0.0/16'
```
This CIDR block contains IP addresses from 10.0.0.0 to 10.0.255.255. The 16 means that we can select any number between 0 and 256 on
the second to last and last sections of the 4 section IP address.
This doc is useful for [AWS::EC2::Subnet](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-subnet.html)

```yaml
  PublicSubnet:
    Type: AWS::EC2::Subnet
    Properties:
      AvailabilityZone:
        Fn::Select:
          - 0
          - Fn::GetAZs: {Ref: 'AWS::Region'}
      VpcId: !Ref 'VPC'
      CidrBlock: '10.0.1.0/24'
      MapPublicIpOnLaunch: true
```
The ```AWS::EC2::Subnet``` specifies a subnet for the specified VPC. Here we have selected the same Availability Zone
as the above VPC and use the command line argument to set the id of the VPC. The number 24 means that the earlier sections are fixed
and we can only use a range from 10.0.1.0 to 10.0.1.255 for our IP address. The entry ```MapPublicIpOnLaunch```
indicates whether instances launched in the subnet receive a public IPv4 address. The default value is ```false```.
Here we have selected true.

This doc is useful for [AWS::EC2::InternetGateway](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-internetgateway.html).
It allocates an internet gateful for use with our VPC. After creating the Internet Gateway we attach it to the VPC with VPCId 
entry. We also add an [AWS::EC2::RouteTable](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-routetable.html).
A route table is a set of rules, called routes, that determine where the network traffic from our subnet or gateway is directed.
Here we attach the Route Table to our VPC. We also use [AWS::EC2::AWS::EC2::SubnetRouteTableAssociation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-subnetroutetableassociation.html) to associate the subnet with the route table.
The [AWS::EC2::Route](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-ec2-route.html) specifies the route table within the VPC.
Here we specify the ```DestinationCidrBlock``` as 0.0.0.0/0. We also add the Public Gateway as a parameter on our command line argument.

```yaml


  InternetGateway:
    Type: AWS::EC2::InternetGateway

  GatewayAttachment:
    Type: AWS::EC2::VPCGatewayAttachment
    Properties:
      VpcId: !Ref 'VPC'
      InternetGatewayId: !Ref 'InternetGateway'

  PublicRouteTable:
    Type: AWS::EC2::RouteTable
    Properties:
      VpcId: !Ref 'VPC'

  PublicSubnetRouteTableAssociation:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet
      RouteTableId: !Ref PublicRouteTable

  PublicRoute:
    Type: AWS::EC2::Route
    DependsOn: GatewayAttachment
    Properties:
      RouteTableId: !Ref 'PublicRouteTable'
      DestinationCidrBlock: '0.0.0.0/0'
      GatewayId: !Ref 'InternetGateway'

```
We use the following command to deploy the above network stack:
```yaml
aws cloudformation deploy \
--stack-name=dev-network \
--template-file network.yml \
--capabilities CAPABILITY_IAM \
--profile stratospheric
```

The Elastic Container Stack (ECS) cluster is the service we use to contain the resources we want to deploy. It is in the network stack
because we will only deploy the ECS cluster once. We run the above command and can see that the stack has been created:

![image](https://user-images.githubusercontent.com/27693622/228232253-81b6be5e-be9f-4fae-a7d7-bd05ab409d9c.png)

We have created a network stack which will not need to change. We have added input parameters which we then use in the stack template.
We have also used the CloudFormation docs to explore the available AWS resources.

### Deploying Service Stack with CloudFormation
Here we will learn about ECS basics including clusters, services and tasks. We will define ECS resources with CloudFormation
and we will learn about the interactions that are possible between CloudFormation stacks.

#### ECS Basics
![image](https://user-images.githubusercontent.com/27693622/228235829-8392ec96-a701-42fc-800a-74e13ab598d8.png)

Elastic Container Service (ECS) is an AWS self service container orchestration service. The applications to deploy are called tasks.
In the diagram above we have multiple tasks. Service A runs two instances of Task 1 and one of Task 2. The application may need more resources
for Task 1 so that is why we have included them within this service. The cluster is a wrapper around one or more services and binds the services
to a specific AWS region and VPC. Each task is a process. We can now look at how each Task is assigned a process:

![image](https://user-images.githubusercontent.com/27693622/228239310-5b6508e7-ac80-4b85-8e8f-321fb5314800.png)

The Task Definition tells us what Docker image to run and then we use the image within that Docker Repository. There are two
different launch types for ECS - Fargate and EC2. The Fargate launch type does work for us. Fargate spins a fleet of required EC2 instances and manages
them for us after we have specified which tasks to run and how many task instances we want to have running at all times. The EC2
launch type allows us to control the EC2 instances that are running our tasks and we have to manage this process. We can define
auto scaling rules but have to define our own auto-scaling group. We are going to use the Fargate Launch type.
We have defined ECS in our network.yml file:
```yaml


  ECSCluster:
    Type: AWS::ECS::Cluster

  ECSSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Access to the ECS containers
      VpcId: !Ref 'VPC'

  ECSSecurityGroupIngressFromAnywhere:
    Type: AWS::EC2::SecurityGroupIngress
    Properties:
      Description: Allows inbound traffic from anywhere
      GroupId: !Ref 'ECSSecurityGroup'
      IpProtocol: -1
      CidrIp: 0.0.0.0/0
```
Here we define a security group that allows inbound traffic from anywhere. We set the IpProtocol to -1 which means any 
ip protocol. Later in the file we add IAM role for permission for ECS and logging. We are using the same ECS cluster for all
services.

### Deploying the Service Stack with CloudFormation

For our Service Stack we have added several parameters:
```yaml
Parameters:
  NetworkStackName:
    Type: String
    Description: The name of the networking stack that
      these resources are put into.
  ServiceName:
    Type: String
    Description: A human-readable name for the service.
  ImageUrl:
    Type: String
    Description: The url of a docker image that will handle incoming traffic.
  ContainerPort:
    Type: Number
    Default: 80
    Description: The port number the application inside the docker container
      is binding to.
  ContainerCpu:
    Type: Number
    Default: 256
    Description: How much CPU to give the container. 1024 is 1 CPU.
  ContainerMemory:
    Type: Number
    Default: 512
    Description: How much memory in megabytes to give the container.
  DesiredCount:
    Type: Number
    Default: 1
    Description: How many copies of the service task to run.
```

These include the network stack name, service name, image url and container port for each service that we want to deploy
within the cluster. We have a Task section to define the task within our service:
```yaml
  TaskDefinition:
    Type: AWS::ECS::TaskDefinition
    Properties:
      Family: !Ref 'ServiceName'
      Cpu: !Ref 'ContainerCpu'
      Memory: !Ref 'ContainerMemory'
      NetworkMode: awsvpc
      RequiresCompatibilities:
        - FARGATE
      ExecutionRoleArn:
        Fn::ImportValue:
          !Join [':', [!Ref 'NetworkStackName', 'ECSTaskExecutionRole']]
      ContainerDefinitions:
        - Name: !Ref 'ServiceName'
          Cpu: !Ref 'ContainerCpu'
          Memory: !Ref 'ContainerMemory'
          Image: !Ref 'ImageUrl'
          PortMappings:
            - ContainerPort: !Ref 'ContainerPort'
          LogConfiguration:
            LogDriver: 'awslogs'
            Options:
              awslogs-group: !Ref 'ServiceName'
              awslogs-region: !Ref AWS::Region
              awslogs-stream-prefix: !Ref 'ServiceName'
```

We also define the service with a maximum percent deployment configuration for when we redeploy Tasks within the service:
```yaml
  Service:
    Type: AWS::ECS::Service
    Properties:
      ServiceName: !Ref 'ServiceName'
      Cluster:
        Fn::ImportValue:
          !Join [':', [!Ref 'NetworkStackName', 'ClusterName']]
      LaunchType: FARGATE
      DeploymentConfiguration:
        MaximumPercent: 200
        MinimumHealthyPercent: 50
      DesiredCount: !Ref 'DesiredCount'
      NetworkConfiguration:
        AwsvpcConfiguration:
          AssignPublicIp: ENABLED
          SecurityGroups:
            - Fn::ImportValue:
                !Join [':', [!Ref 'NetworkStackName', 'ECSSecurityGroup']]
          Subnets:
            - Fn::ImportValue:
                !Join [':', [!Ref 'NetworkStackName', 'PublicSubnet']]
      TaskDefinition: !Ref 'TaskDefinition'
```

We will use the hello-world-app for deploying our service. We need the url of our hello-world-app.

![image](https://user-images.githubusercontent.com/27693622/228247393-17e9f8d5-cf00-448f-9c36-be3747cf5c5c.png)

This is the command for deploying the service:
```yaml
aws cloudformation deploy \
--stack-name dev-service \
--template-file service.yml \
--profile stratospheric \
--parameter-overrides \
  NetworkStackName=dev-network \
  ServiceName=hello-world-app \
  ImageUrl=706054169063.dkr.ecr.us-east-1.amazonaws.com/hello-world-app:latest
  ContainerPort=8080
```
We can then see the created stack on CloudFormation Stacks:
![image](https://user-images.githubusercontent.com/27693622/228248932-1bb1981f-c12d-44ee-a375-e0dec7b0f002.png)

We also now have one cluster running with one service and one running task:
![image](https://user-images.githubusercontent.com/27693622/228249482-e3f285de-e9d2-4f0f-b5dd-edec5b286a9a.png)

If we check the Task for the running Service we see that we have a running task and we can also see a public IP address:
![image](https://user-images.githubusercontent.com/27693622/228250488-1c72c1a3-b127-4d3b-bcf5-4e40f6107d18.png)

We can now type in the ip address at the port 8080:
![image](https://user-images.githubusercontent.com/27693622/228251115-bd13b3be-dc8a-4e57-95f2-18660e6e5602.png)

and we can see our running task using the Docker image that we deployed to ECR.

We have looked at ECS tasks which run docker images that run in an ECS service and ECS cluster.
A task can run one or more Docker images and we can use resources across different CloudFormation stacks using output and input
parameters. In order to replace our ip address we will use a load balancer as a single point of entry for the stack with a fixed host name.

### Using Cloud Development Kit (CDK)
With CloudFormation we declared the resources we want in a cloud yaml file. We will now use Infrastructure as Code with Java to describe
the cloud resources required. CDK is an abstraction on top of CloudFormation.
CDK introduces the idea of a construct to combine multiple resources. Level 1 constructs represent one CloudFormation Resource.
Level 2 Constructs represent a set of resources. Level 2 Constructs infer some of the options for us and set sensible defaults.
Level 3 Constructs create patterns for several resources.

We install the CDK CLI with:
```bash
tom@tom-ubuntu:~/Projects/stratospheric/cloudformation$ npm install -g aws-cdk
tom@tom-ubuntu:~/Projects/stratospheric/cloudformation$ cdk --version
2.70.0 (build c13a0f1)
```
We create a new cdk app with:
```yaml
cdk init app --language=java
```

This creates an application with two main parts. The App:
```java
public class CdkExampleApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkExampleStack(app, "CdkExampleStack", StackProps.builder()
                .build());

        app.synth();
    }
}
```
This class creates an App object and a stack object linked to the app. The synth command creates a synthesis
step to create a CloudFormation template from the Java code.

The Stack class is where we define the resources that we want to deploy:
```java
public class CdkExampleStack extends Stack {
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
    }
}


```

This is a maven project and we can use the following commands:
```bash
It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation
```
We need maven versions to be shared across different environments. This will help us to install the correct
maven version. We install the maven wrapper with the following command:
```bash
mvn wrapper:wrapper
```
This installs the .mvn file:

![image](https://user-images.githubusercontent.com/27693622/228281294-b317cdde-7692-4620-b332-e05439ae8a0c.png)

We can now use the command ```./mvnw``` with the same maven commands as before.

We also change the cdk.json to refer to the mvnw command:
```yaml
{
  "app": "mvnw -e -q compile exec:java"
}
```

We now need to run the following command to create infrastructure for cdk to deploy apps to CloudFormation:
```bash
cdk bootstrap --profile stratospheric
```
This creates an S3 bucket and IAM roles for the deployment.
We can now deploy the application with:
```bash
cdk deploy --profile stratospheric
```

We can now see the CloudFormation stack in the CloudFormation web console:
![image](https://user-images.githubusercontent.com/27693622/228283658-8d12172c-be82-4c3c-b734-a5f577ccb918.png)

At the moment the stack is empty. We can now create the ECS repository with CDK using the following doc:
https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_ecr.Repository.html

Level 2 Constructs are listed as Constructs. The level 1 Constructs are listed as Cfn... For the moment
we will stick with Level 2 Constructs. We get the correct methods for our CDK Stack with the following:
https://docs.aws.amazon.com/cdk/api/v2/java/index.html?software/amazon/awscdk/services/ecr/Repository.html

Our CdkExampleStack now looks like the following:
```java
public class CdkExampleStack extends Stack {
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Repository.Builder.create(this, "repository")
                .repositoryName("hello-world-repo")
                .imageScanOnPush(Boolean.TRUE)
                .lifecycleRules(Arrays.asList(LifecycleRule.builder()
                        .maxImageCount(10)
                        .build()))
                .imageTagMutability(TagMutability.IMMUTABLE)
                .build();
    }
}
```
We can now deploy our cdk app with:
```bash
cdk deploy --profile stratospheric
```
This has created a CloudFormation stack
![image](https://user-images.githubusercontent.com/27693622/228288969-2b1ea3bc-36ce-4357-807f-780d5cf633a9.png)

and a hello-world-repo ECR repository:

![image](https://user-images.githubusercontent.com/27693622/228289184-d94ff025-5e04-4095-a606-7f523dfbce24.png)

For now this repository is empty. We will use the Stratospheric cdk construct maven dependency [here](https://github.com/stratospheric-dev/cdk-constructs/blob/main/src/main/java/dev/stratospheric/cdk/DockerRepository.java)
to add more to our repository configuration. We will add this configuration with the following maven dependency:
```properties
<dependency>
    <groupId>dev.stratospheric</groupId>
    <artifactId>cdk-constructs</artifactId>
    <version>0.1.13</version>
</dependency>
```

We can now reduce the code using the stratospheric cdk-constructs dependency:
```java
public class CdkExampleStack extends Stack {
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String accountId = "12345";
        Environment environment = Environment.builder()
                .account(accountId)
                .region("ap-southeast-2")
                .build();

        DockerRepository repo = new DockerRepository(this, "repo", environment,
                new DockerRepository.DockerRepositoryInputParameters(
                        "hello-world-repo",
                        accountId,
                        10
                ));
    }
}

```
This will do the same as above but also add push and pull permissions. We can delete the hello-world-repo
and create a new stack with the following command:

```bash
cdk deploy --profile stratospheric
```

This fails because we have the incorrect AccountID in /stratospheric/cdk-example/cdk.out/CdkExampleStack.template.json:
```json
[
  "arn:",
   {
    "Ref": "AWS::Partition"
   },
  ":iam::12345:root"
]
```
We will look at how to fix this error in the next section.

In this section, we have abstracted some of CloudFormation's complexity with Constructs and used some of our knowledge of
CloudFormation resources. We have also used a shared library to share the construct across projects.

### CDK Best Practices
We will now learn how to debug a CDK app. We will manage different environments with CDK, manage input parameters and
also manage multiple stacks.

First we correct the above code with an environment object:
```java
public class CdkExampleApp {
    public static void main(final String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        String region = (String) app.getNode().tryGetContext("region");

        Environment environment = makeEnv(accountId, region);

        new CdkExampleStack(app, "CdkExampleStack", StackProps.builder()
                .env(environment)
                .build());

        app.synth();
    }

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}

```
The environment can then be used in the Stack directly:

```java
public class CdkExampleStack extends Stack {
    public CdkExampleStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkExampleStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Environment env = props.getEnv();

        DockerRepository repo = new DockerRepository(this, "repo", env,
                new DockerRepository.DockerRepositoryInputParameters(
                        "hello-world-repo",
                        env.getAccount(),
                        10
                ));
    }
}


```

We now need to pass the accountId into our command. We can call the above code with the following:
```bash
cdk deploy -c accountId=<ACCOUNT_ID> region=eu-west-2
```
This successfully creates the hello-world-repo on ECR.


### AWS AppConfig Agent for Containers
- What is AWS AppConfig?
- Why did we develop an agent?
- About the agent
- Container image
- Demo: Using the agent in ECS

#### What is AWS AppConfig?
AppConfig is changing the way software is developed released and run. It is a dynamic configuration service.
Configuration changes the way the software runs depending on the environment. For instance we may want to use different
databases for different environments. Before dynamic configuration we had static configuration.
For applications we have source code and configuration data. Static Configuration can be time consuming.
In order to change endpoints we would have to roll back the application.
Dynamic configuration allows us to deploy the configuration data from a remote service at runtime.
If we use AppConfig we can change configuration separately from the deployment.
There are deployment safety configurations we can use with AppConfig which allow us to deploy the configuration changes
gradually. AppConfig includes Feature Flags, Access Control Lists and throttling limits.
Throttling is a DDOS protection mechanism which allows a limit of requests to the service. You can allow overrides for
particular users.

#### AppConfig Agent
Before AppConfig Agent management logic including caching, background polling and session management needed to be contained
within AppConfig directly. The agent is a sidecar process that runs alongside the application. It allows us to add configuration
separately. Customers can then set their own config themselves. This is the image gallery:
![image](https://user-images.githubusercontent.com/27693622/228269559-1bc3b3f6-d720-454f-b31e-18a7bfa6eb74.png)

These doc is useful for AWS AppConfig integration with Amazon ECS and Amazon EKS:
https://docs.aws.amazon.com/appconfig/latest/userguide/appconfig-integration-containers-agent.html

We have now added two Apps to be deployed and deleted the plugin code relating to the default App:
```bash
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.0.0</version>
                <configuration>
                    <mainClass>com.myorg.CdkExampleApp</mainClass>
                </configuration>
            </plugin>
```

This does mean that we have to add the App name as parameter when we deploy our App.
```bash
tom@tom-ubuntu:~/Projects/stratospheric/cdk-example$ cdk deploy --app "./mvnw -e -q compile exec:java -Dex
ec.mainClass=com.myorg.CdkExampleApp2" \
--profile stratospheric \
-c accountId=<ACCOUNT_ID> \
-c region=eu-west-2 \
-c repoName=foo
```
We can also add the arguments to the cdk.json file:
```yaml
{
  "context": {
    "region": "eu-west-2",
    "accountId": "706054169063",
    "repoName": "my-cool-repo"
  }
}
```

This shortens our command:
```bash
cdk deploy --app "./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.CdkExampleApp2" --profile stratospheric
```

We can also create a package.json file to deploy and destroy our apps:
```bash
{
  "name": "hello-cdk",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "stack1:deploy": "cdk deploy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.CdkExampleApp \" --require-approval never --profile stratospheric",
    "stack1:destroy": "cdk destroy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.CdkExampleApp \" --require-approval never --profile stratospheric",
    "stack2:deploy": "cdk deploy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.CdkExampleApp2 \" --require-approval never --profile stratospheric",
    "stack2:destroy": "cdk destroy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.CdkExampleApp2 \" --require-approval never --profile stratospheric",
  },
  "devDependencies": {
    "aws-sdk": "2.43.1"
  },
  "engines": {
    "node": ">=16"
  }
}
```
We run npm install to initialize the npm project and can run:
```bash
npm run stack2:deploy
```
The parameters for the deploy are still kept in the cdk.json file:
```json
{
  "context": {
    "region": "eu-west-2",
    "accountId": "706054169063",
    "repoName": "my-cool-repo"
  }
}

```
We could override the values in the cdk.json file from the command line.

So far we have looked at debugging CloudFormation scripts by using the cdk.out file.
We have tried to stick to the rule of using one stack per app and we have used the cdk.json
and npm to save retyping scripts and parameters.

### Deploying a Network Stack with CDK
In this section we will build a CDK app to deploy a network. We will explore the Network construct and experiment with
sharing parameters between CDK constructs with SSM.
This is a cleaned up version of the DockerRepositoryApp:
```java
public class DockerRepositoryApp {

    public static void main(String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");
        
        Environment awsEnvironment = makeEnv(accountId, region);

        Stack dockerRepositoryStack = new Stack(app, "DockerRepositoryStack", StackProps.builder()
                .stackName(applicationName + "-DockerRepository")
                .env(awsEnvironment)
                .build()
        );

        DockerRepository dockerRepository = new DockerRepository(
                dockerRepositoryStack,
                "DockerRepository",
                awsEnvironment,
                new DockerRepository.DockerRepositoryInputParameters(applicationName, accountId)
        );

        app.synth();

    }

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}
```

We then create the NetworApp:
```java
public class NetworkApp {

    public static void main(String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        String environmentName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");


        Environment env = makeEnv(accountId, region);

        Stack networkStack = new Stack(app, "NetworkStack", StackProps.builder()
                .stackName(environmentName + "-Network")
                .env(env)
                .build()
        );

        Network network = new Network(
                networkStack,
                "Network",
                env,
                environmentName,
                new Network.NetworkInputParameters());

        app.synth();

    }

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}

```

We then add the NetworkApp to the package.json:
```json

{
  "name": "cdk-example",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "repository:deploy": "cdk deploy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.DockerRepositoryApp \" --require-approval never --profile stratospheric",
    "repository:destroy": "cdk destroy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.DockerRepositoryApp \" --require-approval never --profile stratospheric"
    "network:deploy": "cdk deploy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.NetworkApp \" --require-approval never --profile stratospheric",
    "network:destroy": "cdk destroy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=com.myorg.NetworkApp \" --require-approval never --profile stratospheric"
  },
  "devDependencies": {
    "aws-cdk": "2.70.0"
  },
  "engines": {
    "node": ">=16"
  }
}
```

Here we are using the Network Construct to create the network:
```java
public class Network extends Construct {
    public Network(Construct scope, String id, Environment environment, String environmentName, NetworkInputParameters networkInputParameters) {
        super(scope, id);
        this.environmentName = environmentName;
        this.vpc = this.createVpc(environmentName);
        this.ecsCluster = Builder.create(this, "cluster").vpc(this.vpc).clusterName(this.prefixWithEnvironmentName("ecsCluster")).build();
        this.createLoadBalancer(this.vpc, networkInputParameters.getSslCertificateArn());
        Tags.of(this).add("environment", environmentName);
    }

    private void createLoadBalancer(IVpc vpc, Optional<String> sslCertificateArn) {
        this.loadbalancerSecurityGroup = software.amazon.awscdk.services.ec2.SecurityGroup.Builder.create(this, "loadbalancerSecurityGroup").securityGroupName(this.prefixWithEnvironmentName("loadbalancerSecurityGroup")).description("Public access to the load balancer.").vpc(vpc).build();
        CfnSecurityGroupIngress ingressFromPublic = software.amazon.awscdk.services.ec2.CfnSecurityGroupIngress.Builder.create(this, "ingressToLoadbalancer").groupId(this.loadbalancerSecurityGroup.getSecurityGroupId()).cidrIp("0.0.0.0/0").ipProtocol("-1").build();
        this.loadBalancer = software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer.Builder.create(this, "loadbalancer").loadBalancerName(this.prefixWithEnvironmentName("loadbalancer")).vpc(vpc).internetFacing(true).securityGroup(this.loadbalancerSecurityGroup).build();
        IApplicationTargetGroup dummyTargetGroup = software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup.Builder.create(this, "defaultTargetGroup").vpc(vpc).port(8080).protocol(ApplicationProtocol.HTTP).targetGroupName(this.prefixWithEnvironmentName("no-op-targetGroup")).targetType(TargetType.IP).deregistrationDelay(Duration.seconds(5)).healthCheck(HealthCheck.builder().healthyThresholdCount(2).interval(Duration.seconds(10)).timeout(Duration.seconds(5)).build()).build();
        this.httpListener = this.loadBalancer.addListener("httpListener", BaseApplicationListenerProps.builder().port(80).protocol(ApplicationProtocol.HTTP).open(true).build());
        this.httpListener.addTargetGroups("http-defaultTargetGroup", AddApplicationTargetGroupsProps.builder().targetGroups(Collections.singletonList(dummyTargetGroup)).build());
        if (sslCertificateArn.isPresent()) {
            IListenerCertificate certificate = ListenerCertificate.fromArn((String)sslCertificateArn.get());
            this.httpsListener = this.loadBalancer.addListener("httpsListener", BaseApplicationListenerProps.builder().port(443).protocol(ApplicationProtocol.HTTPS).certificates(Collections.singletonList(certificate)).open(true).build());
            this.httpsListener.addTargetGroups("https-defaultTargetGroup", AddApplicationTargetGroupsProps.builder().targetGroups(Collections.singletonList(dummyTargetGroup)).build());
            ListenerAction redirectAction = ListenerAction.redirect(RedirectOptions.builder().protocol("HTTPS").port("443").build());
            new ApplicationListenerRule(this, "HttpListenerRule", ApplicationListenerRuleProps.builder().listener(this.httpListener).priority(1).conditions(List.of(ListenerCondition.pathPatterns(List.of("*")))).action(redirectAction).build());
        }

        this.createOutputParameters();
    }
}
```
This Construct creates a VPC, ECSCluster and Load Balancer for us. The VPC Construct creates the Internet Gateway and Routing Table for us also.
CDK abstracts the details for us. For each construct the Network construct creates StringParameters which are accessible through AWS Systems Manager.
![image](https://user-images.githubusercontent.com/27693622/228353250-2a0ebcd0-1684-4667-bc8b-1c20cfcf0f54.png)


We can deploy the stack with:
```bash
npm run network:deploy -- -c environmentName=staging
```

We can also deploy the stack for production:
```bash
npm run network:deploy -- -c environmentName=prod
```
We can look on CloudFormation to see all the Network resources that have been deployed for us:
![image](https://user-images.githubusercontent.com/27693622/228351453-a3eb005c-fd46-4e38-bb43-d0798098c373.png)

We can now see how Constructs can abstract a lot of the complexity of deploying a full stack application. Using the 
We have also seen how the Construct creates entries in AWS Systems Manager for the parameters that are related to the resources
we have deployed.

### Deploying a Service Stack with CDK
We will now use the Service Construct to create a service on AWS ECS.

![image](https://user-images.githubusercontent.com/27693622/228356531-a6d84f03-3bb7-4091-95a6-cdd53eade21b.png)

The above image describes the main parts of the service app.

We now create a Service Stack:
```java

public class ServiceApp {

    public static void main(String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        String environmentName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

        String dockerRepositoryName = (String) app.getNode().tryGetContext("dockerRepositoryName");
        Validations.requireNonEmpty(dockerRepositoryName, "context variable 'dockerRepositoryName' must not be null");


        String dockerImageTag = (String) app.getNode().tryGetContext("dockerImageTag");
        Validations.requireNonEmpty(dockerImageTag, "context variable 'dockerImageTag' must not be null");



        Environment env = makeEnv(accountId, region);

        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
                applicationName,
                environmentName);

        Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
                .stackName(environmentName + "-Service")
                .env(env)
                .build()
        );

        Network.NetworkOutputParameters networkOutputParameters = Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName());

        Service service = new Service(
                serviceStack,
                "Service",
                env,
                applicationEnvironment,
                new Service.ServiceInputParameters(
                        new Service.DockerImageSource(dockerRepositoryName, dockerImageTag),
                        new HashMap<>()),
                networkOutputParameters
        );

        app.synth();

    }

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
}

```

We are going to refer to the hello-world-app on ECR:
![image](https://user-images.githubusercontent.com/27693622/228363818-fcaa19d1-544d-40e1-891e-0e815b7a6e51.png)

We can now deploy our service with:
```bash
npm run service:deploy -- -c environmentName=staging
```

We can now see all the Constructs created as part of our deployment:
![image](https://user-images.githubusercontent.com/27693622/228364864-68681890-1c1a-4072-beda-e14ed35edf62.png)

We can view the logs of the deployment:
![image](https://user-images.githubusercontent.com/27693622/228365292-d41cd9e8-1277-4716-b93c-23b3957f86bb.png)

We can also see the app running on the loadbalancer associated with our target group:
![image](https://user-images.githubusercontent.com/27693622/228365967-e24adf7e-859f-48ad-83b1-46d46ea80160.png)

So, we have deployed a SpringBoot application to the cloud using CDK. The CDK and CloudFormation did a lot of the hard work by spinning up the
Application Load Balancer and EC2 instances. We have built loosely coupled stacks. The Constructs have protected us from a lot of the
complexity of building the resources.

### Continuous Deployment with CDK and Github Actions
We can now practice deploying our application to the cloud using Github Actions and build a CD pipeline.

# Hello Stratospheric
This is the link for our Continuous Deployment github actions example:
https://github.com/TomSpencerLondon/hello-stratospheric

A Hello World app to showcase a continuous deployment pipeline with Spring Boot, CDK, and AWS.

### Build app
```bash
./gradlew build
```

We have a Dockerfile for building the application:
```Dockerfile
FROM eclipse-temurin:17-jre

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

We can query our AWS credentials with:
```bash
aws sts get-caller-identity
```

### Continuous Deployment

![image](https://user-images.githubusercontent.com/27693622/228384793-5de2fcd2-a737-4d4a-8558-07b89e1be581.png)

In the first lane of the diagram above we have the main pipeline which runs tests and builds for a jar file. This is then converted to an image that is published to ECR.
The deploy step deploys the application using the Service App from our repository. The ECS Service requires a VPC and an ECR
Repository. We manually create teh Docker Repository and Network before creating the Service.  When we push to the git repository the app will be built and then published to the ECR repository. If the publish step is successful
the deploy step deploys the Docker image. The ECS Service requires a VPC and ECR Repository. The ECR Repository and the Network will not change
frequently so they are not included in the pipeline. We create the Docker Repository and Network manually.
In GitHub Actions we have a workflow which is a series of actions that are triggered for instance by a push to the main branch.
The Workflow contains one or more jobs and jobs contain one or more steps. Steps in a job always run in sequence.
We will start with a Bootstrap command pushing to the Docker repository.

We will have to enter the secrets in the github repository:
```yml
  AWS_ACCOUNT_ID: ${{ secrets.AWS_ACCOUNT_ID }}
  AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
  AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
  AWS_DEFAULT_REGION: ${{ secrets.AWS_REGION }}
```

We are keeping all the folders for AWS deployment with CDK in the CDK folder. The github workflows are kept in the .github folder.
As part of the manual section of the deploy we bootstrap the environment for the DockerRepository. Bootstrapping is the deployment of an
AWS CloudFormation template to a specific AWS environment (account and Region). The bootstrapping template accepts parameters that customize
some aspects of the bootstrapped resources. We can see the github actions on the Actions tab of our repository:
![image](https://user-images.githubusercontent.com/27693622/228491420-345ee44c-103a-414f-bbcc-2e7d40127ba9.png)

The instructions for the manual deploy are kept in 01-bootstrap.yml and 02-create-environment.yml. The build and deploy commands are kept in
03-build-and-deploy.yml. This runs every time there is a push to the main branch:
```yaml
on:
  push:
    branches:
      - main
```
We also have a concurrency entry on deploy:
```yaml
  deploy:
    runs-on: ubuntu-20.04
    name: Deploy Todo App (Demo)
    needs: build-and-publish
    timeout-minutes: 15
    if: github.ref == 'refs/heads/main'
    concurrency: hello-world-deployment
    steps:
```
This avoids conflicts if two people merge code in the same timeframe. This [doc](https://docs.github.com/en/actions/using-jobs/using-concurrency) is
useful on concurrency. We can now see the staging-Service on CloudFormation:
![image](https://user-images.githubusercontent.com/27693622/228494858-b363b897-5dcb-4484-89e2-0b9bc03fdd43.png)

If we click on the Load Balancer url in Resources we can see our app is up and running:
![image](https://user-images.githubusercontent.com/27693622/228495152-34b45823-bf30-4bd4-9743-708bd0cb524c.png)

We have now wrapped the cdk commands in a GitHub workflow. We had two forms of workflow: manual triggers and on push.

### Spring Boot & AWS
We are going to build an application with Spring Boot and deploy it to AWS Cloud.
This application includes:
- Registration and login
- CRUD: Viewing, adding and deleting Todos
- Sharing Todos
- Email notifications
- Push notifications

![image](https://user-images.githubusercontent.com/27693622/228534282-5d963e84-e43c-41a3-868a-6100dc20c131.png)

The above architecture describes the features we will implement. We interact with a REST API for showing thymeleaf pages.
We use an RDS database on RDS and a DynamoDB database for storage. We will decouple sending emails with SQS. We will allow
push notifications for collaboration with Amazon MQ with a websocket broker relay.


### Local Development
We will learn about the challenges of local cloud development.
We will also learn about LocalStack and Drop-in replacements for Amazon RDS and Amazon Cognito.
The goal is to make local development as easy as possible. LocalStack is a fully functional
AWS Cloud stack:
https://localstack.cloud/

We will use the localstack docker image:
```bash
docker run -p 4566:4566 -e SERVICES=s3 localstack/localstack
```
To use localstack we can use the following command:
```bash
aws s3api create-bucket --bucket toms-s3-bucket --endpoint-url http://localhost:4566 --create-bucket-configuration LocationConstraint=eu-west-1
{
    "Location": "http://toms-s3-bucket.s3.localhost.localstack.cloud:4566/"
}

```

With local development, we now need to connect to the LocalStack instance:
```yaml
  cloud:
    aws:
      rds:
        enabled: false
      sqs:
        endpoint: http://localhost:4566
        region: eu-central-1
      mail:
        endpoint: http://localhost:4566
        region: eu-central-1
      credentials:
        secret-key: foo
        access-key: bar
```
We also hard code our credentials above. We then create our resources on LocalStack. We will use docker-compose.yml for this.
We use local-aws-infrastructure.sh to create our services:
```bash
awslocal sqs create-queue --queue-name stratospheric-todo-sharing
```

We use a local docker image for postgres to replace AWS RDS. We will also use KeyCloak to replace Amazon Cognito:
```yaml
  keycloak:
    image: quay.io/keycloak/keycloak:18.0.0-legacy
    ports:
      - 8888:8080
    environment:
      - KEYCLOAK_USER=keycloak
      - KEYCLOAK_PASSWORD=keycloak
      - DB_VENDOR=h2
      - JAVA_OPTS=-Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=/tmp/stratospheric-realm.json
    volumes:
      - ./src/test/resources/keycloak/stratospheric-realm.json:/tmp/stratospheric-realm.json
```

We will use docker compose before starting our application:
```bash
docker-compose up
```

We now have two properties application-dev and application-aws for local development.
We have learnt about the approaches for local cloud development, using the LocalStack and Docker and we have replaced
RDS and Cognito with a local postgres instance and KeyCloak.

### Todo App Design
![image](https://user-images.githubusercontent.com/27693622/228904750-5a3fe117-0e2b-4324-b33b-3165c86225f2.png)


### Building User Registration and Login with Amazon Cognito
We will now learn how to add User Registration and Login using AWS Cognito. We will first learn about Amazon Cognito.
We will then look at OAuth 2.0 and OpenID Connect and look at the OAuth Authorization Code Grant Flow.
Amazon Cognito is a managed service that provides authentication, authorization and user management for applications.
This allows us to delegate user management to AWS.

#### OAuth 2.0
Open Authorization is the industry standard for authentication and authorization. It provides a protocol for login for applications.
When we sign up we are asked to grant access to the site for specific tasks. We are redirected to another site to grant permissions.
Once we have granted permission the application has access to data shared by our identifying website. We will now learn about
Resource Owners (end users who own resources in a third party application), Resource Sever (provides protected resources),
Client for accessing resources and Authorization Server (a server dedicated to authorization). There are different Grant Types which
might be Authorization Codes, Client Codes and Refresh Tokens.

#### OpenID Connect 1.0
OpenID connect is a protocol for identity authentication. With OpenID Connect the end user entity becomes the protected resource.
This authentication mechanism is most commonly used with the Authorization Code grant type. When we see login with Google
the application is most commonly using OpenID Connect.

#### OAuth Authorization Code Grant Flow
![image](https://user-images.githubusercontent.com/27693622/228911198-bf7f9126-a966-4803-aafa-8c8edc32ad4b.png)

Above we describe user management with OAuth2. Here the resource owner asks to access the application.
The application requests authorization for Github repos from the user. The user authenticates at Github and grants authorization.
The Authorization server then sends an authorization code. The application then exchanges the authorization code for an access token
which is then returned by the authorization server. The application then requests the protected resources (in this case Github repos) with
the access token. If the access token is correct then the resource server returns information about the resource.

Here we have learnt about OAuth and OpenID Connect and the Authentication and Authorization Flows. We will now learn about
Amazon Cognito.

#### Amazon Cognito
We will now learn about creating AWS Cognito resources with CDK. We will now look at some of the terms that Amazon Cognito uses.
A User Pool stores and manages User information. The User Pool App Client runs operations on a User Pool.
The Identity Pool works with IAM roles. For our application, we will create a single user pool to store all our users.
In the User Pool we will configure our password policy, define required and optional user attributes, enable password recovery and customize email notifications.
We will register our application as a User Pool app client to enable user login with OIDC and OAuth 2.0.
This is the setup for the CognitoApp:
```java
public class CognitoApp {

  public static void main(final String[] args) {
    App app = new App();

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    String applicationUrl = (String) app.getNode().tryGetContext("applicationUrl");
    Validations.requireNonEmpty(applicationUrl, "context variable 'applicationUrl' must not be null");

    String loginPageDomainPrefix = (String) app.getNode().tryGetContext("loginPageDomainPrefix");
    Validations.requireNonEmpty(loginPageDomainPrefix, "context variable 'loginPageDomainPrefix' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    new CognitoStack(app, "cognito", awsEnvironment, applicationEnvironment, new CognitoStack.CognitoInputParameters(
      applicationName,
      applicationUrl,
      loginPageDomainPrefix));

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }

}

```
Here we add the domain name of our application with the following:
```java

public class CognitoApp {

    public static void main(final String[] args) {
        App app = new App();
        // ...
        String applicationUrl = (String) app.getNode().tryGetContext("applicationUrl");
        Validations.requireNonEmpty(applicationUrl, "context variable 'applicationUrl' must not be null");
        
        String loginPageDomainPrefix = (String) app.getNode().tryGetContext("loginPageDomainPrefix");
        Validations.requireNonEmpty(loginPageDomainPrefix, "context variable 'loginPageDomainPrefix' must not be null");
    }
}
```
The application url parameter is the final url of our application. Stack sets up the application:
```java
class CognitoStack extends Stack {

    private final ApplicationEnvironment applicationEnvironment;

    private final UserPool userPool;
    private final UserPoolClient userPoolClient;
    private final UserPoolDomain userPoolDomain;
    private String userPoolClientSecret;
    private final String logoutUrl;

    public CognitoStack(
            final Construct scope,
            final String id,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final CognitoInputParameters inputParameters) {
        super(scope, id, StackProps.builder()
                .stackName(applicationEnvironment.prefix("Cognito"))
                .env(awsEnvironment).build());

        this.applicationEnvironment = applicationEnvironment;
        this.logoutUrl = String.format("https://%s.auth.%s.amazoncognito.com/logout", inputParameters.loginPageDomainPrefix, awsEnvironment.getRegion());

        this.userPool = UserPool.Builder.create(this, "userPool")
                .userPoolName(inputParameters.applicationName + "-user-pool")
                .selfSignUpEnabled(false)
                .accountRecovery(AccountRecovery.EMAIL_ONLY)
                .autoVerify(AutoVerifiedAttrs.builder().email(true).build())
                .signInAliases(SignInAliases.builder().username(true).email(true).build())
                .signInCaseSensitive(true)
                .standardAttributes(StandardAttributes.builder()
                        .email(StandardAttribute.builder().required(true).mutable(false).build())
                        .build())
                .mfa(Mfa.OFF)
                .passwordPolicy(PasswordPolicy.builder()
                        .requireLowercase(true)
                        .requireDigits(true)
                        .requireSymbols(true)
                        .requireUppercase(true)
                        .minLength(12)
                        .tempPasswordValidity(Duration.days(7))
                        .build())
                .build();

        this.userPoolClient = UserPoolClient.Builder.create(this, "userPoolClient")
                .userPoolClientName(inputParameters.applicationName + "-client")
                .generateSecret(true)
                .userPool(this.userPool)
                .oAuth(OAuthSettings.builder()
                        .callbackUrls(Arrays.asList(
                                String.format("%s/login/oauth2/code/cognito", inputParameters.applicationUrl),
                                "http://localhost:8080/login/oauth2/code/cognito"
                        ))
                        .logoutUrls(Arrays.asList(inputParameters.applicationUrl, "http://localhost:8080"))
                        .flows(OAuthFlows.builder()
                                .authorizationCodeGrant(true)
                                .build())
                        .scopes(Arrays.asList(OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.PROFILE))
                        .build())
                .supportedIdentityProviders(Collections.singletonList(UserPoolClientIdentityProvider.COGNITO))
                .build();

        this.userPoolDomain = UserPoolDomain.Builder.create(this, "userPoolDomain")
                .userPool(this.userPool)
                .cognitoDomain(CognitoDomainOptions.builder()
                        .domainPrefix(inputParameters.loginPageDomainPrefix)
                        .build())
                .build();

        createOutputParameters();

        applicationEnvironment.tag(this);
    }
}
```
Here we have avoided using MFA and set a password policy for the user. We also set the configuration for the User Pool Client.
We also add a list of UserPoolClientIdentityProviders. We also create output parameters for the Spring application.

We have learnt about Cognito Resources and Infrastructure. This is a useful link for Amazon Cognito:
https://aws.amazon.com/cognito/
This is a useful link for more information about OAuth:
https://oauth.net/2/

#### Connecting to Database with Amazon RDS
In this section we will learn how to work with AWS RDS.
We will learn about RDS and also develop the infrastructure we will deploy.
AWS RDS offers supports PostgreSQL, MySQL, MariaDB, Oracle, Microsoft and Aurora.
It allows us to manage relational databases with tools such as AWS CLI, IAM, CloudFormation and CDK.

![image](https://user-images.githubusercontent.com/27693622/228945157-2b1a86e9-a467-4f64-82f3-414277c535d7.png)

Above is a diagram of the database setup. We put a database instance into a private subnet. The application in our
Service stack would connect to the RDS database. The ECS Task represents a Docker image and the ECS Service wraps
several Docker images into a Service. The overall infrastructure runs on a VPC. We will use an Application Load Balancer
and Internet Gateway to enable access for the internet.

Here we have learnt about working with relational databases on AWS. In the next section we will learn about configuring IAM permissions
for RDS access. We will deploy a database related infrastructure and use the RDS database from our SpringBoot application.

#### Connecting to the Database with AWS RDS

Here we will look at setting up the required IAM permissions. We will then deploy RDS with CDK and then configure the RDS
databaes with our SpringBoot application.

#### Setting up the requisite IAM permissions

We need to add the RDSFullAccess policy for our developer user account:
![image](https://user-images.githubusercontent.com/27693622/228948472-55654f36-5c05-4ac1-a97f-1a5818c9be8e.png)

The application will have access to the database through the CDK app. We will deploy a database infrastructure with CDK and use the
Postgres Database Construct. The

```java
public class PostgresDatabase extends Construct {
    public PostgresDatabase(
            final Construct scope,
            final String id,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final DatabaseInputParameters databaseInputParameters) {

        super(scope, id);

        this.applicationEnvironment = applicationEnvironment;

        // Sadly, we cannot use VPC.fromLookup() to resolve a VPC object from this VpcId, because it's broken
        // (https://github.com/aws/aws-cdk/issues/3600). So, we have to resolve all properties we need from the VPC
        // via SSM parameter store.
        Network.NetworkOutputParameters networkOutputParameters = Network.getOutputParametersFromParameterStore(this, applicationEnvironment.getEnvironmentName());

        String username = sanitizeDbParameterName(applicationEnvironment.prefix("dbUser"));

        databaseSecurityGroup = CfnSecurityGroup.Builder.create(this, "databaseSecurityGroup")
                .vpcId(networkOutputParameters.getVpcId())
                .groupDescription("Security Group for the database instance")
                .groupName(applicationEnvironment.prefix("dbSecurityGroup"))
                .build();

        // This will generate a JSON object with the keys "username" and "password".
        databaseSecret = Secret.Builder.create(this, "databaseSecret")
                .secretName(applicationEnvironment.prefix("DatabaseSecret"))
                .description("Credentials to the RDS instance")
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate(String.format("{\"username\": \"%s\"}", username))
                        .generateStringKey("password")
                        .passwordLength(32)
                        .excludeCharacters("@/\\\" ")
                        .build())
                .build();

        CfnDBSubnetGroup subnetGroup = CfnDBSubnetGroup.Builder.create(this, "dbSubnetGroup")
                .dbSubnetGroupDescription("Subnet group for the RDS instance")
                .dbSubnetGroupName(applicationEnvironment.prefix("dbSubnetGroup"))
                .subnetIds(networkOutputParameters.getIsolatedSubnets())
                .build();

        dbInstance = CfnDBInstance.Builder.create(this, "postgresInstance")
                .dbInstanceIdentifier(applicationEnvironment.prefix("database"))
                .allocatedStorage(String.valueOf(databaseInputParameters.storageInGb))
                .availabilityZone(networkOutputParameters.getAvailabilityZones().get(0))
                .dbInstanceClass(databaseInputParameters.instanceClass)
                .dbName(sanitizeDbParameterName(applicationEnvironment.prefix("database")))
                .dbSubnetGroupName(subnetGroup.getDbSubnetGroupName())
                .engine("postgres")
                .engineVersion(databaseInputParameters.postgresVersion)
                .masterUsername(username)
                .masterUserPassword(databaseSecret.secretValueFromJson("password").toString())
                .publiclyAccessible(false)
                .vpcSecurityGroups(Collections.singletonList(databaseSecurityGroup.getAttrGroupId()))
                .build();

        CfnSecretTargetAttachment.Builder.create(this, "secretTargetAttachment")
                .secretId(databaseSecret.getSecretArn())
                .targetId(dbInstance.getRef())
                .targetType("AWS::RDS::DBInstance")
                .build();

        createOutputParameters();

        applicationEnvironment.tag(this);

    }
}

```
First the construct creates a database security group. We also define subnets which will be used by the database instance. We also create a database secret
and then create the Postgres Database. We can then deploy and destroy the application with our package.json script:
```json
{
  "scripts": {
    "database:deploy": "cdk deploy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=dev.stratospheric.todoapp.cdk.DatabaseApp\" --require-approval never",
    "database:destroy": "cdk destroy --app \"./mvnw -e -q compile exec:java -Dexec.mainClass=dev.stratospheric.todoapp.cdk.DatabaseApp\" --force --require-approval never"
  }
}
```

We run the command and create and instantiate the database and server that we will use.

```bash
npm run database:deploy
```

AWS will instantiate the Postgres server and the other resources we need. The above command runs the cdk deploy.
If we want to use a different profile we can use:
```bash
npm run database:deploy -- --profile stratospheric
```
Here the double dash tells npm not to evaluate what is after it so the --profile is sent on to the aws script.

We now have a database running so we can now setup our SpringBoot application. We do this with the ServiceApp inside the CDK folder:
```java

public class ServiceApp {
    public static void main(final String[] args) {
        App app = new App();

        String environmentName = (String) app.getNode().tryGetContext("environmentName");
        Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

        String applicationName = (String) app.getNode().tryGetContext("applicationName");
        Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

        String accountId = (String) app.getNode().tryGetContext("accountId");
        Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String springProfile = (String) app.getNode().tryGetContext("springProfile");
        Validations.requireNonEmpty(springProfile, "context variable 'springProfile' must not be null");

        String dockerRepositoryName = (String) app.getNode().tryGetContext("dockerRepositoryName");
        Validations.requireNonEmpty(dockerRepositoryName, "context variable 'dockerRepositoryName' must not be null");

        String dockerImageTag = (String) app.getNode().tryGetContext("dockerImageTag");
        Validations.requireNonEmpty(dockerImageTag, "context variable 'dockerImageTag' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        Validations.requireNonEmpty(region, "context variable 'region' must not be null");

        Environment awsEnvironment = makeEnv(accountId, region);

        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
                applicationName,
                environmentName
        );

        // This stack is just a container for the parameters below, because they need a Stack as a scope.
        // We're making this parameters stack unique with each deployment by adding a timestamp, because updating an existing
        // parameters stack will fail because the parameters may be used by an old service stack.
        // This means that each update will generate a new parameters stack that needs to be cleaned up manually!
        long timestamp = System.currentTimeMillis();
        Stack parametersStack = new Stack(app, "ServiceParameters-" + timestamp, StackProps.builder()
                .stackName(applicationEnvironment.prefix("Service-Parameters-" + timestamp))
                .env(awsEnvironment)
                .build());

        Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
                .stackName(applicationEnvironment.prefix("Service"))
                .env(awsEnvironment)
                .build());

        PostgresDatabase.DatabaseOutputParameters databaseOutputParameters =
                PostgresDatabase.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

        CognitoStack.CognitoOutputParameters cognitoOutputParameters =
                CognitoStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

        MessagingStack.MessagingOutputParameters messagingOutputParameters =
                MessagingStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

        ActiveMqStack.ActiveMqOutputParameters activeMqOutputParameters =
                ActiveMqStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

        List<String> securityGroupIdsToGrantIngressFromEcs = Arrays.asList(
                databaseOutputParameters.getDatabaseSecurityGroupId(),
                activeMqOutputParameters.getActiveMqSecurityGroupId()
        );

        new Service(
                serviceStack,
                "Service",
                awsEnvironment,
                applicationEnvironment,
                new Service.ServiceInputParameters(
                        new Service.DockerImageSource(dockerRepositoryName, dockerImageTag),
                        securityGroupIdsToGrantIngressFromEcs,
                        environmentVariables(
                                serviceStack,
                                databaseOutputParameters,
                                cognitoOutputParameters,
                                messagingOutputParameters,
                                activeMqOutputParameters,
                                springProfile,
                                environmentName))
                        .withTaskRolePolicyStatements(List.of(
                                PolicyStatement.Builder.create()
                                        .sid("AllowSQSAccess")
                                        .effect(Effect.ALLOW)
                                        .resources(List.of(
                                                String.format("arn:aws:sqs:%s:%s:%s", region, accountId, messagingOutputParameters.getTodoSharingQueueName())
                                        ))
                                        .actions(Arrays.asList(
                                                "sqs:DeleteMessage",
                                                "sqs:GetQueueUrl",
                                                "sqs:ListDeadLetterSourceQueues",
                                                "sqs:ListQueues",
                                                "sqs:ListQueueTags",
                                                "sqs:ReceiveMessage",
                                                "sqs:SendMessage",
                                                "sqs:ChangeMessageVisibility",
                                                "sqs:GetQueueAttributes"))
                                        .build(),
                                PolicyStatement.Builder.create()
                                        .sid("AllowCreatingUsers")
                                        .effect(Effect.ALLOW)
                                        .resources(
                                                List.of(String.format("arn:aws:cognito-idp:%s:%s:userpool/%s", region, accountId, cognitoOutputParameters.getUserPoolId()))
                                        )
                                        .actions(List.of(
                                                "cognito-idp:AdminCreateUser"
                                        ))
                                        .build(),
                                PolicyStatement.Builder.create()
                                        .sid("AllowSendingEmails")
                                        .effect(Effect.ALLOW)
                                        .resources(
                                                List.of(String.format("arn:aws:ses:%s:%s:identity/stratospheric.dev", region, accountId))
                                        )
                                        .actions(List.of(
                                                "ses:SendEmail",
                                                "ses:SendRawEmail"
                                        ))
                                        .build(),
                                PolicyStatement.Builder.create()
                                        .sid("AllowDynamoTableAccess")
                                        .effect(Effect.ALLOW)
                                        .resources(
                                                List.of(String.format("arn:aws:dynamodb:%s:%s:table/%s", region, accountId, applicationEnvironment.prefix("breadcrumb")))
                                        )
                                        .actions(List.of(
                                                "dynamodb:Scan",
                                                "dynamodb:Query",
                                                "dynamodb:PutItem",
                                                "dynamodb:GetItem",
                                                "dynamodb:BatchWriteItem",
                                                "dynamodb:BatchWriteGet"
                                        ))
                                        .build(),
                                PolicyStatement.Builder.create()
                                        .sid("AllowSendingMetricsToCloudWatch")
                                        .effect(Effect.ALLOW)
                                        .resources(singletonList("*")) // CloudWatch does not have any resource-level permissions, see https://stackoverflow.com/a/38055068/9085273
                                        .actions(singletonList("cloudwatch:PutMetricData"))
                                        .build()
                        ))
                        .withStickySessionsEnabled(true)
                        .withHealthCheckPath("/actuator/health")
                        .withAwsLogsDateTimeFormat("%Y-%m-%dT%H:%M:%S.%f%z")
                        .withHealthCheckIntervalSeconds(30), // needs to be long enough to allow for slow start up with low-end computing instances

                Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName()));

        app.synth();
    }
    
    static Map<String, String> environmentVariables(
            Construct scope,
            PostgresDatabase.DatabaseOutputParameters databaseOutputParameters,
            CognitoStack.CognitoOutputParameters cognitoOutputParameters,
            MessagingStack.MessagingOutputParameters messagingOutputParameters,
            ActiveMqStack.ActiveMqOutputParameters activeMqOutputParameters,
            String springProfile,
            String environmentName
    ) {
        Map<String, String> vars = new HashMap<>();

        String databaseSecretArn = databaseOutputParameters.getDatabaseSecretArn();
        ISecret databaseSecret = Secret.fromSecretCompleteArn(scope, "databaseSecret", databaseSecretArn);

        vars.put("SPRING_PROFILES_ACTIVE", springProfile);
        vars.put("SPRING_DATASOURCE_URL",
                String.format("jdbc:postgresql://%s:%s/%s",
                        databaseOutputParameters.getEndpointAddress(),
                        databaseOutputParameters.getEndpointPort(),
                        databaseOutputParameters.getDbName()));
        vars.put("SPRING_DATASOURCE_USERNAME",
                databaseSecret.secretValueFromJson("username").toString());
        vars.put("SPRING_DATASOURCE_PASSWORD",
                databaseSecret.secretValueFromJson("password").toString());
        vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.getUserPoolClientId());
        vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.getUserPoolClientSecret());
        vars.put("COGNITO_USER_POOL_ID", cognitoOutputParameters.getUserPoolId());
        vars.put("COGNITO_LOGOUT_URL", cognitoOutputParameters.getLogoutUrl());
        vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.getProviderUrl());
        vars.put("TODO_SHARING_QUEUE_NAME", messagingOutputParameters.getTodoSharingQueueName());
        vars.put("WEB_SOCKET_RELAY_ENDPOINT", activeMqOutputParameters.getStompEndpoint());
        vars.put("WEB_SOCKET_RELAY_USERNAME", activeMqOutputParameters.getActiveMqUsername());
        vars.put("WEB_SOCKET_RELAY_PASSWORD", activeMqOutputParameters.getActiveMqPassword());
        vars.put("ENVIRONMENT_NAME", environmentName);

        return vars;
    }

}

```

Here we create the SpringBoot Service with the correct environment variables for configuring the database connection.
The AWS specific database stack can then be used with the Spring application. Spring uses the environment variables to
configure the database connection on SpringBoot start.

We have seen how to configure IAM permissions for RDS access. We have deployed the database and are now using the RDS database from
our Spring application.

### Sharing Todos with Amazon SQS and Amazon SES
Here we will integrate two new AWS services, AWS Simple Queue Service ([SQS](https://aws.amazon.com/sqs/)) and
AWS Simple Email Service ([SES](https://aws.amazon.com/ses/)) so that we can share Todos with other users so that they
can collaborate. Users accept collaboration by email and then collaborate. When users share their todos we put the request
into an SQS queue and then send out emails to the requested other user.

#### Introduction to Amazon SQS
Amazon SQS is a fully managed messaging service for queueing messages to different parts of our application. SQS can
also decouple components in a microservices distributed architecture. We interact with SQS via an https API.
SQS can persist our messages for up to 14 days. The standard queue type delivers on a best effort ordering. FIFO SQS
guarantees messages are sent in the same order as they are sent. Each message remains on the queue until the consumer
acknowledges its delivery by deleting the message. We set up our messaging queue with the MessagingStack:
```java
class MessagingStack extends Stack {

  private final ApplicationEnvironment applicationEnvironment;
  private final IQueue todoSharingQueue;
  private final IQueue todoSharingDlq;

  public MessagingStack(
    final Construct scope,
    final String id,
    final Environment awsEnvironment,
    final ApplicationEnvironment applicationEnvironment) {
    super(scope, id, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Messaging"))
      .env(awsEnvironment).build());

    this.applicationEnvironment = applicationEnvironment;

    this.todoSharingDlq = Queue.Builder.create(this, "todoSharingDlq")
      .queueName(applicationEnvironment.prefix("todo-sharing-dead-letter-queue"))
      .retentionPeriod(Duration.days(14))
      .build();

    this.todoSharingQueue = Queue.Builder.create(this, "todoSharingQueue")
      .queueName(applicationEnvironment.prefix("todo-sharing-queue"))
      .visibilityTimeout(Duration.seconds(30))
      .retentionPeriod(Duration.days(14))
      .deadLetterQueue(DeadLetterQueue.builder()
        .queue(todoSharingDlq)
        .maxReceiveCount(3)
        .build())
      .build();

    createOutputParameters();

    applicationEnvironment.tag(this);
  }

  private static final String PARAMETER_TODO_SHARING_QUEUE_NAME = "todoSharingQueueName";

  private void createOutputParameters() {
    StringParameter.Builder.create(this, PARAMETER_TODO_SHARING_QUEUE_NAME)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_TODO_SHARING_QUEUE_NAME))
      .stringValue(this.todoSharingQueue.getQueueName())
      .build();
  }

  private static String createParameterName(ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName() + "-" + applicationEnvironment.getApplicationName() + "-Messaging-" + parameterName;
  }

  public static String getTodoSharingQueueName(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_TODO_SHARING_QUEUE_NAME, createParameterName(applicationEnvironment, PARAMETER_TODO_SHARING_QUEUE_NAME))
      .getStringValue();
  }

  public static MessagingOutputParameters getOutputParametersFromParameterStore(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new MessagingOutputParameters(
      getTodoSharingQueueName(scope, applicationEnvironment)
    );
  }

  public static class MessagingOutputParameters {
    private final String todoSharingQueueName;

    public MessagingOutputParameters(String todoSharingQueueName) {
      this.todoSharingQueueName = todoSharingQueueName;
    }

    public String getTodoSharingQueueName() {
      return todoSharingQueueName;
    }
  }

}

```
Here we set up the queue and set the Deadletter queue to accept messages after four attempts. We also need to set up
SQS as a dependency in our build.gradle:
```gradle
  implementation 'io.awspring.cloud:spring-cloud-aws-starter-sqs'
```
We then send the todo with a form on our dashboard:
```html

<div class="dropdown-menu" aria-labelledby="dropdownMenuLink">
              <span class="dropdown-item" th:if="${collaborators.isEmpty()}">
                No collaborator available
              </span>
    <form th:method="POST"
          th:each="collaborator : ${collaborators}"
          th:action="@{/todo/{todoId}/collaborations/{collaboratorId}(todoId=${todo.id}, collaboratorId=${collaborator.id})}">
        <button
                th:text="${collaborator.name}"
                type="submit"
                name="submit"
                class="dropdown-item">
        </button>
    </form>
</div>
```

We also have a post endpoint on our controller to share todos:

```java
@Controller
@RequestMapping("/todo")
public class TodoCollaborationController {

    private final TodoCollaborationService todoCollaborationService;

    public TodoCollaborationController(TodoCollaborationService todoCollaborationService) {
        this.todoCollaborationService = todoCollaborationService;
    }

    @Timed(
            value = "stratospheric.collaboration.sharing",
            description = "Measure the time how long it takes to share a todo"
    )
    @PostMapping("/{todoId}/collaborations/{collaboratorId}")
    public String shareTodoWithCollaborator(
            @PathVariable("todoId") Long todoId,
            @PathVariable("collaboratorId") Long collaboratorId,
            @AuthenticationPrincipal OidcUser user,
            RedirectAttributes redirectAttributes
    ) throws JsonProcessingException {
        String collaboratorName = todoCollaborationService.shareWithCollaborator(user.getEmail(), todoId, collaboratorId);

        redirectAttributes.addFlashAttribute("message",
                String.format("You successfully shared your todo with the user %s. " +
                        "Once the user accepts the invite, you'll see them as a collaborator on your todo.", collaboratorName));
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/dashboard";
    }
}
```
The TodoCollaboration controller takes collaboration requests and sharings them using the todoCollaborationService.
The Service then shares the request:
```java
@Service
@Transactional
public class TodoCollaborationService {

    private final TodoRepository todoRepository;
    private final PersonRepository personRepository;
    private final TodoCollaborationRequestRepository todoCollaborationRequestRepository;

    private final SqsTemplate sqsTemplate;
    private final String todoSharingQueueName;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(TodoCollaborationService.class.getName());

    private static final String INVALID_TODO_ID = "Invalid todo ID: ";
    private static final String INVALID_PERSON_ID = "Invalid person ID: ";
    private static final String INVALID_PERSON_EMAIL = "Invalid person Email: ";

    public TodoCollaborationService(
            @Value("${custom.sharing-queue}") String todoSharingQueueName,
            TodoRepository todoRepository,
            PersonRepository personRepository,
            TodoCollaborationRequestRepository todoCollaborationRequestRepository,
            SqsTemplate sqsTemplate,
            SimpMessagingTemplate simpMessagingTemplate) {
        this.todoRepository = todoRepository;
        this.personRepository = personRepository;
        this.todoCollaborationRequestRepository = todoCollaborationRequestRepository;
        this.sqsTemplate = sqsTemplate;
        this.todoSharingQueueName = todoSharingQueueName;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public String shareWithCollaborator(String todoOwnerEmail, Long todoId, Long collaboratorId) {

        Todo todo = todoRepository
                .findByIdAndOwnerEmail(todoId, todoOwnerEmail)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TODO_ID + todoId));

        Person collaborator = personRepository
                .findById(collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_ID + collaboratorId));

        if (todoCollaborationRequestRepository.findByTodoAndCollaborator(todo, collaborator) != null) {
            LOG.info("Collaboration request for todo {} with collaborator {} already exists", todoId, collaboratorId);
            return collaborator.getName();
        }

        LOG.info("About to share todo with id {} with collaborator {}", todoId, collaboratorId);

        TodoCollaborationRequest collaboration = new TodoCollaborationRequest();
        String token = UUID.randomUUID().toString();
        collaboration.setToken(token);
        collaboration.setCollaborator(collaborator);
        collaboration.setTodo(todo);
        todo.getCollaborationRequests().add(collaboration);

        todoCollaborationRequestRepository.save(collaboration);

        sqsTemplate.send(todoSharingQueueName, new TodoCollaborationNotification(collaboration));

        return collaborator.getName();
    }
}
```

We then listen for the request with our TodoSharingListener:
```java

@Component
public class TodoSharingListener {

  private final MailSender mailSender;
  private final TodoCollaborationService todoCollaborationService;
  private final boolean autoConfirmCollaborations;
  private final String confirmEmailFromAddress;
  private final String externalUrl;

  private static final Logger LOG = LoggerFactory.getLogger(TodoSharingListener.class.getName());

  public TodoSharingListener(
    MailSender mailSender,
    TodoCollaborationService todoCollaborationService,
    @Value("${custom.auto-confirm-collaborations}") boolean autoConfirmCollaborations,
    @Value("${custom.confirm-email-from-address}") String confirmEmailFromAddress,
    @Value("${custom.external-url}") String externalUrl) {
    this.mailSender = mailSender;
    this.todoCollaborationService = todoCollaborationService;
    this.autoConfirmCollaborations = autoConfirmCollaborations;
    this.confirmEmailFromAddress = confirmEmailFromAddress;
    this.externalUrl = externalUrl;
  }

  @SqsListener(value = "${custom.sharing-queue}")
  public void listenToSharingMessages(TodoCollaborationNotification payload) throws InterruptedException {
    LOG.info("Incoming todo sharing payload: {}", payload);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(confirmEmailFromAddress);
    message.setTo(payload.getCollaboratorEmail());
    message.setSubject("A todo was shared with you");
    message.setText(
      String.format(
        """
          Hi %s,\s

          someone shared a Todo from %s with you.

          Information about the shared Todo item:\s

          Title: %s\s
          Description: %s\s
          Priority: %s\s

          You can accept the collaboration by clicking this link: %s/todo/%s/collaborations/%s/confirm?token=%s\s

          Kind regards,\s
          Stratospheric""",
        payload.getCollaboratorEmail(),
        externalUrl,
        payload.getTodoTitle(),
        payload.getTodoDescription(),
        payload.getTodoPriority(),
        externalUrl,
        payload.getTodoId(),
        payload.getCollaboratorId(),
        payload.getToken()
      )
    );
    mailSender.send(message);

    LOG.info("Successfully informed collaborator about shared todo.");

    if (autoConfirmCollaborations) {
      LOG.info("Auto-confirmed collaboration request for todo: {}", payload.getTodoId());
      Thread.sleep(2_500);
      todoCollaborationService.confirmCollaboration(payload.getCollaboratorEmail(), payload.getTodoId(), payload.getCollaboratorId(), payload.getToken());
    }
  }
}

```

Here we have learnt about implementing collaboration features with SQS for decoupling events from behaviour.

### Sharing Todos with Amazon SQS and Amazon SES

Here, we will use Amazon SES for sending emails to collaborators. Amazon SES is an easy to set up email service.
We interact with the SES service with CDK or an API. We could use this service for marketing, sign up or email news services.
Amazon SES is available in several regions. We allow sending emails with the following configuration:
```java
public class ServiceApp {

    public static void main(final String[] args) {
        // ...
        PolicyStatement.Builder.create()
                .sid("AllowSendingEmails")
                .effect(Effect.ALLOW)
                .resources(
                        List.of(String.format("arn:aws:ses:%s:%s:identity/stratospheric.dev", region, accountId))
                )
                .actions(List.of(
                        "ses:SendEmail",
                        "ses:SendRawEmail"
                ))
                .build();
        // ...
    }
}
```
Spring defines two interfaces for sending emails: MailSender and JavaMailSender. We also add the ses dependency to our
build.gradle file:
```gradle
  implementation 'io.awspring.cloud:spring-cloud-aws-starter-ses'
```

We send the email from our TodoSharingListener:
```java
@Component
public class TodoSharingListener {
    // ...
  @SqsListener(value = "${custom.sharing-queue}")
  public void listenToSharingMessages(TodoCollaborationNotification payload) throws InterruptedException {
    LOG.info("Incoming todo sharing payload: {}", payload);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(confirmEmailFromAddress);
    message.setTo(payload.getCollaboratorEmail());
    message.setSubject("A todo was shared with you");
    message.setText(
      String.format(
        """
          Hi %s,\s

          someone shared a Todo from %s with you.

          Information about the shared Todo item:\s

          Title: %s\s
          Description: %s\s
          Priority: %s\s

          You can accept the collaboration by clicking this link: %s/todo/%s/collaborations/%s/confirm?token=%s\s

          Kind regards,\s
          Stratospheric""",
        payload.getCollaboratorEmail(),
        externalUrl,
        payload.getTodoTitle(),
        payload.getTodoDescription(),
        payload.getTodoPriority(),
        externalUrl,
        payload.getTodoId(),
        payload.getCollaboratorId(),
        payload.getToken()
      )
    );
    mailSender.send(message);

    LOG.info("Successfully informed collaborator about shared todo.");

    if (autoConfirmCollaborations) {
      LOG.info("Auto-confirmed collaboration request for todo: {}", payload.getTodoId());
      Thread.sleep(2_500);
      todoCollaborationService.confirmCollaboration(payload.getCollaboratorEmail(), payload.getTodoId(), payload.getCollaboratorId(), payload.getToken());
    }
  }
}

```
The mailSender#send function hands the delivery of the email to Amazon SES. We handle the user's response to the email in
the TodoCollaborationController:
```java
@Controller
@RequestMapping("/todo")
public class TodoCollaborationController {
    //...
  @GetMapping("/{todoId}/collaborations/{collaboratorId}/confirm")
  public String confirmCollaboration(
    @PathVariable("todoId") Long todoId,
    @PathVariable("collaboratorId") Long collaboratorId,
    @RequestParam("token") String token,
    @AuthenticationPrincipal OidcUser user,
    RedirectAttributes redirectAttributes
  ) {
    if (todoCollaborationService.confirmCollaboration(user.getEmail(), todoId, collaboratorId, token)) {
      redirectAttributes.addFlashAttribute("message", "You've confirmed that you'd like to collaborate on this todo.");
      redirectAttributes.addFlashAttribute("messageType", "success");
    } else {
      redirectAttributes.addFlashAttribute("message", "Invalid collaboration request.");
      redirectAttributes.addFlashAttribute("messageType", "danger");
    }

    return "redirect:/dashboard";
  }
}

```
We confirm the collaboration with the TodoCollaborationService:
```java
@Service
@Transactional
public class TodoCollaborationService {
    // ...
  public boolean confirmCollaboration(String authenticatedUserEmail, Long todoId, Long collaboratorId, String token) {

    Person collaborator = personRepository
      .findByEmail(authenticatedUserEmail)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_EMAIL + authenticatedUserEmail));

    if (!collaborator.getId().equals(collaboratorId)) {
      return false;
    }

    TodoCollaborationRequest collaborationRequest = todoCollaborationRequestRepository
      .findByTodoIdAndCollaboratorId(todoId, collaboratorId);

    LOG.info("Collaboration request: {}", collaborationRequest);

    if (collaborationRequest == null || !collaborationRequest.getToken().equals(token)) {
      return false;
    }

    LOG.info("Original collaboration token: {}", collaborationRequest.getToken());
    LOG.info("Request token: {}", token);

    Todo todo = todoRepository
      .findById(todoId)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_TODO_ID + todoId));

    todo.addCollaborator(collaborator);

    todoCollaborationRequestRepository.delete(collaborationRequest);

    String name = collaborationRequest.getCollaborator().getName();
    String subject = "Collaboration confirmed.";
    String message = "User "
      + name
      + " has accepted your collaboration request for todo #"
      + collaborationRequest.getTodo().getId()
      + ".";
    String ownerEmail = collaborationRequest.getTodo().getOwner().getEmail();

    simpMessagingTemplate.convertAndSend("/topic/todoUpdates/" + ownerEmail, subject + " " + message);

    LOG.info("Successfully informed owner about accepted request.");

    return true;
  }
}

```
For local testing with LocalStack we use the following definition in our docker-compose.yml:
```yaml
  localstack:
    image: localstack/localstack:0.14.4
    ports:
      - 4566:4566
    environment:
      - SERVICES=sqs,ses,dynamodb
      - DEFAULT_REGION=eu-central-1
      - USE_SINGLE_REGION=true
    volumes:
      - ./src/test/resources/localstack/local-aws-infrastructure.sh:/docker-entrypoint-initaws.d/init.sh
```
To set up our local environment we use the following script in local-aws-infrastructure.sh:
```bash
#!/bin/sh

awslocal sqs create-queue --queue-name stratospheric-todo-sharing

awslocal ses verify-email-identity --email-address noreply@stratospheric.dev
awslocal ses verify-email-identity --email-address info@stratospheric.dev
awslocal ses verify-email-identity --email-address tom@stratospheric.dev
awslocal ses verify-email-identity --email-address bjoern@stratospheric.dev
awslocal ses verify-email-identity --email-address philip@stratospheric.dev

awslocal dynamodb create-table \
    --table-name local-todo-app-breadcrumb \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10 \

echo "Initialized."
```
For local development we override the url endpoint with our application-dev.yml:
```yaml
  cloud:
    aws:
      rds:
        enabled: false
      sqs:
        endpoint: http://localhost:4566
        region: eu-central-1
      mail:
        endpoint: http://localhost:4566
        region: eu-central-1
      credentials:
        secret-key: foo
        access-key: bar
```
We set auto-confirm-collaborations to true to enable email success:
```yaml
  auto-confirm-collaborations: true
```
This is then used in the autoConfirmCollaborations parameter in our TodoSharingListener:
```java
public class TodoSharingListener {

    private final MailSender mailSender;
    private final TodoCollaborationService todoCollaborationService;
    private final boolean autoConfirmCollaborations;
    private final String confirmEmailFromAddress;
    private final String externalUrl;

    private static final Logger LOG = LoggerFactory.getLogger(TodoSharingListener.class.getName());

    public TodoSharingListener(
            MailSender mailSender,
            TodoCollaborationService todoCollaborationService,
            @Value("${custom.auto-confirm-collaborations}") boolean autoConfirmCollaborations,
            @Value("${custom.confirm-email-from-address}") String confirmEmailFromAddress,
            @Value("${custom.external-url}") String externalUrl) {
        this.mailSender = mailSender;
        this.todoCollaborationService = todoCollaborationService;
        this.autoConfirmCollaborations = autoConfirmCollaborations;
        this.confirmEmailFromAddress = confirmEmailFromAddress;
        this.externalUrl = externalUrl;
    }
}
```

Here we have learnt about sending and receiving emails with AWS SES and we have looked at implementing email functionality
in a Spring Boot application.

### Production Readiness with AWS
We will now look at Amazon CloudWatch and send log data to this service. We will set up metrics for CloudWatch and alarms if
thresholds are breached. We will also make the application production-ready by securing it with HTTPS and hosting it on a custom
domain.

We will use Spring Boot Logback for logging:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder"/>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```
Here we will use centralized logging. We will use CloudWatch logging and learn how to send logs from ECS Fargate
to Amazon CloudWatch.

![image](https://user-images.githubusercontent.com/27693622/228988138-00f0fb54-4738-45fb-8c8a-b68bd21cec23.png)

Above, we can see that we are running our application in an ECS Cluster. The Fargate Task runs our Docker images
and we will send logs to Amazon CloudWatch so that we can keep our logs in a centralized location.

### Deploying the application

We first need to set up our cdk.json to the correct configuration for our account:
```yaml
{
  "context": {
    "applicationName": "todo-maker",
    "region": "eu-west-2",
    "accountId": "706054169063",
    "dockerRepositoryName": "todo-maker",
    "dockerImageTag": "1",
    "applicationUrl": "https://app.drspencer.io",
    "loginPageDomainPrefix": "stratospheric-staging",
    "environmentName": "staging",
    "springProfile": "aws",
    "activeMqUsername": "activemqUser",
    "canaryUsername": "canary",
    "canaryUserPassword": "SECRET_OVERRIDDEN_BY_WORKFLOW",
    "confirmationEmail": "info@stratospheric.dev",
    "applicationDomain": "app.drspencer.io",
    "sslCertificateArn": "arn:aws:acm:eu-west-2:706054169063:certificate/ab9a0a58-cd09-493f-84cb-b42f9db0902a",
    "hostedZoneDomain": "drspencer.io",
    "githubToken": "SECRET_OVERRIDDEN_BY_WORKFLOW"
  }
}
```
We then bootstrap the resources:
```bash
npm run bootstrap -- --profile stratospheric
```

We then create an SSL Certificate for my domain:
```bash
npm run certificate:deploy -- --profile stratospheric
```

We then deploy the NetworkStack-dependent infrastructure:
```bash
npm run network:deploy -- --profile stratospheric
npm run database:deploy -- --profile stratospheric
npm run activeMq:deploy -- --profile stratospheric
```
Next we deploy the NetworkStack-independent infrastructure:
```bash
npm run repository:deploy -- --profile stratospheric
npm run messaging:deploy -- --profile stratospheric
npm run cognito:deploy -- --profile stratospheric
```
Then we run the command to route traffic from our custom domain to the ELB:
```bash
npm run domain:deploy -- --profile stratospheric
```

#### Build and Push the First Docker Image
First we go to the root of the application and run:
```bash
./gradlew build docker build -t <accountId>.dkr.ecr.<region>.amazonaws.com/<applicationName>:1

aws ecr get-login-password --region <region> --profile stratospheric | docker login \
--username AWS --password-stdin <accountId>.drk.ecr.

docker push <accountId>.drk.ecr.<region>.amazonaws.com/<applicationName>:1
```

#### Deploy the Docker image to the ECS Cluster
```bash
npm run service:deploy -- --profile stratospheric
```

#### Deploy monitoring Infrastructure
```bash
cd cdk
npm run monitoring:deploy -- --profile stratospheric
```

#### Deploy Canary stack
```bash
cd cdk
npm run canary:deploy -- --profile stratospheric
```

#### Destroy everything
```bash
npm run *:destroy -- --profile stratospheric
```
Run the above command with all the earlier scripts to ensure that all stacks are removed

This is what the app looks like after deploy:

![drspencer_2023-4-2T19-7-49](https://user-images.githubusercontent.com/27693622/229370825-ccbcc867-86ab-486a-9bfe-391dbb503290.png)

### CloudWatch Logging Terminology
- Log Stream: stream of logs from the same source (e.g. a single Docker Container)
- Log Group: An aggregation of log streams to group logs together
- CloudWatch Log Insights: Service that provides UI and query language to search one or log groups

Example queries to CloudWatch could include:
```bash
fields @timestamp, @message
| sort @timestamp desc
| limit 20
``` 

