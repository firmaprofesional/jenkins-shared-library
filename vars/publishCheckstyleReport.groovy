#!/usr/bin/env groovy

def call(String checkstyleFile) {
    def checkstyle = scanForIssues tool: checkStyle(pattern: checkstyleFile)
    publishIssues issues: [checkstyle]
}