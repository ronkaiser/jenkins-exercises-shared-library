#!/user/bin/env groovy

import io.github.ronkaiser.jenkins.Docker
def call() {
    return new Docker(this).dockerLogin()
}