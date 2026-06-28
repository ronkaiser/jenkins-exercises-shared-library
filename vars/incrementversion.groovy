#!/usr/bin/env groovy

def call() {
    echo "incrementing app version..."
    sh 'npm version minor --no-git-tag-version'

    def pkg = readJSON file: 'package.json'
    def version = pkg.version

    env.IMAGE_NAME = "${version}-${env.BUILD_NUMBER}"
    echo "Image tag: ${env.IMAGE_NAME}"
}