pipeline { 
  agent any
  options { timestamps(); disableConcurrentBuilds() }

  triggers {
    GenericTrigger(
      genericVariables: [
        [key:'GL_EVENT',        value:'$.object_kind'],
        [key:'GL_MR_ACTION',    value:'$.object_attributes.action'],
        [key:'GL_MR_STATE',     value:'$.object_attributes.state'],
        [key:'GL_MR_TITLE',     value:'$.object_attributes.title'],
        [key:'GL_MR_SOURCE',    value:'$.object_attributes.source_branch'],
        [key:'GL_MR_TARGET',    value:'$.object_attributes.target_branch'],
        [key:'GL_MR_IID',       value:'$.object_attributes.iid'],
        [key:'GL_PROJECT',      value:'$.project.path_with_namespace'],
        [key:'GL_MR_SHA',       value:'$.object_attributes.last_commit.id'],
        [key:'GL_MR_URL',       value:'$.object_attributes.url'],
        [key:'GL_ASSIGNEE',     value:'$.assignees[0].username'],
        [key:'GL_REVIEWER',     value:'$.reviewers[0].username'],
        [key:'GL_USER_NAME',    value:'$.user.name']
      ],
      token: 'uknow-mr',
      printContributedVariables: false,
      printPostContent: false,
      regexpFilterText: '$GL_EVENT:$GL_MR_ACTION',
      regexpFilterExpression: '^merge_request:(open|reopen|merge|update)$'
    )
  }

  environment {
    GIT_URL_HTTPS   = 'https://lab.ssafy.com/s13-final/S13P31E107.git'
    GIT_CREDS_HTTPS = 'seok'

    // GitLab 설정
    GITLAB_BASE        = 'https://lab.ssafy.com'                // 예: https://gitlab.com 과 동일 개념
    GITLAB_PROJECT_ENC = 's13-final%2FS13P31E107'               // <namespace>%2F<project>
    STATUS_CONTEXT     = 'jenkins:mr-title-check'               // 커밋 상태의 context 라벨
    RELEASE_BRANCH     = 'main'                                // 릴리스 브랜치 이름 (태그 푸시용)
    DEVELOP_BRANCH     = 'develop'                                // 개발 브랜치 이름 (자동 배포용)

    COMPOSE_DEV_FILE = 'deploy/docker-compose.dev.yml'
    COMPOSE_PROD_FILE = 'deploy/docker-compose.prod.yml'
    COMPOSE_AI_FILE = 'deploy/docker-compose.ai.yml'
  }

  stages {
    stage('Debug payload') {
      steps {
        sh 'env | sort | sed -n "1,140p"'
      }
    }

    stage('Checkout source branch (HTTPS)') {
      when {
        expression {
            ((env.GL_MR_ACTION ?: "") != "merge") && ((env.GL_MR_STATE ?: "") != "merged")
        }
      }
      steps {
        checkout([$class: 'GitSCM',
          branches: [[name: "*/${GL_MR_SOURCE}"]],
          userRemoteConfigs: [[
            url: env.GIT_URL_HTTPS,
            credentialsId: env.GIT_CREDS_HTTPS,
            refspec: '+refs/heads/*:refs/remotes/origin/* +refs/tags/*:refs/tags/*'
          ]],
          extensions: [[$class:'CloneOption', shallow:true, depth:1, timeout:10]]
        ])
        // 타깃 브랜치도 로컬로 받아두기 (커밋 범위 검사/추후 단계 대비)
        sh '''
          set -eu
          git fetch origin "${GL_MR_TARGET}:${GL_MR_TARGET}" || true
        '''
      }
    }

    stage('Load conventions') {
      steps {
        sh '''
          set -eu

          MR_TITLE_COMMON='^\\[(🔀|⛑️|🛫)\\s+(FE|BE|INFRA|AI)\\]\\s+.+$'
          MR_TITLE_DEV='^\\[🔀\\s+(FE|BE|INFRA|AI)\\]\\s+.+$'
          MR_TITLE_HOT='^\\[⛑️\\s+(FE|BE|INFRA|AI)\\]\\s+.+$'
          MR_TITLE_REL='^\\[🛫\\s+(FE|BE|INFRA|AI)\\]\\s+.+$'

          RELEASE_REGEX='v[0-9]+\\.[0-9]+\\.[0-9]+(-\\S+)?$'

          {
            echo "MR_TITLE_COMMON='${MR_TITLE_COMMON}'"
            echo "MR_TITLE_DEV='${MR_TITLE_DEV}'"
            echo "MR_TITLE_HOT='${MR_TITLE_HOT}'"
            echo "MR_TITLE_REL='${MR_TITLE_REL}'"
            echo "RELEASE_REGEX='${RELEASE_REGEX}'"
          } > .conventions.env

          echo "[Loaded title conventions]"
          cat .conventions.env
        '''
      }
    }

    stage('Validate MR title') {
      when {
        expression {
            ((env.GL_MR_ACTION ?: "") != "merge") && ((env.GL_MR_STATE ?: "") != "merged")
        }
      }
      steps {
        withCredentials([usernamePassword(
          credentialsId: env.GIT_CREDS_HTTPS,
          usernameVariable: 'GIT_USER',
          passwordVariable: 'GIT_PASS'
        )]) {
          sh '''
            set -eu
            . ./.conventions.env

            TITLE="${GL_MR_TITLE:-}"
            IID="${GL_MR_IID:-}"
            SHA="${GL_MR_SHA:-}"
            PROJECT_ENC="$(printf '%s' "${GL_PROJECT:-}" | sed 's#/#%2F#g')"

            if   echo "$TITLE" | grep -Pq "$MR_TITLE_DEV"; then KIND="develop"
            elif echo "$TITLE" | grep -Pq "$MR_TITLE_HOT"; then KIND="hotfix"
            elif echo "$TITLE" | grep -Pq "$MR_TITLE_REL"; then KIND="release"
            else
              MSG="❌ MR title invalid. Must match one of:\\n- ${MR_TITLE_DEV}\\n- ${MR_TITLE_HOT}\\n- ${MR_TITLE_REL}\\nTitle: ${TITLE}"

              if [ -n "$SHA" ]; then
                curl -sS -X POST -H "PRIVATE-TOKEN: ${GIT_PASS}" \
                  --data-urlencode "state=failed" \
                  --data-urlencode "context=${STATUS_CONTEXT}" \
                  --data-urlencode "description=MR title check failed" \
                  --data-urlencode "target_url=${BUILD_URL:-}" \
                  "${GITLAB_BASE}/api/v4/projects/${PROJECT_ENC}/statuses/${SHA}" || true
              fi

              curl -sS -X POST -H "PRIVATE-TOKEN: ${GIT_PASS}" \
                --data-urlencode "body=${MSG}" \
                "${GITLAB_BASE}/api/v4/projects/${PROJECT_ENC}/merge_requests/${IID}/notes" || true

              curl -sS -X PUT -H "PRIVATE-TOKEN: ${GIT_PASS}" \
                --data-urlencode "state_event=close" \
                "${GITLAB_BASE}/api/v4/projects/${PROJECT_ENC}/merge_requests/${IID}" || true

              exit 3
            fi

            echo "$TITLE" | grep -Pq "$MR_TITLE_COMMON" || { echo "❌ Not matching common pattern"; exit 4; }
            echo "✅ MR title OK ($KIND)"
          '''
        }
      }  
    }

    stage('Report status: success to GitLab') {
      when {
        expression {
            (env.GL_MR_ACTION ?: "") != "merge"
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: env.GIT_CREDS_HTTPS,
                                          usernameVariable: 'GIT_USER',
                                          passwordVariable: 'GIT_PASS')]) {
          sh '''
            set -eu
            SHA="${GL_MR_SHA:-}"
            [ -n "$SHA" ] || { echo "No MR last_commit SHA to report"; exit 0; }

            CONTEXT="jenkins:mr-title-check"
            DESC="MR title check passed"
            TARGET="${BUILD_URL:-}"

            PROJECT_ENC="$(printf '%s' "${GL_PROJECT:-}" | sed 's#/#%2F#g')"

            curl -sS -X POST \
              -H "PRIVATE-TOKEN: ${GIT_PASS}" \
              --data-urlencode "state=success" \
              --data-urlencode "context=${CONTEXT}" \
              --data-urlencode "description=${DESC}" \
              --data-urlencode "target_url=${TARGET}" \
              "${GITLAB_BASE}/api/v4/projects/${PROJECT_ENC}/statuses/${SHA}" \
              -w "\\nHTTP %{http_code}\\n"
          '''

          script {
            if (env.GL_MR_ACTION == 'update') {
              env.SKIP_POST_SUCCESS = 'true'

              currentBuild.result = 'SUCCESS'
              throw new org.jenkinsci.plugins.workflow.steps.FlowInterruptedException(hudson.model.Result.SUCCESS)
            }
          }
        }
      }
    }

    stage('Prepare repo(develop)') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.DEVELOP_BRANCH)
        }
      }
      steps {
        checkout([$class:'GitSCM',
          branches: [[name: "*/${env.DEVELOP_BRANCH}"]],
          userRemoteConfigs: [[
            url: env.GIT_URL_HTTPS,
            credentialsId: env.GIT_CREDS_HTTPS,
            refspec: '+refs/heads/develop:refs/remotes/origin/develop'
          ]],
          extensions: [[$class:'CloneOption', shallow:true, depth:1, timeout:10]]
        ])
      }
    }

    stage('Prepare .env.ai') {
      steps {
        withCredentials([file(credentialsId: 'ENV_AI_FILE', variable: 'ENV_AI_FILE')]) {
          sh '''
            set -eu
            install -m 600 "$ENV_AI_FILE" deploy/.env.ai
          '''
        }
      }
    }

    stage('Prepare .env.dev.ai') {
      steps {
        withCredentials([file(credentialsId: 'ENV_AI_DEV_FILE', variable: 'ENV_AI_DEV_FILE')]) {
          sh '''
            set -eu
            install -m 600 "$ENV_AI_DEV_FILE" deploy/.env.dev.ai
          '''
        }
      }
    }

    stage('Prepare .env.dev') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.DEVELOP_BRANCH)
        }
      }
      steps {
        withCredentials([file(credentialsId: 'ENV_DEV_FILE', variable: 'ENV_DEV_FILE')]) {
          sh '''
            set -eu
            install -m 600 "$ENV_DEV_FILE" deploy/.env.dev
          '''
        }
      }
    }

    stage('dev Deploy (compose up)') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.DEVELOP_BRANCH)
        }
      }
      steps {
        script {
          def dev_source = env.GL_MR_SOURCE ?: ""

          if (dev_source.contains("/be/") || dev_source.contains("/BE/")) {
              sh ''' 
                set -eux

                docker compose --env-file deploy/.env.dev -f "$COMPOSE_DEV_FILE" pull || true
                docker compose --env-file deploy/.env.dev -f "$COMPOSE_DEV_FILE" up -d --build klp_back_dev
              '''
          } else if (dev_source.contains("/fe/") || dev_source.contains("/FE/")) {
              sh ''' 
                set -eux

                docker compose --env-file deploy/.env.dev -f "$COMPOSE_DEV_FILE" pull || true
                docker compose --env-file deploy/.env.dev -f "$COMPOSE_DEV_FILE" up -d --build klp_front_dev
              '''
          } else if (dev_source.contains("/ai/") || dev_source.contains("/AI/")) {
              sh ''' 
                set -eux

                docker compose --env-file deploy/.env.dev.ai -f "$COMPOSE_AI_FILE" pull || true
                docker compose --env-file deploy/.env.dev.ai -f "$COMPOSE_AI_FILE" up -d --build klp_ai_dev
              '''
          } else {
              echo "No deploy target matched for source branch: ${dev_source}"
          }
        }
      }
    }

    // release 브랜치에 병합 시에만 레포 준비(체크아웃) → 태그 푸시용
    stage('Prepare repo (only on merge)') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.RELEASE_BRANCH)
        }
      }
      steps {
        checkout([$class:'GitSCM',
          branches: [[name: "*/${GL_MR_TARGET}"]],
          userRemoteConfigs: [[
            url: env.GIT_URL_HTTPS,
            credentialsId: env.GIT_CREDS_HTTPS,
            refspec: '+refs/heads/*:refs/remotes/origin/* +refs/tags/*:refs/tags/*'
          ]],
          extensions: [[$class:'CloneOption', shallow:true, depth:1, timeout:10]]
        ])
      }
    }

    stage('Tag on merge (title-based version)') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" && (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.RELEASE_BRANCH)
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: env.GIT_CREDS_HTTPS,
                                          usernameVariable: 'GIT_USER',
                                          passwordVariable: 'GIT_PASS')]) {
          sh '''
            set -eu
            . ./.conventions.env
            TITLE="${GL_MR_TITLE:-}"
            TGT="${GL_MR_TARGET:-}"
            IID="${GL_MR_IID:-}"

            echo "$TITLE" | grep -Pq "$MR_TITLE_REL" && IS_REL=1 || IS_REL=0
            VERSION="$( echo "$TITLE" | grep -Po "$RELEASE_REGEX" | head -n1 || true )"
            if [ "$IS_REL" -eq 1 ] && [ -z "${VERSION:-}" ]; then
              echo "ℹ️ release MR but no semantic version in title; using fallback"
            fi
            [ -n "${VERSION:-}" ] || VERSION="release-$(date +%Y%m%d)-!${IID}"
            echo "📦 Tagging: ${VERSION}"

            git config user.email "9526yu@naver.com"
            git config user.name  "jungseokyu"

            BASE_URL="${GIT_URL_HTTPS}"
            case "$BASE_URL" in
              https://*) : ;;
              https//*)   BASE_URL="https://${BASE_URL#https//}";;
              *)          BASE_URL="https://${BASE_URL#https://}";;
            esac
            AUTH_URL="https://${GIT_USER}:${GIT_PASS}@${BASE_URL#https://}"

            if [ ! -d .git ]; then
              git init
              git remote add origin "$AUTH_URL"
            else
              git remote set-url origin "$AUTH_URL"
            fi
            echo "🔗 origin -> $(git remote get-url origin)"

            if ! git ls-remote --heads origin "${TGT}" >/dev/null 2>&1; then
              echo "❌ Remote branch 'origin/${TGT}' does not exist."
              exit 7
            fi

            if git rev-parse --is-shallow-repository >/dev/null 2>&1 && \
              [ "$(git rev-parse --is-shallow-repository)" = "true" ]; then
              git fetch --unshallow origin || true
            fi

            git fetch --prune origin \
              "+refs/heads/${TGT}:refs/remotes/origin/${TGT}" \
              "+refs/tags/*:refs/tags/*"

            git checkout -B "${TGT}" "origin/${TGT}"

            if git show-ref --tags --verify --quiet "refs/tags/${VERSION}"; then
              echo "⚠️ Tag ${VERSION} already exists; skip"
              exit 0
            fi

            git tag -a "${VERSION}" -m "MR !${IID} merged to ${TGT} (${VERSION})"
            git push origin "refs/tags/${VERSION}"
            echo "✅ Tag pushed: ${VERSION}"
          '''
        }
      }
    }

    stage('Prepare .env.prod') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.RELEASE_BRANCH)
        }
      }
      steps {
        withCredentials([file(credentialsId: 'ENV_PROD_FILE', variable: 'ENV_PROD_FILE')]) {
          sh '''
            set -eu
            install -m 600 "$ENV_PROD_FILE" deploy/.env.prod
          '''
        }
      }
    }

    stage('prod Deploy (compose up)') {
      when {
        expression {
          ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
          (env.GL_MR_TARGET == env.RELEASE_BRANCH)
        }
      }
      steps {
        script {
          sh ''' 
          set -eux

          docker compose --env-file deploy/.env.prod -f "$COMPOSE_PROD_FILE" pull || true
          docker compose --env-file deploy/.env.ai -f "$COMPOSE_AI_FILE" pull || true
          docker compose --env-file deploy/.env.prod -f "$COMPOSE_PROD_FILE" up -d --build klp_back
          docker compose --env-file deploy/.env.prod -f "$COMPOSE_PROD_FILE" up -d --build klp_front
          docker compose --env-file deploy/.env.ai -f "$COMPOSE_AI_FILE" up -d --build klp_ai
          '''
        }
      }
    }
      
    // stage('Docker Image Push to DockerHub') {
    //   when {
    //     expression {
    //       ((env.GL_MR_ACTION ?: "") == "merge" || (env.GL_MR_STATE ?: "") == "merged") &&
    //       (env.GL_MR_TARGET == env.RELEASE_BRANCH)
    //     }
    //   }
    //   environment {
    //     // 커밋 해시(12자리) 기준 태깅, 없으면 manual
    //     IMG_SHA = "${(env.GL_MR_SHA ?: env.GIT_COMMIT ?: 'manual').take(12)}"
    //   }
    //   steps {
    //     withCredentials([
    //       usernamePassword(
    //         credentialsId: 'dockerhub-creds',
    //         usernameVariable: 'DOCKER_USER',
    //         passwordVariable: 'DOCKER_PASS'
    //       )
    //     ]) {
    //       script {
    //         // 서비스별 정의 (경로/도커파일/이미지명)
    //         def targets = [
    //           [name: 'backend',  ctx: 'Backend/SecondBrain',   df: 'Dockerfile', image: 'seok1419/klp-backend'],
    //           [name: 'frontend', ctx: 'Frontend',     df: 'Dockerfile', image: 'seok1419/klp-frontend'],
    //           // [name: 'ai',       ctx: 'AI',  df: 'Dockerfile', image: 'seok1419/klp-ai']
    //         ]

    //         sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
            
    //         targets.each { t ->
    //           sh """
    //             set -eu
    //             export DOCKER_BUILDKIT=1

    //             # 경로/파일 존재 확인
    //             test -d "${t.ctx}"
    //             test -f "${t.ctx}/${t.df}"

    //             docker build --pull \
    //               -t "${t.image}:${IMG_SHA}" \
    //               -f "${t.ctx}/${t.df}" \
    //               "${t.ctx}"

    //             docker tag "${t.image}:${IMG_SHA}" "${t.image}:latest"

    //             docker push "${t.image}:${IMG_SHA}"
    //             docker push "${t.image}:latest"
    //           """
    //         }
    //         sh '''
    //           docker logout || true
    //           docker image prune -f || true
    //         '''
    //       }
    //     }
    //   }
    // }
  }

  post {
    success {
      withCredentials([string(credentialsId: 'MM_WEBHOOK', variable: 'MM_WEBHOOK')]) {
        script{
          if (env.SKIP_POST_SUCCESS == 'true') {
            echo 'update action - skip post success'
            return
          }

          // 브랜치 이름에 따라 알림 다르게
          def source = env.GL_MR_SOURCE ?: ""
          def category = ""

          if (source.contains("/be/") || source.contains("/BE/")) {
              category = "backend"
          } else if (source.contains("/fe/") || source.contains("/FE/")) {
              category = "frontend"
          } else {
              category = "infra"
          }

          if ((env.GL_MR_ACTION == 'merge' && env.GL_MR_STATE == 'merged') && (env.GL_MR_TARGET == 'develop')) {
            if (category == "backend") {
              mattermostSend(
                endpoint: MM_WEBHOOK,
                color: 'good',
                message: """
#### :homer_bush: Successfully Merged :homer_bush:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Pipeline Success!
---
##### 서비스 점검하러 가기
:springboot: [Backend Spring Server]()
"""
              )
            } else if (category == "frontend") {
              mattermostSend(
                endpoint: MM_WEBHOOK,
                color: 'good',
                message: """
#### :homer_bush: Successfully Merged :homer_bush:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Pipeline Success!
---
##### 서비스 점검하러 가기
:react: [Frontend React Server]()
"""
              )
            } else {
              mattermostSend(
                endpoint: MM_WEBHOOK,
                color: 'good',
                message: """
#### :homer_bush: Successfully Merged :homer_bush:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Pipeline Success!
"""
              )
            }
          } else if ((env.GL_MR_ACTION == 'merge' || env.GL_MR_STATE == 'merged') && (env.GL_MR_TARGET == env.RELEASE_BRANCH)) {
            mattermostSend(
                endpoint: MM_WEBHOOK,
                color: 'good',
                message: """
#### :homer_bush: Successfully Merged :homer_bush:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Pipeline Success!
---
##### 서비스 점검하러 가기
:springboot: [Backend Spring Server]()
:react: [Frontend React Server]()
"""
              )
          } else {
            mattermostSend(
                endpoint: MM_WEBHOOK,
                color: 'good',
                message: """
#### :green_frog: `${env.GL_USER_NAME ?: 'E104'}` MR Generated!!!!!!!!! :green_frog:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
*서둘러서 코드 리뷰 해주세요~! 수정 필요할 경우 작성자 태그해주세요!!*
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}
:eyes_5s: Reviewer: @${env.GL_REVIEWER ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Pipeline Success!
"""
            )
          }
        }
      }
      echo "✅ MR success by seok."
    }

    failure {
      // 실패 원인 tail은 우리가 쌓아둔 로그 파일이 있으면 그걸 활용. 그런데 가능할지 모르겠음.
      withCredentials([string(credentialsId: 'MM_WEBHOOK', variable: 'MM_WEBHOOK')]) {
        script {
          // 에러나 예외 로그가 담긴 최대 20줄을 뽑아서 메세지에 포함
          def errLogs = sh(
            script: "grep -iE 'error|exception' ${currentBuild.rawBuild.logFile} | tail -n 5",
            returnStdout: true
          ).trim()

          if (!errLogs) {
            errLogs = "No specific error logs found (check console for details)"
          }

          mattermostSend(
              endpoint: MM_WEBHOOK,
              color: 'danger',  
              message: """
#### :x: Jenkins Pipeline Failed :x:

##### [${env.GL_MR_TITLE ?: 'No title'}](${env.GL_MR_URL ?: env.BUILD_URL})
:pencil2: Assignee: @${env.GL_ASSIGNEE ?: '9526yu'}

:gun_cat: **Target**: `${env.GL_MR_TARGET ?: 'develop'}`

##### Error Logs
```
${errLogs}
```
"""
          )
        }
      }
      echo "❌ MR failed."
    }
  }
}