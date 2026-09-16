#!/usr/bin/env groovy

def call(String orgSlug, String projectSlug, String version, String environment, String startedAt, String completedAt, String pipelineName) {
    try {
        def sentryApiUrl = 'https://de.sentry.io'
        def encodedVersion = URLEncoder.encode(version, 'UTF-8').replace('+', '%20')

        withCredentials([string(credentialsId: 'SENTRY_AUTH_TOKEN', variable: 'SENTRY_AUTH_TOKEN')]) {
            def releasePayload = """{"version":"${version}","projects":["${projectSlug}"]}"""

            sh(script: """
                curl -s -o /dev/null -w "%{http_code}" -X POST \
                "${sentryApiUrl}/api/0/organizations/${orgSlug}/releases/" \
                -H "Authorization: Bearer \$SENTRY_AUTH_TOKEN" \
                -H "Content-Type: application/json" \
                -d '${releasePayload}'
            """, returnStdout: true).trim()

            def deployPayload = """{"environment":"${environment}","name":"${pipelineName} #${env.BUILD_NUMBER}","url":"${env.BUILD_URL}","dateStarted":"${startedAt}","dateFinished":"${completedAt}"}"""

            def httpStatus = sh(script: """
                curl -s -o /dev/null -w "%{http_code}" -X POST \
                "${sentryApiUrl}/api/0/organizations/${orgSlug}/releases/${encodedVersion}/deploys/" \
                -H "Authorization: Bearer \$SENTRY_AUTH_TOKEN" \
                -H "Content-Type: application/json" \
                -d '${deployPayload}'
            """, returnStdout: true).trim()

            echo "Sentry deployment notified — HTTP ${httpStatus}"
        }
    } catch (Exception e) {
        echo "Sentry notification failed (non-blocking): ${e.message}"
    }
}
