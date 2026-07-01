#!/usr/bin/env groovy
package io.github.ronkaiser.jenkins

class Git implements Serializable {

    def script

    Git(script) {
        this.script = script
    }

    def commitVersionBump(String repoUrl, String branch, String credentialsId) {
        script.withCredentials([
            script.usernamePassword(
                credentialsId: credentialsId,
                passwordVariable: 'GIT_PASS',
                usernameVariable: 'GIT_USER'
            )
        ]) {
            script.sh 'git config --global user.email "jenkins@example.com"'
            script.sh 'git config --global user.name "jenkins"'

            script.sh "git remote set-url origin ${repoUrl}"
            script.sh 'git add .'
            script.sh 'git diff --cached --quiet || git commit -m "ci: version bump"'

            script.sh """
                git -c "credential.helper=!f() { echo username=\$GIT_USER; echo password=\$GIT_PASS; }; f" pull --rebase origin ${branch}
                git -c "credential.helper=!f() { echo username=\$GIT_USER; echo password=\$GIT_PASS; }; f" push origin HEAD:${branch}
            """
        }
    }
}