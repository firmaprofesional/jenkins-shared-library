#!/usr/bin/env groovy

def call(String deptracDir, String deptracFile) {
    publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: deptracDir, reportFiles: deptracFile, reportName: 'DeptracGraph', reportTitles: 'Deptrac Graph'])
}