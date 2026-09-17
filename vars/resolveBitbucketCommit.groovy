#!/usr/bin/env groovy

// Resolves the commit SHA a ref (branch or tag name) points to in a
// Bitbucket Cloud repo. Some pipelines deploy a branch (e.g. "release/1.30"),
// others deploy a tag (e.g. "1.26") — tries branches first, then tags.
// Returns an empty string (non-blocking) if the lookup fails, so callers can
// still deploy even if commit association can't be set up for this run.
def call(String workspace, String repoSlug, String ref) {
    try {
        withCredentials([usernamePassword(credentialsId: 'bitbucket-check-version', usernameVariable: 'BITBUCKET_USER', passwordVariable: 'BITBUCKET_TOKEN')]) {
            def sha = fetchHash(workspace, repoSlug, 'branches', ref)
            if (!sha) {
                sha = fetchHash(workspace, repoSlug, 'tags', ref)
            }
            if (!sha) {
                echo "Could not resolve commit SHA for ${workspace}/${repoSlug}@${ref} (not found as branch or tag)"
            }
            return sha
        }
    } catch (Exception e) {
        echo "Bitbucket commit resolution failed (non-blocking): ${e.message}"
        return ''
    }
}

def fetchHash(String workspace, String repoSlug, String kind, String ref) {
    def response = sh(script: """
        curl -s -w "\\nHTTP_STATUS:%{http_code}" -u "\$BITBUCKET_USER:\$BITBUCKET_TOKEN" \
        "https://api.bitbucket.org/2.0/repositories/${workspace}/${repoSlug}/refs/${kind}/${ref}"
    """, returnStdout: true).trim()

    def statusIdx = response.lastIndexOf('HTTP_STATUS:')
    def body = statusIdx >= 0 ? response.substring(0, statusIdx) : response
    def status = statusIdx >= 0 ? response.substring(statusIdx + 'HTTP_STATUS:'.length()).trim() : ''

    echo "Bitbucket ${kind}/${ref} lookup -> HTTP ${status}: ${body.take(300)}"

    if (status != '200') {
        return ''
    }
    def matcher = (body =~ /"hash"\s*:\s*"([0-9a-f]+)"/)
    return matcher.find() ? matcher.group(1) : ''
}
