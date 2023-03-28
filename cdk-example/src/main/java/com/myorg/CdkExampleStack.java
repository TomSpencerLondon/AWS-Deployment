package com.myorg;

import dev.stratospheric.cdk.DockerRepository;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

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
