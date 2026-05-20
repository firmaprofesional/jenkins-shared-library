#!/usr/bin/env groovy

def call(container, String step) {
    def exitCode = sh(
        script: "docker inspect -f '{{.State.ExitCode}}' ${container.id}",
        returnStdout: true
    ).trim().toInteger()

    def value = 'OK'
    if (exitCode > 0) {
        currentBuild.result = 'FAILURE'
        value = 'FAILURE'
    }

    env."${step}_result" = value
}