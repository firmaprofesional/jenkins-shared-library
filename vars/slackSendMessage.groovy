#!/usr/bin/env groovy

def call(String buildStatus = 'STARTED', time) {
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'

    def color
    def message
    color = 'good'
    message = 'Succes after'
    if (buildStatus == 'UNSTABLE') {
        color = 'warning'
        message = 'Still Failing after'
    } else if (buildStatus == 'FAILURE') {
        color = 'danger'
        message = 'Failure after'
    }
    def msg = "${env.JOB_NAME} - ${env.BUILD_DISPLAY_NAME} " + message + " " + time + " (<${env.BUILD_URL}|Open>)";
    slackSend(color: color, message: msg)
}
