#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

stage('Marketplace Documents Upload') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
    string(credentialsId: 'SFTP_FT_MARKETPLACE_TOKEN', variable: 'MARKETPLACE_TOKEN')]) {
        sh "${bob} -r ${ruleset} generate-doc-zip-package"
        archiveArtifacts "build/doc-marketplace/**"
        //  archiveArtifacts "build/doc-svl-replacement/**"
        if(env.RELEASE) {
            sh "${bob} -r ${ruleset} marketplace-upload:upload-doc-to-arm-released"
        }
        else {
            sh "${bob} -r ${ruleset} marketplace-upload:upload-doc-to-arm-dev"
        }
        sh "${bob} -r ${ruleset} marketplace-upload:refresh-adp-portal-marketplace"
    }
}
