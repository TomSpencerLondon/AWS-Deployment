package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkExampleApp2 {
    public static void main(final String[] args) {
        App app = new App();

        String accountId = (String) app.getNode().tryGetContext("accountId");
        requireNonEmpty(accountId, "context variable 'accountId' must not be null");

        String region = (String) app.getNode().tryGetContext("region");
        requireNonEmpty(region, "context variable 'region' must not be null");

        String repoName = (String) app.getNode().tryGetContext("repoName");

        Environment environment = makeEnv(accountId, region);


        new CdkExampleStack2(app, "CdkExampleStack2", StackProps.builder()
                .env(environment)
                .build(), repoName);

        app.synth();
    }

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }

    public static void requireNonEmpty(String string, String message) {
        if (string == null || "".equals(string)) {
            throw new IllegalArgumentException(message);
        }
    }
}
