#!/bin/sh

VAGRANT_DATA=/vagrant_data

source $VAGRANT_DATA/settings

sudo apt-get update

# need to be in first as it installs add-apt-repository command
sudo apt-get install -y python-software-properties

if [ $(grep -c '^deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' /etc/apt/sources.list) -eq 0 ];
then
  echo ">> Add MongoDB APT repo"
	sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
	echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list
fi

echo ">> Add Oracle Java PPA"
sudo add-apt-repository -y ppa:webupd8team/java

echo ">> Add Maven3 PPA"
sudo add-apt-repository -y ppa:natecarlson/maven3

echo ">> Add NodeJS PPA"
sudo add-apt-repository -y ppa:chris-lea/node.js

sudo apt-get update

echo ">> Install MongoDB"
sudo apt-get install mongodb-10gen

if [ ! -d /etc/mysql ];
then
  echo ">> Install MySQL"
  echo mysql-server mysql-server/root_password select $MYSQL_ROOT_PWD | debconf-set-selections
  echo mysql-server mysql-server/root_password_again select $MYSQL_ROOT_PWD | debconf-set-selections
	sudo apt-get -y install mysql-server
fi

echo ">> Install Oracle Java 8"
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
sudo apt-get install -y oracle-java8-installer
sudo update-java-alternatives -s java-8-oracle
echo "export JAVA_HOME=/usr/lib/jvm/java-8-oracle" >> ~/.bashrc

echo ">> Install utilities"
sudo apt-get -y install -y openssh-server vim git zip bzip2 fontconfig curl make

echo ">> Install Maven 3"
sudo apt-get install -y maven3
sudo ln -s /usr/share/maven3/bin/mvn /usr/bin/mvn

echo ">> Install NodeJS"
sudo apt-get install -y nodejs

echo ">> Install Yeoman"
npm install -g yo

echo ">> Install Jhipster"
npm install -g generator-jhipster@0.12.0

echo ">> Install Compass"
curl -L get.rvm.io | bash -s stable
sudo bash -c "source /etc/profile.d/rvm.sh && rvm requirements; rvm install 2.1.1; gem install compass sass"

sudo apt-get clean
