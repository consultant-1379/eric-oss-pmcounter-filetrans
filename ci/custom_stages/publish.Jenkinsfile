#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

if(env.RELEASE) {
    stage('Custom Publish') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'), file(credentialsId: 'docker-config-json', variable: 'DOCKER_CONFIG_JSON')]) {
            ci_pipeline_scripts.checkDockerConfig()
            sh "${bob} -r ${ci_ruleset} upload-mvn-jars"
            sh "${bob} -r ${ruleset} publish"
        }
    }
}
