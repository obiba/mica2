"""
Update an existing collected dataset, mainly for managing the linkage with opal.
"""

import sys
import mica.core
import json
import urllib

def add_arguments(parser):
    """
    Add REST command specific options
    """
    parser.add_argument('id', help='Collected dataset ID')
    parser.add_argument('--study', '-std', required=False, help='Mica study')
    parser.add_argument('--population', '-pop', required=False, help='Mica population')
    parser.add_argument('--dce', '-dce', required=False, help='Mica study population data collection event')
    parser.add_argument('--project', '-prj', required=False, help='Opal project')
    parser.add_argument('--table', '-tbl', required=False, help='Opal table')
    parser.add_argument('--publish', '-pub', action='store_true', help='Publish the colected dataset')
    parser.add_argument('--unpublish', '-un', action='store_true', help='Unpublish the collected dataset')

def new_request(args):
    request = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args)).new_request()
    request.fail_on_error()
    request.accept_json()
    if args.verbose:
        request.verbose()
    return request

def do_update(path, args):
    print "Updating " + args.id + "..."
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
        if not args.study or not args.population or not args.dce or not args.project or not args.table:
            print "Study table is missing and cannot be created."
            sys.exit(2)
        dataset['obiba.mica.CollectedDatasetDto.type'] = { 'studyTable': { } }
    dataset['obiba.mica.CollectedDatasetDto.type']['studyTable'].pop('studySummary', None)

    # update
    comment = []
    if args.study:
        comment.append('Study: ' + args.study)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['studyId'] = args.study
    if args.population:
        comment.append('Population: ' + args.population)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['populationId'] = args.population
    if args.dce:
        comment.append('DCE: ' + args.dce)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['dataCollectionEventId'] = args.dce
    if args.project:
        comment.append('Project: ' + args.project)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['project'] = args.project
    if args.table:
        comment.append('Table: ' + args.table)
        dataset['obiba.mica.CollectedDatasetDto.type']['studyTable']['table'] = args.table
    request = new_request(args)
    request.put().resource(path).query({ 'comment': ', '.join(comment) + ' (update-collected-dataset)' }).content_type_json()
    request.content(json.dumps(dataset, separators=(',',':')))
    if args.verbose:
        print "Updated: "
        print json.dumps(dataset, sort_keys=True, indent=2, separators=(',', ': '))
    request.send()

def do_command(args):
    """
    Execute dataset update command
    """
    # Build and send request
    try:
        path = '/draft/collected-dataset/' + args.id
        if args.project or args.table:
            do_update(path, args)

        if args.publish:
            print "Publishing " + args.id + "..."
            request = new_request(args)
            request.put().resource(path + '/_publish').send()

        if args.unpublish:
            print "Unpublishing " + args.id + "..."
            request = new_request(args)
            request.delete().resource(path + '/_publish').send()

    except Exception, e:
        print e
        sys.exit(2)
    except pycurl.error, error:
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr
        sys.exit(2)
