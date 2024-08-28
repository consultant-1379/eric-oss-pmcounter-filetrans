#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

stage('Post Image') {
    sh "${bob} -r ${ruleset} image:store-image-size"
    sh "${bob} -r ${ruleset} image:update-characteristic-test-report"
    sh "${bob} -r ${ruleset} image:update-test-report"
}
