skipTests = false
version=0.1-SNAPSHOT
mvn_exec = mvn -Dmaven.test.skip=${skipTests}
current_dir = $(shell pwd)
mica_server_home = ${current_dir}/mica-webapp/target/mica_server_home
mica_server_log = ${mica_server_home}/logs

help:
	@echo
	@echo "Mica Server"
	@echo
	@echo "Available make targets:"
	@echo "  all         : Clean & install all modules"
	@echo "  clean       : Clean all modules"
	@echo "  install     : Install all modules"
	@echo "  core        : Install core module"
	@echo "  search      : Install search module"
	@echo "  rest        : Install rest module"
	@echo
	@echo "  run         : Run webapp module"
	@echo "  debug       : Debug webapp module on port 8002"
	@echo "  grunt       : Start grunt on port 9000"
	@echo "  npm-install : Download all NodeJS dependencies"
	@echo
	@echo "  clear-log   : Delete logs from ${mica_server_log}"
	@echo "  drop-mongo  : Drop MongoDB mica database"
	@echo
	@echo "  dependencies-tree   : Displays the dependency tree"
	@echo "  dependencies-update : Check for new dependency updates"
	@echo "  plugins-update      : Check for new plugin updates"
	@echo

all: clean install

clean:
	${mvn_exec} clean

install:
	${mvn_exec} install

core:
	cd mica-core && ${mvn_exec} install

search:
	cd mica-search && ${mvn_exec} install

model:
	cd mica-web-model && ${mvn_exec} install

rest:
	cd mica-rest && ${mvn_exec} install

run:
	cd mica-webapp && \
	${mvn_exec} spring-boot:run -Pdev -DMICA_SERVER_HOME="${mica_server_home}" -DMICA_SERVER_LOG="${mica_server_log}"

run-prod:
	cd mica-webapp && \
	mvn install -Pci-build && \
	cd ../mica-dist && \
	mvn clean package && \
	cd target && \
	unzip mica-dist-${version}-dist.zip && \
	mkdir mica_server_home && \
	mv mica-dist-${version}/conf mica_server_home/conf && \
	export MICA_SERVER_HOME="${current_dir}/mica-dist/target/mica_server_home" && \
	mica-dist-${version}/bin/mica-server

debug:
	export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,address=8002,suspend=n && \
	cd mica-webapp && \
	${mvn_exec} spring-boot:run -Pdev -Dspring.profiles.active=dev -DMICA_SERVER_HOME="${mica_server_home}" -DMICA_SERVER_LOG="${mica_server_log}"

grunt:
	cd mica-webapp && \
	grunt server

npm-install:
	cd mica-webapp && \
	npm install

clear-log:
	rm -rf ${mica_server_log}

drop-mongo:
	mongo mica --eval "db.dropDatabase()"

dependencies-tree:
	mvn dependency:tree

dependencies-update:
	mvn versions:display-dependency-updates

plugins-update:
	mvn versions:display-plugin-updates

keystore:
	rm -f keystore.p12
	keytool -genkey -alias tomcat -keystore keystore.p12 -storepass changeit -validity 365 -keyalg RSA -keysize 2048 -storetype pkcs12 -dname "CN=Mica, O=Maelstrom, OU=OBiBa, L=Montreal, ST=Quebec, C=CA"
	@echo "Generated keystore file:" `pwd`/keystore.p12

elasticsearch-head:
	rm -rf .work/elasticsearch-head && \
	mkdir -p .work && \
	cd .work && \
	git clone git://github.com/mobz/elasticsearch-head.git
	@echo  
	@echo "ElasticSearch-Head is available at:" 
	@echo "file://${current_dir}/.work/elasticsearch-head/index.html" 
	@echo  
