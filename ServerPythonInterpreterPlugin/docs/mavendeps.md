mvn -f tools/graalpy-deps/pom.xml dependency:copy-dependencies `
  -DincludeScope=runtime `
  -DoutputDirectory=lib-common

Get-ChildItem lib-common -Filter "*-sources.jar","*-javadoc.jar" | Remove-Item -Force
