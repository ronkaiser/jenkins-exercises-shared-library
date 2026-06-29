#!/usr/bin/env groovy

def call() {
    echo "run tests"
    sh 'npm install && npm test'
}