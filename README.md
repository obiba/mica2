# Mica [![Build Status](https://travis-ci.org/obiba/mica2.svg?branch=master)](https://travis-ci.org/obiba/mica2)

Web data portal application server.

* See [download instructions](http://www.obiba.org/pages/products/mica/#download).
* Read the [documentation](http://micadoc.obiba.org).
* Have a bug or a question? Please create an issue on [GitHub](https://github.com/obiba/mica2/issues).
* Continuous integration is on [Travis](https://travis-ci.org/obiba/mica2).

## For developers

Install NodeJS, Grunt, Bower and Debian packaging utils

```
sudo add-apt-repository -y ppa:chris-lea/node.js
sudo apt-get install -y nodejs devscripts
npm install -g grunt-cli bower
```

If you run mica2 server for the first time, run `make npm-install`.

Make sure you use **Java 8**:

```
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

During development, run

* `make all drop-mongo run` in one terminal to start a fresh empty mica REST server on [HTTP port 8082](http://localhost:8082) or [HTTPS port 8445](https://localhost:8445)
* `make grunt` in another terminal to start Grunt server with live reload on port **9000**

See `make help` for other targets.

## Mailing list

Have a question? Ask on our mailing list!

obiba-users@googlegroups.com

[http://groups.google.com/group/obiba-users](http://groups.google.com/group/obiba-users)

## License

OBiBa software are open source and made available under the [GPL3 licence](http://www.obiba.org/node/62). OBiBa software are free of charge.

## OBiBa acknowledgments

If you are using OBiBa software, please cite our work in your code, websites, publications or reports.

"The work presented herein was made possible using the OBiBa suite (www.obiba.org), a  software suite developed by Maelstrom Research (www.maelstrom-research.org)"

The default Study model included in Mica was designed by [Maelstrom Research](https://www.maelstrom-research.org) under the [Creative Commons License with Non Commercial and No Derivative](https://creativecommons.org/licenses/by-nc-nd/4.0/) constraints.
