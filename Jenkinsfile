#!/usr/bin/env groovy

library identifier: 'apm@master', 
retriever: modernSCM(
  [$class: 'GitSCMSource', 
  credentialsId: 'f6c7695a-671e-4f4f-a331-acdce44ff9ba', 
  remote: 'git@github.com:elastic/apm-pipeline-library.git'])

pipeline {
  agent any
  environment {
    HOME = "${env.HUDSON_HOME}"
    BASE_DIR="src/github.com/elastic/apm-agent-go"
    JOB_GIT_CREDENTIALS = "f6c7695a-671e-4f4f-a331-acdce44ff9ba"
  }
  triggers {
    cron('0 0 * * 1-5')
    githubPush()
  }
  options {
    timeout(time: 1, unit: 'HOURS') 
    buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '2', daysToKeepStr: '30'))
    timestamps()
    preserveStashes()
    ansiColor('xterm')
    skipDefaultCheckout(true)
  }
  parameters {
    string(name: 'branch_specifier', defaultValue: "", description: "the Git branch specifier to build (<branchName>, <tagName>, <commitId>, etc.)")
    string(name: 'JOB_INTEGRATION_TEST_BRANCH_SPEC', defaultValue: "refs/heads/master", description: "the Git branch specifier to make the integrations test")
    string(name: 'JOB_HEY_APM_TEST_BRANCH_SPEC', defaultValue: "refs/heads/master", description: "the Git branch specifier to make the Hey APM test")      
    string(name: 'ELASTIC_STACK_VERSION', defaultValue: "6.4", description: "Elastic Stack version used for integration test (master, 6.3, 6.4, ...)")          
    
    booleanParam(name: 'linux_ci', defaultValue: true, description: 'Enable Linux build')
    booleanParam(name: 'test_ci', defaultValue: true, description: 'Enable test')
    booleanParam(name: 'integration_test_ci', defaultValue: true, description: 'Enable run integgration test')
    booleanParam(name: 'bench_ci', defaultValue: true, description: 'Enable benchmarks')
    booleanParam(name: 'doc_ci', defaultValue: true, description: 'Enable build documentation')
  }
  
  stages {
    /**
     Checkout the code and stash it, to use it on other stages.
    */
    stage('Checkout') {
      agent { label 'master || linux' }
      environment {
        PATH = "${env.PATH}:${env.HUDSON_HOME}/go/bin/:${env.WORKSPACE}/bin"
        GOPATH = "${env.WORKSPACE}"
      }
      
      steps {
        withEnvWrapper() {
            dir("${BASE_DIR}"){
              script{
                if(!branch_specifier){
                  echo "Checkout SCM ${BRANCH_NAME} - ${CHANGE_ID}"
                  checkout scm
                } else {
                  echo "Checkout ${branch_specifier}"
                  checkout([$class: 'GitSCM', branches: [[name: "${branch_specifier}"]], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [], 
                    submoduleCfg: [], 
                    userRemoteConfigs: [[credentialsId: "${JOB_GIT_CREDENTIALS}", 
                    url: "${GIT_URL}"]]])
                }
                env.JOB_GIT_COMMIT = getGitCommitSha()
                env.JOB_GIT_URL = "${GIT_URL}"
                
                github_enterprise_constructor()
                
                currentBuild.changeSets.each{ change -> println change}

                on_change{
                  echo "build cause a change (commit or PR)"
                }
                
                on_commit {
                  echo "build cause a commit"
                }
                
                on_merge {
                  echo "build cause a merge"
                }
                
                on_pull_request {
                  echo "build cause PR"
                }

                sh("git tag -a '${BUILD_TAG}' -m 'Jenkins TAG ${RUN_DISPLAY_URL}'")
                sh("git push git@github.com:${ORG_NAME}/${REPO_NAME}.git --tags")
                
                sh "export"
              }
            }
            stash allowEmpty: true, name: 'source'
        }
      }
    }
    
    /**
    Build on a linux environment.
    */
    stage('build') { 
      agent { label 'linux' }
      environment {
        PATH = "${env.PATH}:${env.HUDSON_HOME}/go/bin/:${env.WORKSPACE}/bin"
        GOPATH = "${env.WORKSPACE}"
      }
      
      when { 
        beforeAgent true
        environment name: 'linux_ci', value: 'true' 
      }
      steps {
        withEnvWrapper() {
          unstash 'source'
          dir("${BASE_DIR}"){    
            sh """#!/bin/bash
            make install check
            """
          }
        }
      }
    }
    stage('test') { 
      agent { label 'linux' }
      environment {
        PATH = "${env.PATH}:${env.HUDSON_HOME}/go/bin/:${env.WORKSPACE}/bin"
        GOPATH = "${env.WORKSPACE}"
      }
      
      when { 
        beforeAgent true
        environment name: 'test_ci', value: 'true' 
      }
      steps {
        withEnvWrapper() {
          unstash 'source'
          dir("${BASE_DIR}"){    
            sh """#!/bin/bash
            ./scripts/jenkins/test.sh
            """
          }
        }
      }
      post { 
        always { 
          publishHTML(target: [
              allowMissing: true, 
              keepAll: true,
              reportDir: "${BASE_DIR}/build", 
              reportFiles: 'coverage-*-report.html', 
              reportName: 'coverage report', 
              reportTitles: 'Coverage'])
          publishCoverage(adapters: [
            coberturaAdapter("${BASE_DIR}/build/coverage-*-report.xml")], 
            sourceFileResolver: sourceFiles('STORE_ALL_BUILD'))
          cobertura(autoUpdateHealth: false, 
            autoUpdateStability: false, 
            coberturaReportFile: "${BASE_DIR}/build/coverage-*-report.xml", 
            conditionalCoverageTargets: '70, 0, 0', 
            failNoReports: false, 
            failUnhealthy: false, 
            failUnstable: false, 
            lineCoverageTargets: '80, 0, 0', 
            maxNumberOfBuilds: 0, 
            methodCoverageTargets: '80, 0, 0', 
            onlyStable: false, 
            sourceEncoding: 'ASCII', 
            zoomCoverageChart: false)
          archiveArtifacts(allowEmptyArchive: true, 
            artifacts: "${BASE_DIR}/build/junit-*.xml", 
            onlyIfSuccessful: false)
          junit(allowEmptyResults: true, 
            keepLongStdio: true, 
            testResults: "${BASE_DIR}/build/junit-*.xml")
        }
      }
    }
    stage('Benchmarks') { 
      agent { label 'linux' }
      environment {
        PATH = "${env.PATH}:${env.HUDSON_HOME}/go/bin/:${env.WORKSPACE}/bin"
        GOPATH = "${env.WORKSPACE}"
      }
      
      when { 
        beforeAgent true
        allOf { 
          branch 'master';
          environment name: 'bench_ci', value: 'true' 
        }
      }
      steps {
        withEnvWrapper() {
          unstash 'source'
          dir("${BASE_DIR}"){    
            sh """#!/bin/bash
            ./scripts/jenkins/bench.sh
            """
          }
        }
      }
    }
    stage('Docker tests') { 
      agent { label 'linux && docker' }
      environment {
        PATH = "${env.PATH}:${env.HUDSON_HOME}/go/bin/:${env.WORKSPACE}/bin"
        GOPATH = "${env.WORKSPACE}"
      }
      
      when { 
        beforeAgent true
        allOf { 
          branch 'master';
          environment name: 'integration_test_ci', value: 'true' 
        }
      }
      steps {
        withEnvWrapper() {
          unstash 'source'
          dir("${BASE_DIR}"){    
            sh """#!/bin/bash
            ./scripts/jenkins/docker-test.sh
            """
          }
        }
      }
    }
  }
  post {
    always { 
      echo 'Post Actions'
      dir('cleanTags'){
        unstash 'source'
        sh("""
        git fetch --tags
        git tag -d '${BUILD_TAG}'
        git push git@github.com:${ORG_NAME}/${REPO_NAME}.git --tags
        """)
        deleteDir()
      }
    }
    success { 
      echo 'Success Post Actions'
      updateGithubCommitStatus(
        repoUrl: "${JOB_GIT_URL}",
        commitSha: "${JOB_GIT_COMMIT}",
        message: "## :green_heart: Build Succeeded\n [${JOB_NAME}](${BUILD_URL})",
      )
    }
    aborted { 
      echo 'Aborted Post Actions'
      setGithubCommitStatus(repoUrl: "${JOB_GIT_URL}",
        commitSha: "${JOB_GIT_COMMIT}",
        message: "## :broken_heart: Build Aborted\n [${JOB_NAME}](${BUILD_URL})",
        state: "error")
    }
    failure { 
      echo 'Failure Post Actions'
      //step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: "${NOTIFY_TO}", sendToIndividuals: false])
      setGithubCommitStatus(repoUrl: "${JOB_GIT_URL}",
        commitSha: "${JOB_GIT_COMMIT}",
        message: "## :broken_heart: Build Failed\n [${JOB_NAME}](${BUILD_URL})",
        state: "failure")
    }
    unstable { 
      echo 'Unstable Post Actions'
      setGithubCommitStatus(repoUrl: "${JOB_GIT_URL}",
        commitSha: "${JOB_GIT_COMMIT}",
        message: "## ::yellow_heart: Build Unstable\n [${JOB_NAME}](${BUILD_URL})",
        state: "error")
    }
  }
}