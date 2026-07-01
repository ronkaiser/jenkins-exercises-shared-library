# Jenkins Shared Library

Reusable pipeline steps for the [jenkins-exercises](https://github.com/ronkaiser/jenkins-exercises) Node.js CI/CD pipeline. Pipeline logic lives here; the application repo keeps a thin `Jenkinsfile` that orchestrates stages and passes project-specific values.

## Repository structure

```
jenkins-exercises-shared-library/
├── vars/                              # Callable steps (used from Jenkinsfile)
│   ├── incrementVersion.groovy
│   ├── runTests.groovy
│   ├── buildImage.groovy
│   ├── dockerLogin.groovy
│   ├── dockerPush.groovy
│   └── commitToGit.groovy
└── src/io/github/ronkaiser/jenkins/   # Groovy helper classes
    ├── Docker.groovy
    └── Git.groovy
```

| Folder | Purpose |
|--------|---------|
| **`vars/`** | Global pipeline steps. Filename = step name (e.g. `buildImage.groovy` → `buildImage(...)`). |
| **`src/`** | Serializable Groovy classes. Folder path must match the `package` declaration. |

Classes in `src/` cannot call pipeline steps (`sh`, `echo`, etc.) directly. They receive the pipeline script via `new Docker(this)` / `new Git(this)` and invoke steps through `script.sh`, `script.withCredentials`, and so on.

## Pipeline steps

### `incrementVersion()`

Bumps the npm version and sets the Docker image tag on the pipeline environment.

| | |
|---|---|
| **Parameters** | None |
| **Working directory** | Caller must wrap with `dir(APP_DIR)` — expects `package.json` in the current directory |
| **Side effect** | Sets `env.IMAGE_NAME` to `{version}-{BUILD_NUMBER}` |

### `runTests()`

Installs dependencies and runs the Jest test suite.

| | |
|---|---|
| **Parameters** | None |
| **Working directory** | Caller must wrap with `dir(APP_DIR)` |

### `buildImage(imageName)`

Builds a Docker image from the repository root.

| | |
|---|---|
| **Parameters** | `imageName` — full image reference (e.g. `ronkaiser86/myapp:1.3.0-42`) |
| **Working directory** | Repository root (where the `Dockerfile` lives) |
| **Implementation** | `Docker.buildDockerImage()` |

### `dockerLogin()`

Logs in to Docker Hub using Jenkins credentials.

| | |
|---|---|
| **Parameters** | None |
| **Credentials** | `docker-hub-repo` (username/password) — configured in `Docker.groovy` |
| **Implementation** | `Docker.dockerLogin()` |

### `dockerPush(imageName)`

Pushes a tagged image to the registry.

| | |
|---|---|
| **Parameters** | `imageName` — must match the tag used in `buildImage` |
| **Implementation** | `Docker.dockerPush()` |

### `commitToGit(repoUrl, branch, credentialsId)`

Commits staged changes and pushes to the remote branch.

| | |
|---|---|
| **Parameters** | `repoUrl` — HTTPS Git remote URL (no embedded credentials) |
| | `branch` — target branch (e.g. `main`) |
| | `credentialsId` — Jenkins username/password credential for Git |
| **Working directory** | Repository root |
| **Implementation** | `Git.commitVersionBump()` |

The commit step configures a local Git identity, sets the remote URL, stages all changes, creates an idempotent commit (`ci: version bump`), rebases on the remote branch, then pushes using a temporary credential helper.

## Helper classes (`src/`)

### `io.github.ronkaiser.jenkins.Docker`

| Method | Description |
|--------|-------------|
| `buildDockerImage(imageName)` | `docker build -t <imageName> .` |
| `dockerLogin()` | `docker login` via stdin with `docker-hub-repo` credentials |
| `dockerPush(imageName)` | `docker push <imageName>` |

### `io.github.ronkaiser.jenkins.Git`

| Method | Description |
|--------|-------------|
| `commitVersionBump(repoUrl, branch, credentialsId)` | Git config, add, commit, pull --rebase, push with `GIT_USER` / `GIT_PASS` |

## Jenkins setup

Register this library once in **Manage Jenkins → System → Global Pipeline Libraries**:

| Field | Value |
|-------|-------|
| **Name** | `jenkins-shared-library` |
| **Default version** | `main` |
| **Retrieval method** | Modern SCM → Git |
| **Repository** | `https://github.com/ronkaiser/jenkins-exercises-shared-library.git` |
| **Credentials** | Required if the repository is private |

In the application `Jenkinsfile`, load the library at the top:

```groovy
@Library('jenkins-shared-library') _
```

## Usage example

From the [jenkins-exercises](https://github.com/ronkaiser/jenkins-exercises) `Jenkinsfile`:

```groovy
@Library('jenkins-shared-library') _

pipeline {
    agent any
    tools { nodejs 'node' }
    environment { APP_DIR = 'app' }

    stages {
        stage('increment version') {
            steps {
                script {
                    dir(env.APP_DIR) { incrementversion() }
                }
            }
        }
        stage('run tests') {
            steps {
                dir(env.APP_DIR) { runtests() }
            }
        }
        stage('build and push docker image') {
            steps {
                script {
                    def imageName = "ronkaiser86/myapp:${env.IMAGE_NAME}"
                    buildImage(imageName)
                    dockerLogin()
                    dockerPush(imageName)
                }
            }
        }
        stage('commit to git') {
            steps {
                script {
                    commitToGit(
                        'https://github.com/ronkaiser/jenkins-exercises.git',
                        'main',
                        'github-pat-devops-08'
                    )
                }
            }
        }
    }
}
```

## Required Jenkins credentials

| Credential ID | Type | Used by |
|---------------|------|---------|
| `docker-hub-repo` | Username/password | `dockerLogin()` |
| `github-pat-devops-08` | Username/password (GitHub PAT) | `commitToGit()` |

Secrets are never stored in this repository. Credential IDs are referenced in Groovy; values are injected at runtime by Jenkins.

## Reusing in another project

1. Register this library in Jenkins (or reference it inline with `library identifier:`).
2. Add `@Library('jenkins-shared-library') _` to the project `Jenkinsfile`.
3. Pass project-specific values as parameters:
   - Docker image prefix in `buildImage` / `dockerPush`
   - Git repo URL, branch, and credential ID in `commitToGit`
4. Keep `APP_DIR` and tool configuration (`nodejs`) in the application `Jenkinsfile`.

## Versioning

For production use, tag releases of this library and pin the version in consuming pipelines:

```groovy
@Library('jenkins-shared-library@v1.0.0') _
```

Using `@main` (or the default branch) is fine for development but means library changes take effect on the next pipeline run without an application repo change.
