pipeline { 
  agent any
  options { timestamps(); disableConcurrentBuilds() }

  parameters {
    // booleanParam(name: 'MANUAL_DEV_DEPLOY', defaultValue: false, description: 'Check to trigger manual dev deploy')
    // booleanParam(name: 'MANUAL_PROD_DEPLOY', defaultValue: false, description: 'Check to trigger manual prod deploy')
    booleanParam(name: 'MANUAL_BACK', defaultValue: false, description: 'Backend only')
    booleanParam(name: 'MANUAL_FRONT', defaultValue: false, description: 'Frontend only')
    booleanParam(name: 'MANUAL_AI', defaultValue: false, description: 'AI Backend only')
    booleanParam(name: 'MANUAL_WORKER', defaultValue: false, description: 'deploy worker')
  }

  environment {
    GIT_URL_HTTPS = 'https://lab.ssafy.com/s13-final/S13P31E107.git'
    GIT_CREDS_HTTPS = 'seok'
    RELEASE_BRANCH = 'master'
    DEVELOP_BRANCH = 'develop'
    COMPOSE_FILE = 'Deploy/docker-compose.yml'
  }

  stages {
    stage('Checkout') {
      steps {
        script {
            checkout([$class: 'GitSCM',
              branches: [[name: "*/develop"]],
              userRemoteConfigs: [[url: env.GIT_URL_HTTPS, credentialsId: env.GIT_CREDS_HTTPS]]
            ])
            sh '''
              set -eu
              git fetch --no-tags origin "develop:develop" || true
            '''
        }
      }
    }

    stage('Prepare .env') {
      steps {
        withCredentials([file(credentialsId: 'ENV_FILE', variable: 'ENV_FILE')]) {
          sh '''
            set -eu
            install -m 600 "$ENV_FILE" Deploy/.env
          '''
        }
      }
    }

    stage('Back Deploy (compose up)') {
      when {
        expression {
            params.MANUAL_BACK
        }
      }
      steps {
        sh '''
          set -eux

          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" pull || true
          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build klp_back
        '''
      }
    }

    stage('Front Deploy (compose up)') {
      when {
        expression {
            params.MANUAL_FRONT
        }
      }
      steps {
        sh '''
          set -eux

          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" pull || true
          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build klp_front
        '''
      }
    }

    stage('AI Deploy (compose up)') {
      when {
        expression {
          params.MANUAL_AI
        }
      }
      steps {
        sh '''
          set -eux

          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" pull || true
          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build klp_ai
        '''
      }
    }

    stage('Worker Deploy (compose up)') {
      when {
        expression {
          params.MANUAL_WORKER
        }
      }
      steps {
        sh '''
          set -eux

          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" pull || true
          docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build note_consumer
        '''
      }
    }
  }

  post {
    success {
      echo "✅ DEV deploy done (compose up --build)."
    }
    failure {
      echo "❌ DEV deploy failed."
    }
  }
}
