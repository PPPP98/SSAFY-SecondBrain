pipeline { 
  agent any
  options { 
    timestamps()
    disableConcurrentBuilds() 
  }

  parameters {
    booleanParam(name: 'MANUAL_BACK',   defaultValue: false, description: 'Backend only (build)')
    booleanParam(name: 'MANUAL_FRONT',  defaultValue: false, description: 'Frontend only (build)')
    booleanParam(name: 'MANUAL_AI',     defaultValue: false, description: 'AI Backend only (build)')
    booleanParam(name: 'MANUAL_WORKER', defaultValue: false, description: 'Deploy worker (note_consumer)')
  }

  environment {
    GIT_URL_HTTPS   = 'https://lab.ssafy.com/s13-final/S13P31E107.git'
    GIT_CREDS_HTTPS = 'seok'
    RELEASE_BRANCH  = 'master'
    DEVELOP_BRANCH  = 'develop'
    COMPOSE_FILE    = 'Deploy/docker-compose.yml'

    // Blue/Green 관련
    NGINX_CONTAINER   = 'nginx'
    ACTIVE_COLOR_FILE = '/etc/nginx/snippets/active-color.conf'  // nginx 컨테이너 기준 경로
  }

  stages {
    stage('Checkout (develop)') {
      steps {
        script {
          checkout([$class: 'GitSCM',
            branches: [[name: "*/${env.DEVELOP_BRANCH}"]],
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

    // ===== Blue/Green 색 결정 =====
    stage('Determine Color') {
      when {
        expression {
          // back/front/ai 중 하나라도 체크되었을 때만 Blue/Green 동작
          return params.MANUAL_BACK || params.MANUAL_FRONT || params.MANUAL_AI
        }
      }
      steps {
        script {
          // nginx 컨테이너 내부 active-color.conf에서 현재 색 읽기
          def currentColor = sh(
            script: """
              if docker exec ${NGINX_CONTAINER} test -f ${ACTIVE_COLOR_FILE}; then
                docker exec ${NGINX_CONTAINER} sh -c "grep -E 'set .*active_color' ${ACTIVE_COLOR_FILE} | awk '{print \\$3}' | tr -d ';'"
              else
                echo blue
              fi
            """,
            returnStdout: true
          ).trim()

          env.CURRENT_COLOR = currentColor
          env.NEXT_COLOR    = (currentColor == 'blue') ? 'green' : 'blue'

          echo "CURRENT_COLOR = ${env.CURRENT_COLOR}"
          echo "NEXT_COLOR    = ${env.NEXT_COLOR}"
        }
      }
    }

    // ===== Blue/Green 스택에 수동 배포 =====
    stage('Blue/Green Deploy (manual)') {
      when {
        expression {
          return params.MANUAL_BACK || params.MANUAL_FRONT || params.MANUAL_AI
        }
      }
      steps {
        script {
          timeout(time: 20, unit: 'MINUTES') {
            // 공통: compose pull
            sh '''
              set -eux
              docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" pull || true
            '''

            // NEXT_COLOR 스택 서비스 이름
            def backService  = "klp_back_${env.NEXT_COLOR}"
            def frontService = "klp_front_${env.NEXT_COLOR}"
            def aiService    = "klp_ai_${env.NEXT_COLOR}"

            // ----- Backend -----
            if (params.MANUAL_BACK) {
              echo "Deploying (build) backend: ${backService}"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build ${backService}
              """
            } else {
              echo "Backend not selected (MANUAL_BACK=false) — ensure ${backService} is up without rebuild"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d ${backService}
              """
            }

            // ----- Frontend -----
            if (params.MANUAL_FRONT) {
              echo "Deploying (build) frontend: ${frontService}"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build ${frontService}
              """
            } else {
              echo "Frontend not selected — ensure ${frontService} is up without rebuild"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d ${frontService}
              """
            }

            // ----- AI -----
            if (params.MANUAL_AI) {
              echo "Deploying (build) AI: ${aiService}"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d --build ${aiService}
              """
            } else {
              echo "AI not selected — ensure ${aiService} is up without rebuild"
              sh """
                set -eux
                docker compose --env-file Deploy/.env -f "$COMPOSE_FILE" up -d ${aiService}
              """
            }
          }
        }
      }
    }

    // ===== NEXT_COLOR 헬스체크 =====
    stage('Health Check (NEXT_COLOR)') {
      when {
        expression {
          return params.MANUAL_BACK || params.MANUAL_FRONT || params.MANUAL_AI
        }
      }
      steps {
        script {
          def maxAttempts = 10
          def ok = false

          for (int i = 1; i <= maxAttempts; i++) {
            echo "Health check attempt ${i}/${maxAttempts} on color=${env.NEXT_COLOR}"

            def backOk = sh(
              script: "curl -fsS http://klp_back_${env.NEXT_COLOR}:8080/health || echo FAIL",
              returnStdout: true
            ).trim()

            def aiOk = sh(
              script: "curl -fsS http://klp_ai_${env.NEXT_COLOR}:8000/ai/health || echo FAIL",
              returnStdout: true
            ).trim()

            def frontOk = sh(
              script: "curl -fsS http://klp_front_${env.NEXT_COLOR}/health || echo FAIL",
              returnStdout: true
            ).trim()

            if (!backOk.contains("FAIL") && !aiOk.contains("FAIL") && !frontOk.contains("FAIL")) {
              ok = true
              break
            }
            sleep 10
          }

          if (!ok) {
            error "NEXT_COLOR (${env.NEXT_COLOR}) stack health check failed"
          }
        }
      }
    }

    // ===== Nginx active_color 스위치 + 이전 색 stop =====
    stage('Switch Nginx & Stop Previous Color') {
      when {
        expression {
          return params.MANUAL_BACK || params.MANUAL_FRONT || params.MANUAL_AI
        }
      }
      steps {
        script {
          // nginx 컨테이너 내부 active-color.conf 업데이트 + reload
          sh """
            set -eux
            docker exec ${NGINX_CONTAINER} /bin/sh -c 'echo "set \\$active_color ${env.NEXT_COLOR};" > ${ACTIVE_COLOR_FILE} && nginx -s reload'
          """
        }
      }
    }

    // ===== Worker는 Blue/Green과 별개로 단일 배포 =====
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
      echo "✅ MANUAL deploy done (Blue/Green + compose up)."
    }
    failure {
      echo "❌ MANUAL deploy failed."
    }
  }
}
