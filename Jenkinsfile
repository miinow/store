pipeline {
  agent any

  tools {
    jdk 'temurin-17'
  }

  options {
    ansiColor('xterm')
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    choice(name: 'BUILD_TYPE', choices: ['build', 'release'], description: 'Select build type: build (snapshot) or release')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          // Ensure tags are available for Axion
          if (isUnix()) {
            sh 'git fetch --tags --force || true'
          } else {
            bat 'git fetch --tags --force || ver > nul'
          }
        }
      }
    }

    stage('Load Pipeline Properties') {
      steps {
        script {
          if (fileExists('jenkinsfile.properties')) {
            def props = readProperties file: 'jenkinsfile.properties'
            // Apply properties to environment; parameters can still be used explicitly in UI
            ['REGISTRY','IMAGE_REPO','REGISTRY_CREDENTIALS_ID'].each { key ->
              def val = props[key]
              if (val != null && val.toString().trim()) {
                env."${key}" = val.toString().trim()
              }
            }
            echo "Loaded pipeline properties: REGISTRY=${env.REGISTRY}, IMAGE_REPO=${env.IMAGE_REPO}, REGISTRY_CREDENTIALS_ID=${env.REGISTRY_CREDENTIALS_ID}"
          } else {
            echo 'jenkinsfile.properties not found; using defaults and job parameters.'
          }
        }
      }
    }

    stage('Prepare Release Version') {
          when { expression { return params.BUILD_TYPE == 'release' } }
          steps {
            script {
              // Resolve currentVersion (likely X.Y.Z-SNAPSHOT) and compute release (strip -SNAPSHOT)
              def cv = ''
              if (isUnix()) {
                sh 'chmod +x gradlew'
                cv = sh(script: './gradlew -q currentVersion', returnStdout: true).trim()
              } else {
                def out = bat(script: 'gradlew.bat -q currentVersion', returnStdout: true)
                cv = out.tokenize('\n').last().trim()
              }
              env.RELEASE_VERSION = cv.replace('-SNAPSHOT','')
              echo "Planned release version: ${env.RELEASE_VERSION}"
            }
          }
        }

    stage('Build & Test') {
      steps {
        script {
          if (isUnix()) {
            sh 'chmod +x gradlew'
            sh './gradlew clean build --no-daemon'
          } else {
            bat 'gradlew.bat clean build --no-daemon'
          }
        }
      }
      post {
        always {
          junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
          archiveArtifacts artifacts: 'build/reports/tests/test/**', onlyIfSuccessful: false, allowEmptyArchive: true
          archiveArtifacts artifacts: 'build/reports/jacoco/test/html/**', onlyIfSuccessful: false, allowEmptyArchive: true
        }
      }
    }

    stage('Docker Build & Push') {
      when { expression { return params.BUILD_TYPE in ['build','release'] } }
      steps {
        script {
          def shortSha = (env.GIT_COMMIT ?: 'dev').take(12)
          def branch = env.BRANCH_NAME ?: ''
          def isMain = (branch == 'main' || branch == 'master')

          def imageVersion = ''
          if (params.BUILD_TYPE == 'release') {
            imageVersion = env.RELEASE_VERSION ?: '0.0.0'
          } else {
            if (isUnix()) {
              sh 'chmod +x gradlew'
              imageVersion = sh(script: './gradlew -q currentVersion', returnStdout: true).trim()
            } else {
              def out = bat(script: 'gradlew.bat -q currentVersion', returnStdout: true)
              imageVersion = out.tokenize('\n').last().trim()
            }
          }

          def imageVersionTag = "${env.REGISTRY}/${env.IMAGE_REPO}:${imageVersion}"
          def imageShaTag = "${env.REGISTRY}/${env.IMAGE_REPO}:${shortSha}"

          docker.withRegistry("https://${env.REGISTRY}", env.REGISTRY_CREDENTIALS_ID) {
            // Build image from multi-stage Dockerfile
            def img = docker.build(imageVersionTag, '--pull .')
            img.push()

            // Push SHA tag for traceability
            sh "docker tag ${imageVersionTag} ${imageShaTag}"
            sh "docker push ${imageShaTag}"
          }
        }
      }
    }

    stage('Release (Tag and Bump)') {
      when { expression { return params.BUILD_TYPE == 'release' } }
      steps {
        script {
          if (isUnix()) {
            sh 'chmod +x gradlew'
            sh './gradlew release --no-daemon'
          } else {
            bat 'gradlew.bat release --no-daemon'
          }
        }
      }
    }
  }


  post {
    success {
      echo "Pipeline completed successfully."
    }
    failure {
      echo 'Pipeline failed. Check logs and archived reports.'
    }
    always {
      cleanWs(cleanWhenFailure: false)
    }
  }
}
