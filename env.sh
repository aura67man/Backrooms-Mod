# Source this before running Gradle:  . ./env.sh
# Keeps the JDK and every Gradle cache inside the project folder.
_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[ -x "$_here/.jdk25/bin/java" ] && export JAVA_HOME="$_here/.jdk25" && export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME="$_here/.gradle-home"
