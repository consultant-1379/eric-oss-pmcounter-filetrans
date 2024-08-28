#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

stage ('FOSS Validation for Mimer') {
    if ( env.MUNIN_UPDATE_ENABLED == "true" ) {
        withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
            sh "${bo} -r ${ruleset} munin-update-version"
        }
    }
}
