"""
Import data exported from old mica as zip files.
"""

import sys
import mica.core
import os.path
import os

def add_arguments(parser):
    """
    Add REST command specific options
    """
    parser.add_argument('path', help='Path to the zip file or directory that contains zip files to be imported')
    parser.add_argument('--publish', '-pub', action='store_true', help='Publish imported study')

def import_zip(args, path):
    """
    Import the Zip file content
    """
    print "Importing " + path + "..."
    # Build and send request
    try:
        request = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args)).new_request()
        request.fail_on_error()

        if args.verbose:
            request.verbose()

        # send request
        request.content_upload(path).accept_json().content_type('multipart/form-data')
        response = request.post().resource('/draft/studies/_import?publish=' + str(args.publish).lower()).send()

        # format response
        res = response.content

        # output to stdout
        if len(res) > 0:
            print res
    except Exception, e:
        print e
        sys.exit(2)
    except pycurl.error, error:
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr
        sys.exit(2)


def do_command(args):
    """
    Execute Import Zip command
    """
    if args.path.endswith('.zip'):
        import_zip(args, args.path)
    else:
        for export_file in os.listdir(args.path):
            if export_file.endswith('.zip'):
                import_zip(args, args.path + '/' + export_file)


