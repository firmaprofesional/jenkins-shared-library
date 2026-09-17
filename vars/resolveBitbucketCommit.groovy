#!/usr/bin/env groovy

// Resolves the current HEAD commit SHA of a branch in a Bitbucket Cloud repo.
// Returns an empty string (non-blocking) if the lookup fails, so callers can
// still deploy even if commit association can't be set up for this run.
def call(String workspace, String repoSlug, String branch) {
    try {
        withCredentials([usernamePassword(credentialsId: 'bitbucket_token_read', usernameVariable: 'BITBUCKET_USER', passwordVariable: 'BITBUCKET_TOKEN')]) {
            def sha = sh(script: """
                curl -s -u "\$BITBUCKET_USER:\$BITBUCKET_TOKEN" \
                "https://api.bitbucket.org/2.0/repositories/${workspace}/${repoSlug}/refs/branches/${branch}" \
                | grep -o '"hash":[ ]*"[0-9a-f]*"' | head -1 | grep -o '[0-9a-f]\\{40\\}'
            """, returnStdout: true).trim()

            if (!sha) {
                echo "Could not resolve commit SHA for ${workspace}/${repoSlug}@${branch} (branch not found or unexpected API response)"
            }
            return sha
        }
    } catch (Exception e) {
        echo "Bitbucket commit resolution failed (non-blocking): ${e.message}"
        return ''
    }
}
