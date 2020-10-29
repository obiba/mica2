skipTests = false
version=3.1-SNAPSHOT
mvn_exec = mvn -Dmaven.test.skip=${skipTests}
current_dir = $(shell pwd)
search_plugin_version=

ifdef MICA_HOME
	mica_home = ${MICA_HOME}
else
	mica_home = ${current_dir}/mica-webapp/target/mica_home
endif
mica_log = ${mica_home}/logs

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
	@echo "  proto       : Install web-model module"
	@echo "  rest        : Install rest module"
	@echo
	@echo "  run         : Run webapp module"
	@echo "  debug       : Debug webapp module on port 8002"
	@echo "  grunt       : Start grunt on port 9000"
	@echo "  npm-install : Download all NodeJS dependencies"
	@echo
	@echo "  clear-log   : Delete logs from ${mica_log}"
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

spi:
	cd mica-spi && ${mvn_exec} install

search:
	cd mica-search && ${mvn_exec} install

proto: model

model:
	cd mica-web-model && ${mvn_exec} install

python:
	cd ../mica-python-client && ${mvn_exec} install

rest:
	cd mica-rest && ${mvn_exec} install

log:
	tail -f ${mica_home}/logs/mica.log

restlog:
	tail -f ${mica_home}/logs/rest.log

seed:
	mkdir -p mica-webapp/target/mica_home/seed/in && \
	cp mica-core/src/test/resources/seed/studies.json mica-webapp/target/mica_home/seed/in

plugins:
	rm -rf ${mica_home}/plugins/* && \
	mkdir -p ${mica_home}/plugins && \
	cd ../mica-search-es${search_plugin_version} && mvn clean install && \
	cp target/mica-search-es${search_plugin_version}-*-dist.zip ${mica_home}/plugins
#	cd ${mica_home}/plugins && unzip *zip && rm *zip

run:
	cd mica-webapp && \
	${mvn_exec} spring-boot:run -Pdev -DMICA_HOME="${mica_home}" -DMICA_LOG="${mica_log}"

run-prod:
	cd mica-webapp && \
	mvn install -Pci-build && \
	cd ../mica-dist && \
	mvn clean package && \
	cd target && \
	unzip mica-dist-${version}-dist.zip && \
	mkdir mica_home && \
	mv mica-dist-${version}/conf mica_home/conf && \
	export MICA_HOME="${current_dir}/mica-dist/target/mica_home" && \
	mica-dist-${version}/bin/mica2

debug:
	export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,address=8002,suspend=n && \
	cd mica-webapp && \
	${mvn_exec} spring-boot:run -Pdev -Dspring.profiles.active=dev -DMICA_HOME="${mica_home}" -DMICA_LOG="${mica_log}"

run-python:
	cd ../mica-python-client/target/mica-python/bin && \
	chmod +x ./scripts/mica && \
	export PYTHONPATH=${current_dir}/../mica-python-client/target/mica-python/bin && \
	./scripts/mica ${args}

grunt:
	cd mica-webapp && \
	grunt server

npm-install:
	cd mica-webapp && \
	npm install

clear-log:
	rm -rf ${mica_log}

drop-mongo:
	mongo mica --eval "db.dropDatabase()"

drop-data: drop-mongo
	rm -rf ${mica_home}

dependencies-tree:
	mvn dependency:tree

dependencies-update:
	mvn versions:display-dependency-updates

plugins-update:
	mvn versions:display-plugin-updates

elasticsearch-head:
	rm -rf .work/elasticsearch-head && \
	mkdir -p .work && \
	cd .work && \
	git clone git://github.com/mobz/elasticsearch-head.git
	@echo
	@echo "ElasticSearch-Head is available at:"
	@echo "file://${current_dir}/.work/elasticsearch-head/index.html"
	@echo

vue-mica-search:
	cd ../vue-mica-search && \
	yarn build-lib && \
	cp dist/* ../mica2/mica-webapp/src/main/webapp/assets/libs/node_modules/vue-mica-search/dist/

rql:
	cd ../epigeny/rql && \
		npm run build && \
		cp dist/* ../../mica2/mica-webapp/src/main/webapp/assets/libs/node_modules/rql/dist/

templates:
	cd mica-webapp && cp -r src/main/resources/_templates/ target/classes/
