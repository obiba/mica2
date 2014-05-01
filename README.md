# Mica-Server

## For developers

Install NodeJS, Grunt and Bower

```
sudo add-apt-repository -y ppa:chris-lea/node.js
sudo apt-get install -y nodejs
sudo npm install -g grunt-cli bower
```

If you run mica-server for the first time, run `make npm-install`

During development, run

* `make all drop-mongo run` in one terminal to start a fresh empty mica REST server on port **8080**
* `make grunt` in another terminal to start Grunt server with live reload on port **9000**

See `make help` for other targets.



## Download

See Obiba Maven 2.x repository: [http://www.obiba.org/maven2](http://www.obiba.org/maven2)

And Obiba Snaphot Maven 2.x repository: [http://www.obiba.org/maven2-snapshots](http://www.obiba.org/maven2-snapshots)


## Bug tracker

Have a bug? Please create an issue on [OBiBa JIRA](http://jira.obiba.org/jira/browse/MICASERVER).


## Continuous integration

See [OBiBa Jenkins](http://ci.obiba.org/view/mica-server).


## Mailing list

Have a question? Ask on our mailing list!

obiba-users@googlegroups.com

[http://groups.google.com/group/obiba-users](http://groups.google.com/group/obiba-users)


## License

OBiBa software are open source and made available under the [GPL3 licence](http://www.obiba.org/node/62). OBiBa software are free of charge.