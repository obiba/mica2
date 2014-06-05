# OBiBa AngularJS commons

## How to build?

Install NodeJS, Grunt and Bower:

```
sudo add-apt-repository -y ppa:chris-lea/node.js
sudo apt-get install -y nodejs
npm install -g grunt-cli bower
```

Download dependencies:

```
npm install
bower install
```

Build project:

```
grunt
```

Test coverage available in `/coverage` dir.

## How to use it?

In your project add a dependency to ng-obiba:

```
bower install git@github.com:obiba/ng-obiba.git --save
```

To update to latest ng-obiba version:

```
bower update
```


## Bug tracker

Have a bug? Please create an issue on [OBiBa JIRA](http://jira.obiba.org/jira/browse/NGOBIBA).


## Continuous integration

See [OBiBa Jenkins](http://ci.obiba.org/view/ng-obiba).


## Mailing list

Have a question? Ask on our mailing list!

obiba-users@googlegroups.com

[http://groups.google.com/group/obiba-users](http://groups.google.com/group/obiba-users)


## License

OBiBa software are open source and made available under the [GPL3 licence](http://www.obiba.org/node/62).
OBiBa software are free of charge.