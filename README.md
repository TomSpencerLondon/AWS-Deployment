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
because we will only deploy the ECS cluster once. We run the above command and can see that the stack is being deployed:

![image](https://user-images.githubusercontent.com/27693622/228232253-81b6be5e-be9f-4fae-a7d7-bd05ab409d9c.png)
