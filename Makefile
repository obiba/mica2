skipTests=true
mvn_exec=mvn -Dmaven.test.skip=${skipTests}

help:
	@echo
	@echo "Mica Server"
	@echo
	@echo "Available make targets:"
	@echo "  all   : Clean & install all modules"
	@echo "  core  : Install core module"
	@echo "  rest  : Install rest module"
	@echo "  run   : Run webapp module"
	@echo "  debug : Debug webapp module on port 8000"
	@echo
	@echo "  dependencies-tree   : Displays the dependency tree"
	@echo "  dependencies-update : Check for new dependency updates"
	@echo "  plugins-update      : Check for new plugin updates"
	@echo



all:
	${mvn_exec} clean install

core:
	cd mica-core && ${mvn_exec} install

rest:
	cd mica-rest && ${mvn_exec} install

run:
	cd mica-webapp && ${mvn_exec} spring-boot:run

debug:
	export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n && \
	cd mica-webapp && ${mvn_exec} spring-boot:run

dependencies-tree:
	mvn dependency:tree

dependencies-update:
	mvn versions:display-dependency-updates

plugins-update:
	mvn versions:display-plugin-updates