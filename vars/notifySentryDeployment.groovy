#!/usr/bin/env groovy

def call(String orgSlug, String projectSlug, String version, String environment, String startedAt, String completedAt, String pipelineName, String commitSha = '', String repoSlug = '') {
    try {
        def sentryApiUrl = 'https://de.sentry.io'

        // Sentry release versions can't contain "/". Callers often pass a branch
        // name (e.g. "release/1.30") — by convention that's "<prefix>/<version>",
        // so use the segment after the last "/" as the actual release version.
        def sentryVersion = version.contains('/') ? version.tokenize('/').last() : version
        if (sentryVersion != version) {
            echo "Sentry release version derived from '${version}' -> '${sentryVersion}'"
        }
        def encodedVersion = URLEncoder.encode(sentryVersion, 'UTF-8').replace('+', '%20')

        withCredentials([string(credentialsId: 'SENTRY_AUTH_TOKEN', variable: 'SENTRY_AUTH_TOKEN')]) {
            // Optional: associate this release with the deployed commit so Sentry
            // can show suspect commits on issues (requires the Bitbucket
            // integration + repository mapping configured in Sentry).
            def refsJson = ''
            if (commitSha) {
                def repo = repoSlug ?: projectSlug
                refsJson = ""","refs":[{"repository":"firmapro/${repo}","commit":"${commitSha}"}]"""
            }
            def releasePayload = """{"version":"${sentryVersion}","projects":["${projectSlug}"]${refsJson}}"""

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
