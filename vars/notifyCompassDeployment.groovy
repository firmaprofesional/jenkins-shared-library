#!/usr/bin/env groovy

def call(String compassCloudId,String componentId, String state, String environment, String startedAt, String completedAt, String pipelineName) {
    try {
        def cloudId = compassCloudId
        def jobName = pipelineName
        // Stable per-component identifier for the Compass event source. Using
        // jobName/pipelineName here would create a new event source per branch
        // or release version, and Compass caps components at 20 event sources
        // (ATTACHED_EVENT_SOURCE_LIMIT_REACHED -> HTTP 409).
        def eventSourceKey = componentId.tokenize('/').last()

        def envCategory = "DEVELOPMENT"
        if (environment.toLowerCase().contains("prod")) {
            envCategory = "PRODUCTION"
        } else if (environment.toLowerCase().contains("qa") || environment.toLowerCase().contains("pre")) {
            envCategory = "STAGING"
        }

        withCredentials([
            usernamePassword(
                credentialsId: 'COMPASS_BASIC_AUTH',
                usernameVariable: 'COMPASS_USER',
                passwordVariable: 'COMPASS_TOKEN'
            )
        ]) {
            def payload = """{"cloudId":"${cloudId}","componentId":"${componentId}","event":{"deployment":{"updateSequenceNumber":${env.BUILD_NUMBER},"displayName":"${jobName} #${env.BUILD_NUMBER}","url":"${env.BUILD_URL}","lastUpdated":"${completedAt}","externalEventSourceId":"${eventSourceKey}-${environment}","description":"Deploy triggered from Jenkins","deploymentProperties":{"sequenceNumber":${env.BUILD_NUMBER},"state":"${state}","startedAt":"${startedAt}","completedAt":"${completedAt}","pipeline":{"pipelineId":"${jobName}","displayName":"${jobName}","url":"${env.BUILD_URL}"},"environment":{"environmentId":"${environment}","displayName":"${environment}","category":"${envCategory}"}}}}}"""

            def httpStatus = sh(script: """
                curl -s -o /dev/null -w "%{http_code}" -X POST \
                "https://logaltygroup.atlassian.net/gateway/api/compass/v1/events" \
                -u "${COMPASS_USER}:${COMPASS_TOKEN}" \
                -H "Content-Type: application/json" \
                -d '${payload}'
            """, returnStdout: true).trim()

            echo "Compass deployment notified — HTTP ${httpStatus}"
        }
    } catch (Exception e) {
        echo "Compass notification failed (non-blocking): ${e.message}"
    }
}
