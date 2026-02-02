set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building project..."
cd "$PROJECT_DIR"
mvn -q -DskipTests package
mvn -q dependency:copy-dependencies -DoutputDirectory=target/dependency

echo "Running application..."
java -Dfile.encoding=UTF-8 \
  -cp "target/classes;target/dependency/*;src/libs/Stemmer.jar" \
  client.GUI
