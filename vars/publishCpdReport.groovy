#!/usr/bin/env groovy

def call(String cpdFile) {
    def cpd = scanForIssues tool: cpd(pattern: cpdFile)
    publishIssues issues: [cpd]
}