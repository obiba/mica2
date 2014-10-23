#!/usr/bin/env python

from ez_setup import use_setuptools
use_setuptools()
from setuptools import setup

VERSION = '@project.version@'
NAME = '@project.name@'
PACKAGE_NAME = 'mica-python-client'
AUTHOR = 'OBiBa'
AUTHOR_EMAIL = 'OBiBa <info@obiba.org>'
MAINTAINER = 'OBiBa'
MAINTAINER_EMAIL = 'OBiBa <info@obiba.org>'
LICENSE = 'GPL-3'
PLATFORMS = "Any"
URL = 'http://www.obiba.org'
DOWNLOAD_URL = '@project.download.url@'
DESCRIPTION = '@project.description@'
DESCRIPTION_LOG = """Data publishing Web application for biobanks by OBiBa. Mica is
    OBiBa's publishing application for biobanks. Participant data, once
    collected from any data source, must be integrated and stored in a central
    data repository under a uniform model.
"""
PACKAGES = ['mica', 'mica.protobuf']
PACKAGES_DIR = {'mica': 'bin/mica'}
SCRIPTS = ['bin/scripts/mica']
INSTALL_REQUIRES = ['protobuf >= 2.4', 'pycurl']

setup(
    name=PACKAGE_NAME,
    version=VERSION,
    author=AUTHOR,
    author_email=AUTHOR_EMAIL,
    maintainer=MAINTAINER,
    maintainer_email=MAINTAINER_EMAIL,
    url=URL,
    license=LICENSE,
    description=DESCRIPTION,
    long_description=DESCRIPTION_LOG,
    platforms=PLATFORMS,
    packages=PACKAGES,
    package_dir=PACKAGES_DIR,
    scripts=SCRIPTS,
    install_requires=INSTALL_REQUIRES
)
