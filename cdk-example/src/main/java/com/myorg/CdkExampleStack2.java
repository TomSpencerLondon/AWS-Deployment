package com.myorg;

import dev.stratospheric.cdk.DockerRepository;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class CdkExampleStack2 extends Stack {
    public CdkExampleStack2(final Construct scope, final String id, final StackProps props, final String repoName) {
        super(scope, id, props);

        Environment env = props.getEnv();

        DockerRepository repo = new DockerRepository(this, "repo", env,
                new DockerRepository.DockerRepositoryInputParameters(
                        repoName,
                        env.getAccount(),
                        10
                ));
    }
}
