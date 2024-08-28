#!/usr/bin/env groovy

def bob = "./bob/bob"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

stage('Custom Lint') {
    parallel(
        "lint markdown": {
            sh "${bob} -r ${ci_ruleset} lint:markdownlint lint:vale"
        },
        "lint helm": {
            sh "${bob} -r ${ci_ruleset} lint:helm"
        },
        "lint helm design rule checker": {
            sh "${bob} -r ${ci_ruleset} lint:helm-chart-check"
        },
        "lint code": {
            sh "${bob} -r ${ci_ruleset} lint:license-check"
        },
        "lint metrics": {
            sh "${bob} -r ${ci_ruleset} lint:metrics-check"
        }
    )
}
