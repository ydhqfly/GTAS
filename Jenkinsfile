def label = "mypod-${UUID.randomUUID().toString()}"
podTemplate(label: label, containers: [
    containerTemplate(name: 'maven', image: 'maven:3.6.0-jdk-8-alpine', ttyEnabled: true, command: 'cat'),
    containerTemplate(name: 'docker', image: 'docker', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.8', command: 'cat', ttyEnabled: true)
  ],
volumes: [
    hostPathVolume(mountPath: '/root/.m2/repository', hostPath: '/root/.m2/repository'),
  hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
]) {

    node(label) {
        def myRepo = checkout scm
        def gitCommit = myRepo.GIT_COMMIT
        def gitBranch = myRepo.GIT_BRANCH
        def branchName = sh(script: "echo $gitBranch | cut -c8-", returnStdout: true)
        def shortGitCommit = "${gitCommit[0..10]}"
        def previousGitCommit = sh(script: "git rev-parse ${gitCommit}~", returnStdout: true)
        def gitCommitCount = sh(script: "git rev-list --all --count", returnStdout: true)
        def regURL = "dockerhub.com/wcogtas/gtas"

        def regNamespace = "wcogtas"
        def artifactID = sh(script: "grep '<artifactId>' pom.xml | head -n 1 | sed -e 's/artifactId//g' | sed -e 's/\\s*[<>/]*//g' | tr -d '\\r\\n'", returnStdout: true)
        def POMversion = sh(script: "grep '<version>' pom.xml | head -n 1 | sed -e 's/version//g' | sed -e 's/\\s*[<>/]*//g' | tr -d '\\r\\n'", returnStdout: true)
 
        try {
        notifySlack()

        stage('Maven project') {
            container('maven') {

                stage('Validate') {
                    sh 'mvn -f gtas-parent/ -B  validate'        
                }
                
                stage('Compile') {
                    sh 'mvn -B  -f gtas-parent/ compile  -Dmaven.test.failure.ignore=true'
                }
                
                stage('Unit Test') {
                    sh 'mvn -B -f gtas-parent/ test -Dmaven.test.failure.ignore=true'
                }
                            
                stage ('Code Analysis') {
                    withSonarQubeEnv {
                        sh "mvn -f gtas-parent/ jacoco:report test pmd:pmd findbugs:findbugs checkstyle:checkstyle   package sonar:sonar -Dmaven.test.failure.ignore=true"
                    }
                 //   junit 'gtas-parent/target/surefire-reports/*.xml'
                }
                
                
             //   stage('Publish test results') {
             //       junit 'gtas-parent/target/surefire-reports/*.xml'
             //   } 
                
                
                stage('Security Scan components') {
                   sh 'mvn -B -f gtas-parent/ install org.owasp:dependency-check-maven:check  -Dmaven.test.failure.ignore=true'
               }
                
            }
        }
        stage('Create Docker images') {
          container('docker') {
         withCredentials([[$class: 'UsernamePasswordMultiBinding',
           credentialsId: 'registry',
           usernameVariable: 'DOCKER_REG_USER',
           passwordVariable: 'DOCKER_REG_PASSWORD']]) {
          sh """
            docker login -u ${DOCKER_REG_USER}  -p ${DOCKER_REG_PASSWORD}
            echo ${regNamespace}/${artifactID}
            cd gtas-parent/gtas-job-scheduler-war/
            docker build -t wcogtas/gtas .
            # docker build -t ${regNamespace}/${artifactID} .
            docker tag wcogtas/gtas wcogtas/gtas:0.1.0.${shortGitCommit}
            docker tag wcogtas/gtas wcogtas/gtas:0.1.0.${gitCommitCount}
            docker push wcogtas/gtas
            """
         }
      }
    }
 
    
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        notifySlack(currentBuild.result)
    }
    }
}

def notifySlack(String buildStatus = 'STARTED') {
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'

    def color

    if (buildStatus == 'STARTED') {
        color = '#D4DADF'
    } else if (buildStatus == 'SUCCESS') {
        color = '#BDFFC3'
    } else if (buildStatus == 'UNSTABLE') {
        color = '#FFFE89'
    } else {
        color = '#FF9FA1'
    }

    def msg = "${buildStatus}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}"

  //  slackSend(color: color, message: msg)
 }
