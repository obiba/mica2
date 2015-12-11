"""
Mica file management.
"""

import argparse
import json
import sys
import pycurl
import mica.core
import urllib


class FileAction(argparse.Action):
    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, '_file_cmd', self.dest)
        setattr(namespace, self.dest, values)


class StoreTrueFileAction(FileAction):
    def __init__(self, *args, **kwargs):
        kwargs.update(dict(nargs=0, const=True))
        super(StoreTrueFileAction, self).__init__(*args, **kwargs)

    def __call__(self, parser, namespace, values, option_string=None):
        super(StoreTrueFileAction, self).__call__(parser, namespace, self.const, option_string=option_string)


class MicaFile:
    """
    File on Mica file system
    """

    def __init__(self, path):
        self.path = path

    def get_dl_ws(self):
        return '/'.join(['/draft/file-dl', urllib.quote(self.path.strip('/'))])

    def get_ws(self):
        return '/'.join(['/draft/file', urllib.quote(self.path.strip('/'))])


def add_arguments(parser):
    """
    Add file command specific options
    """
    parser.add_argument('path', help='File path in Mica file system')
    parser.add_argument('--json', '-j', action='store_true', help='Pretty JSON formatting of the response')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--download', '-dl', action=StoreTrueFileAction, help='Download file')
    group.add_argument('--upload', '-up', action=FileAction, help='Upload a local file to a folder in Mica file system, requires the folder to be in DRAFT state')
    group.add_argument('--create', '-c', action=FileAction, help='Create a folder at a specific location, requires the file to be in DRAFT state')
    group.add_argument('--copy', '-cp', action=FileAction, help='Copy a file to the specified destination')
    group.add_argument('--move', '-mv', action=FileAction, help='Move a file to the specified destination, requires the file to be in DRAFT state')
    group.add_argument('--delete', '-d', action=StoreTrueFileAction, help='Delete a file on Mica file system, requires the file to be in DELETED state')
    group.add_argument('--name', '-n', action=FileAction, help='Rename a file, requires the file to be in DRAFT state')
    group.add_argument('--status', '-st', action=FileAction, help='Change file status')
    group.add_argument('--publish', '-pu', action=StoreTrueFileAction, help='Publish a file, requires the file to be in UNDER_REVIEW state')
    group.add_argument('--unpublish', '-un', action=StoreTrueFileAction, help='Unpublish a file')


class MicaFileClient(object):

    FILES_WS = '/draft/files'
    STATUS_DRAFT = 'DRAFT'
    STATUS_UNDER_REVIEW = 'UNDER_REVIEW'
    STATUS_DELETED = 'DELETED'

    def __init__(self, client, file, verbose):
        self.client = client
        self.file = file
        self.verbose = verbose

    def _get_request(self):
        request = self.client.new_request()
        request.fail_on_error().accept_json()

        if self.verbose:
            request.verbose()

        return request

    def _validate_status(self, status):
        state = self.get().as_json()
        if state['revisionStatus'] != status:
            raise Exception('Invalid file revision status. Found: %s, Required: %s' % (state['revisionStatus'], status))

    def get(self):
        return self._get_request().get().resource(self.file.get_ws()).send()

    def create(self, name):
        self._validate_status(self.STATUS_DRAFT)
        return self._get_request().post().resource(self.FILES_WS).content_type_json().content(
                json.dumps(dict(id='', fileName='.', path='/'.join([self.file.path, name])))).send()

    def copy(self, dest):
        return self._get_request().put().resource('%s?copy=%s' % (self.file.get_ws(), urllib.quote_plus(dest, safe=''))).send()

    def move(self, dest):
        self._validate_status(self.STATUS_DRAFT)
        return self._get_request().put().resource('%s?move=%s' % (self.file.get_ws(), urllib.quote_plus(dest, safe=''))).send()

    def name(self, name):
        self._validate_status(self.STATUS_DRAFT)
        return self._get_request().put().resource('%s?name=%s' % (self.file.get_ws(), urllib.quote_plus(name, safe=''))).send()

    def status(self, status):
        return self._get_request().put().resource('%s?status=%s' % (self.file.get_ws(), status.upper())).send()

    def publish(self, published):
        if published:
            self._validate_status(self.STATUS_UNDER_REVIEW)

        return self._get_request().put().resource('%s?publish=%s' % (self.file.get_ws(), str(published).lower())).send()

    def unpublish(self, *args):
        return self.publish(False)

    def upload(self, upload):
        response = self._get_request().content_upload(upload).accept('text/html')\
                .content_type('multipart/form-data').post().resource('/files/temp').send()
        location = response.headers['Location'].split('/ws')[1]
        temp_file = self._get_request().get().resource(location).send().as_json()
        fileName = temp_file.pop('name', '')
        temp_file.update(dict(fileName=fileName,justUploaded=True, path=self.file.path))

        return self._get_request().post().resource(self.FILES_WS).content_type_json().content(
                json.dumps(temp_file)).send()

    def download(self, *args):
        return self._get_request().get().resource(self.file.get_dl_ws()).send()

    def delete(self, *args):
        self._validate_status(self.STATUS_DELETED)
        return self._get_request().delete().resource(self.file.get_ws()).send()


def do_command(args):
    """
    Execute file command
    """
    # Build and send request

    try:
        client = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args))
        file = MicaFile(args.path)
        file_client = MicaFileClient(client, file, args.verbose)
        response = getattr(file_client, args._file_cmd)(getattr(args, args._file_cmd)) if hasattr(args, '_file_cmd') else file_client.get()

        # format response
        res = response.pretty_json() if args.json and not args.download and not args.upload else response.content

        # output to stdout
        print res
    except Exception, e:
        print >> sys.stderr, e
        sys.exit(2)
    except pycurl.error, error:
        print response
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr
        sys.exit(2)
