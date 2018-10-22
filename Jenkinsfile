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
                    echo "Checkout SCM ${GIT_BRANCH} - ${GIT_COMMIT}"
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
      
      when { 
        beforeAgent true
        environment name: 'linux_ci', value: 'true' 
      }
      steps {
        withEnvWrapper() {
          unstash 'source'
          dir("${BASE_DIR}"){    
            sh """#!/bin/bash
            make build
            """
          }
        }
      }
    }
  }
}