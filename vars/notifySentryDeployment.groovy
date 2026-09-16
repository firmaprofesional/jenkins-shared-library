#!/usr/bin/env groovy

def call(String orgSlug, String projectSlug, String version, String environment, String startedAt, String completedAt, String pipelineName) {
    try {
        def sentryApiUrl = 'https://de.sentry.io'
        def encodedVersion = URLEncoder.encode(version, 'UTF-8').replace('+', '%20')

        withCredentials([string(credentialsId: 'SENTRY_AUTH_TOKEN', variable: 'SENTRY_AUTH_TOKEN')]) {
            def releasePayload = """{"version":"${version}","projects":["${projectSlug}"]}"""

            def releaseResponse = sh(script: """
                curl -s -w "\\nHTTP_STATUS:%{http_code}" -X POST \
                "${sentryApiUrl}/api/0/organizations/${orgSlug}/releases/" \
                -H "Authorization: Bearer \$SENTRY_AUTH_TOKEN" \
                -H "Content-Type: application/json" \
                -d '${releasePayload}'
            """, returnStdout: true).trim()

            echo "Sentry release create response: ${releaseResponse}"

            def deployPayload = """{"environment":"${environment}","name":"${pipelineName} #${env.BUILD_NUMBER}","url":"${env.BUILD_URL}","dateStarted":"${startedAt}","dateFinished":"${completedAt}"}"""

            def deployResponse = sh(script: """
                curl -s -w "\\nHTTP_STATUS:%{http_code}" -X POST \
                "${sentryApiUrl}/api/0/organizations/${orgSlug}/releases/${encodedVersion}/deploys/" \
                -H "Authorization: Bearer \$SENTRY_AUTH_TOKEN" \
                -H "Content-Type: application/json" \
                -d '${deployPayload}'
            """, returnStdout: true).trim()

            echo "Sentry deploy create response: ${deployResponse}"
        }
    } catch (Exception e) {
        echo "Sentry notification failed (non-blocking): ${e.message}"
    }
}
