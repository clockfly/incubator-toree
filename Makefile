#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.	See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.	You may obtain a copy of the License at
#
#		 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
#

.PHONY: help clean release pip-release

BASE_VERSION=0.2.0.dev1-sean
VERSION=$(BASE_VERSION)-incubating
COMMIT=$(shell git rev-parse --short=12 --verify HEAD)
ifeq (, $(findstring dev, $(VERSION)))
IS_SNAPSHOT?=false
else
IS_SNAPSHOT?=true
SNAPSHOT:=-SNAPSHOT
endif

APACHE_SPARK_VERSION?=2.1.1
SCALA_VERSION?=2.11

define GEN_PIP_PACKAGE_INFO
printf "__version__ = '$(BASE_VERSION)'\n" >> dist/toree-pip/toree/_version.py
printf "__commit__ = '$(COMMIT)'\n" >> dist/toree-pip/toree/_version.py
endef


ENV_OPTS:=APACHE_SPARK_VERSION=$(APACHE_SPARK_VERSION) VERSION=$(VERSION) IS_SNAPSHOT=$(IS_SNAPSHOT)

ASSEMBLY_JAR:=toree-assembly-$(VERSION)$(SNAPSHOT).jar

help:
	@echo '			clean - clean build files'
	@echo '		release - creates packaged distribution'

clean-dist:
	-rm -r dist

clean: VM_WORKDIR=/src/toree-kernel
clean: clean-dist
	$($(ENV_OPTS) sbt clean)
	rm -r `find . -name target -type d`

target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): VM_WORKDIR=/src/toree-kernel
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): ${shell find ./*/src/main/**/*}
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): ${shell find ./*/build.sbt}
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): ${shell find ./project/*.scala} ${shell find ./project/*.sbt}
target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR): dist/toree-legal project/build.properties build.sbt
	$($(ENV_OPTS) sbt root/assembly)

dist/toree/lib: target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR)
	@mkdir -p dist/toree/lib
	@cp target/scala-$(SCALA_VERSION)/$(ASSEMBLY_JAR) dist/toree/lib/.

dist/toree/bin: ${shell find ./etc/bin/*}
	@mkdir -p dist/toree/bin
	@cp -r etc/bin/* dist/toree/bin/.

dist/toree/VERSION:
	@mkdir -p dist/toree
	@echo "VERSION: $(VERSION)" > dist/toree/VERSION
	@echo "COMMIT: $(COMMIT)" >> dist/toree/VERSION

dist/toree-legal/LICENSE: LICENSE etc/legal/LICENSE_extras
	@mkdir -p dist/toree-legal
	@cat LICENSE > dist/toree-legal/LICENSE
	@echo '\n' >> dist/toree-legal/LICENSE
	@cat etc/legal/LICENSE_extras >> dist/toree-legal/LICENSE

dist/toree-legal/NOTICE: NOTICE etc/legal/NOTICE_extras
	@mkdir -p dist/toree-legal
	@cat NOTICE > dist/toree-legal/NOTICE
	@echo '\n' >> dist/toree-legal/NOTICE
	@cat etc/legal/NOTICE_extras >> dist/toree-legal/NOTICE

dist/toree-legal/DISCLAIMER:
	@mkdir -p dist/toree-legal
	@cp DISCLAIMER dist/toree-legal/DISCLAIMER

dist/toree-legal: dist/toree-legal/LICENSE dist/toree-legal/NOTICE dist/toree-legal/DISCLAIMER
	@cp -R etc/legal/licenses dist/toree-legal/.

dist/toree: dist/toree/VERSION dist/toree-legal dist/toree/lib dist/toree/bin RELEASE_NOTES.md
	@cp -R dist/toree-legal/* dist/toree
	@cp RELEASE_NOTES.md dist/toree/RELEASE_NOTES.md

dist: dist/toree

dist/toree-pip/toree-$(BASE_VERSION).tar.gz: DOCKER_WORKDIR=/srv/toree/dist/toree-pip
dist/toree-pip/toree-$(BASE_VERSION).tar.gz: dist/toree
	@mkdir -p dist/toree-pip
	@cp -r dist/toree dist/toree-pip
	@cp dist/toree/LICENSE dist/toree-pip/LICENSE
	@cp dist/toree/NOTICE dist/toree-pip/NOTICE
	@cp dist/toree/DISCLAIMER dist/toree-pip/DISCLAIMER
	@cp dist/toree/VERSION dist/toree-pip/VERSION
	@cp dist/toree/RELEASE_NOTES.md dist/toree-pip/RELEASE_NOTES.md
	@cp -R dist/toree/licenses dist/toree-pip/licenses
	@cp -rf etc/pip_install/* dist/toree-pip/.
	@$(GEN_PIP_PACKAGE_INFO)
	cd dist/toree-pip/; python3.5 setup.py sdist --dist-dir=.

pip-release: dist/toree-pip/toree-$(BASE_VERSION).tar.gz

release: pip-release
