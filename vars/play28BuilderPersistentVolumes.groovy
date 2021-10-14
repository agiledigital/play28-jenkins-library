def call(Map config) {
  return [
    [
      path: '/home/jenkins/.ivy2',
      claimName: "${config.project}-home-jenkins-ivy2",
      sizeGiB: 2
    ],
    [
      path: '/home/jenkins/.sbt',
      claimName: "${config.project}-home-jenkins-sbt",
      sizeGiB: 2
    ]
  ]
}