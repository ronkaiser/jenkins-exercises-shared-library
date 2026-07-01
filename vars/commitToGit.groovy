#!/usr/bin/env groovy

import io.github.ronkaiser.jenkins.Git

def call(String repoUrl, String branch, String credentialsId) {
    return new Git(this).commitVersionBump(repoUrl, branch, credentialsId)
}