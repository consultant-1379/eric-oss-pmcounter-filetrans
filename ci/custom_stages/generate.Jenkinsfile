#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

stage('Custom Generate Readme Docs') {
    sh "${bob} -r ${ruleset} generate-docs"
    archiveArtifacts "build/doc/**/*.*"
}
