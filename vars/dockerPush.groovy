#!/user/bin/env groovy

import io.github.ronkaiser.jenkins.Docker
def call(String imageName) {
    return new Docker(this).dockerPush(imageName)
}