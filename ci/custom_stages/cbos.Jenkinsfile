#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

stage('CBOS Version') {
    sh "${bob} -r ${ruleset} update-cbos-version"
}
