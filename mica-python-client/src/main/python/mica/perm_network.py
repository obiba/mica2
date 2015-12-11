"""
Apply permissions on a network.
"""

import sys
import mica.core
import mica.perm

def add_arguments(parser):
    """
    Add command specific options
    """
    mica.perm.add_permission_arguments(parser)
    parser.add_argument('id', help='Network ID')

def do_command(args):
    """
    Execute permission command
    """
    # Build and send requests
    try:
        mica.perm.validate_args(args)

        request = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args)).new_request()

        if args.verbose:
            request.verbose()

        # send request
        if args.delete:
            request.delete()
        else:
            request.put()

        try:
            response = request.resource(mica.perm.do_ws(args, ['draft','network', args.id, 'permissions'])).send()
        except Exception, e:
            print Exception, e

        # format response
        if response.code != 200:
            print response.content

    except Exception, e:
        print e
        sys.exit(2)

    except pycurl.error, error:
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr
        sys.exit(2)
