"""
Update several existing collected dataset, mainly for managing the linkage with opal.
"""

import sys
import mica.core
import json
import urllib
import re

def add_arguments(parser):
    """
    Add REST command specific options
    """
    parser.add_argument('id', help='Regular expression to filter the collected dataset IDs')
    parser.add_argument('--project', '-prj', required=False, help='Opal project')
    parser.add_argument('--dry', '-d', action='store_true', help='Dry run to evaluate the regular expression')
    parser.add_argument('--publish', '-pub', action='store_true', help='Publish the colected dataset')
    parser.add_argument('--unpublish', '-un', action='store_true', help='Unpublish the collected dataset')

def new_request(args):
    request = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args)).new_request()
    request.fail_on_error()
    request.accept_json()
    if args.verbose:
        request.verbose()
    return request

def do_update(path, args, id):
    print "Updating " + id + "..."
    # get existing and remove useless fields
    request = new_request(args)
    response = request.get().resource(path).send()
    dataset = json.loads(response.content)
    dataset.pop('obiba.mica.EntityStateDto.datasetState', None)
    dataset.pop('variableType', None)
    dataset.pop('timestamps', None)
    dataset.pop('published', None)
    dataset.pop('permissions', None)
    if 'obiba.mica.CollectedDatasetDto.type' not in dataset:
        print "Study table is missing in " + id
        sys.exit(2)
    dataset['obiba.mica.CollectedDatasetDto.type']['studyTable'].pop('studySummary', None)

    # update
    comment = []
    if args.project:
        comment.append('Project: ' + args.project)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['project'] = args.project
    request = new_request(args)
    request.put().resource(path).query({ 'comment': ', '.join(comment) + ' (update-collected-datasets)' }).content_type_json()
    request.content(json.dumps(dataset, separators=(',',':')))
    if args.verbose:
        print "Updated: "
        print json.dumps(dataset, sort_keys=True, indent=2, separators=(',', ': '))
    request.send()

def do_command(args):
    """
    Execute datasets update command
    """
    # Build and send request
    try:
        path = '/draft/collected-datasets'
        request = new_request(args)
        response = request.get().resource(path).send()
        datasets = json.loads(response.content)
        for dataset in datasets:
            id = dataset['id']
            if re.match(args.id, id):
                if args.dry:
                    print id
                else:
                    path = '/draft/collected-dataset/' + id
                    if args.project:
                        do_update(path, args, id)
                    if args.publish:
                        print "Publishing " + id + "..."
                        request = new_request(args)
                        request.put().resource(path + '/_publish').send()
                    if args.unpublish:
                        print "Unpublishing " + id + "..."
                        request = new_request(args)
                        request.delete().resource(path + '/_publish').send()

    except Exception, e:
        print e
        sys.exit(2)
    except pycurl.error, error:
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr
        sys.exit(2)
