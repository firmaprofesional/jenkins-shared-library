#!/usr/bin/env groovy

def call(Map config) {
    String channel     = config.channel     ?: 'deployments'
    String environment = config.environment ?: 'unknown'
    String title       = config.title       ?: env.JOB_NAME
    String requestedBy = config.requestedBy ?: 'DevOps'
    List   deployments = config.deployments ?: []

    String color        = 'good'
    boolean hasErrors   = false
    String lines        = ''

    deployments.each { d ->
        String emoji = ':tada:'
        if (d.result == 'FAILURE') {
            color     = 'danger'
            emoji     = ':skull:'
            hasErrors = true
        }
        String branch = (d.branch ?: 'unknown').replace('refs/heads/', '')
        lines += "\n:small_orange_diamond:${d.name} (${branch}) ${emoji}"
    }

    String suffix  = hasErrors ? ' with errors' : ''
    String message = "${title} deployed${suffix} on ${environment}:${lines}\nrequested by: ${requestedBy}"

    slackSend(color: color, channel: channel, message: message)
}
