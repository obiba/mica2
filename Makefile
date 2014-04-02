skipTests=true
mvn_exec=mvn -Dmaven.test.skip=${skipTests}

all:
	${mvn_exec} clean install

core:
	cd mica-core && ${mvn_exec} install

rest:
	cd mica-rest && ${mvn_exec} install

run:
	cd mica-webapp && ${mvn_exec} spring-boot:run