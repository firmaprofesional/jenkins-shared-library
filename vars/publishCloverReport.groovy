#!/usr/bin/env groovy

def call(String cloverReportDir, String cloverReportFileName) {
    step(
        [
            $class: 'CloverPublisher', 
            cloverReportDir: cloverReportDir, 
            cloverReportFileName: cloverReportFileName
        ]
    )
}
