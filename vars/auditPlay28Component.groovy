/*
 * Toolform-compatible Jenkins 2 Pipeline build step for Play Framework 2.8 based components built using SBT
 */
 
def call(Map config) {
  final sbt = { cmd ->
    ansiColor('xterm') {
      dir(config.baseDir) {
        // todo: configure these in a single place referred to in persistent volumes template
        sh "sbt -batch -sbt-dir /home/jenkins/.sbt -Dsbt.repository.config=/home/jenkins/sbt.boot.properties -Dsbt.ivy.home=/home/jenkins/.ivy2/ -Divy.home=/home/jenkins/.ivy2/ -v \'${cmd}\'"
      }
    }
  }

  def buildVersion = config.buildNumber

  container('play28-builder-0') {

    stage('Audit Run Details') {
      echo "Project:   ${config.project}"
      echo "BuildNumber: ${config.buildNumber}"
    }
    stage('Fetch dependencies') {
      sbt "update"
    }

    stage('Dependency Check') {
      sbt "dependencyCheck"
      sbt "dependencyCheckAggregate"
    }


    stage('Archive to Jenkins') {
      archiveArtifacts "${config.baseDir}/target/scala-2.13/dependency-check-report.html"
      slackUploadFile filePath: "${config.baseDir}/target/scala-2.13/dependency-check-report.html", initialComment:  "${config.project} audit html file", channel: config.slackThead.threadId
      
      def tarName = "audits-sbt-${config.project}-${config.buildNumber}.tar.gz"
      sh "tar -czvf \"${tarName}\" `find ./ -name \"dependency-check-report.*\"`"

      slackUploadFile filePath: tarName, initialComment:  "${config.project} audit jsonl file:\"${tarName}\"", channel: config.slackThead.threadId
      archiveArtifacts tarName
    }
  }
}