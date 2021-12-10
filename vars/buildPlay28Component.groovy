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

  def fullComponentName = "${config.project}-${config.component}"
  def buildVersion = config.buildNumber
  def modulePath = "${config.baseDir}/modules/${config.subPath}"

  container("play28-builder-${config.index}") {

    stage('Build Details') {
      echo "Project:   ${config.project}"
      echo "Component: ${config.component}"
      echo "BuildNumber: ${config.buildNumber}"
      echo "Module: ${config.module}"
    }

    stage('Prepare environment') {
      writeFile(file: "/home/jenkins/sbt.boot.properties", 
        text: libraryResource('au/com/agiledigital/jenkins-pipelines/build-sbt-play28/sbt.boot.properties'))
    }

    stage('Fetch dependencies') {
      sh 'ls *.conf'
      sbt "update"
    }

    stage('Compile') {
      sbt "compile"
    }
    try {
      stage('Test') {
        sbt ";project ${config.get('module', config.component)}; testOnly ** -- junitxml console"
        junit "${config.baseDir}/modules/${config.subPath}/target/test-reports/*.xml"
      }
    } catch (e) {
      // If there was an exception thrown, the build failed
      currentBuild.result = "UNSTABLE"
    }
  }

  if(config.stage == 'dist') { 
    container("play28-builder-${config.index}") {
      stage('Inject configuration') {
        // TODO: Allow ${SETTINGS_CONTEXT} to be overriden
          // From https://stash.agiledigital.com.au/projects/MCP/repos/docker-builder/browse/builders/play2-multi-build/build.sh
        sh """
        |# Insert the project.conf, environment.conf, etc into the deployable.
        |cp *.conf "${modulePath}/conf"
        |
        |cat "${modulePath}/conf/combined.conf"
        |
        |""".stripMargin()
      }
      stage('Package') {
        sbt ";project ${config.get('module', config.component)}; set name := \"${fullComponentName}\"; set version := \"${buildVersion}\"; dist"
      }
    }

    stage('Archive to Jenkins') {
      def tarName = "${fullComponentName}-${buildVersion}.tar.gz"
      // Re-compress dist zip file as tar gzip without top level folder
      sh "unzip ${modulePath}/target/universal/${fullComponentName}-${buildVersion}.zip"
      // Remove dist .bat, and rename main executable to have a generic name
      sh "rm \"${fullComponentName}-${buildVersion}/bin/${fullComponentName}.bat\""
      sh "mv \"${fullComponentName}-${buildVersion}/bin/${fullComponentName}\" \"${fullComponentName}-${buildVersion}/bin/dist\""
      sh "tar -czvf \"${tarName}\" -C \"${fullComponentName}-${buildVersion}\" ."
      archiveArtifacts tarName
    }
  }
}