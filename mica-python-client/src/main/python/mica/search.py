'''
Mica search query.
'''

import argparse
import json
import sys
import pycurl
import mica.core
import csv
import codecs
import string

def add_arguments(parser):
    '''
    Add tags command specific options
    '''
    parser.add_argument('--out', '-o', required=False, help='Output file (default is stdout).')
    parser.add_argument('--target', '-t', required=True, choices=['variable', 'dataset', 'study', 'population', 'dce', 'network'],
                        help='Document type to be searched for.')
    parser.add_argument('--query', '-q', required=False, help='Query that filters the documents. If not specified, no filter is applied.')
    parser.add_argument('--start', '-s', required=False, type=int, default=0, help='Start search at document position.')
    parser.add_argument('--limit', '-lm', required=False, type=int, default=100, help='Max number of documents.')
    parser.add_argument('--locale', '-lc', required=False, default='en', help='The language for labels.')
    parser.add_argument('--json', '-j', action='store_true', help='Output the response in JSON.')

def send_search_request(client, ws, query, verbose=False):
    '''
    Create a new request
    '''
    response = None
    try:
        request = client.new_request()
        if verbose:
            request.verbose()
        response = request.post().resource(ws).content_type_form().form({'query': query}).send()
    except Exception, e:
        print >> sys.stderr, e
    except pycurl.error, error:
        errno, errstr = error
        print >> sys.stderr, 'An error occurred: ', errstr

    return response.as_json()

def as_rql(name, args):
    return name + '(' + ','.join(args) + ')'

def append_rql(query, target, select, sort, start, limit, locale):
    _fields = as_rql('fields(', select) + ')'
    _sort = as_rql('sort', sort)
    _limit = as_rql('limit', [str(start), str(limit)])
    statement = ','.join([_fields, _limit, _sort])
    # normalize
    q = query
    if q == None or q == '':
        q = target + '()'

    # hack: replace target call with statement
    if target + '()' in q:
        q = string.replace(q, target + '()', target + '(' + statement + ')')
    elif target + '(' in q:
        q = string.replace(q, target + '(', target + '(' + statement + ',')
    else:
        q = target + '(' + statement + '),' + q

    return q + ',locale(' + locale + ')'

def extract_label(labels, locale='en', localeKey='lang', valueKey='value'):
    encoder = codecs.getincrementalencoder('utf-8')()
    label_und = None
    if labels:
        for label in labels:
            if label[localeKey] == locale:
                return encoder.encode(label[valueKey])
            if label[localeKey] == 'und':
                label_und = label[valueKey]
    return encoder.encode(label_und)

def new_writer(out, headers):
    file = sys.stdout
    if out:
        file = open(out, 'wb')
    writer = csv.DictWriter(file, fieldnames=headers, escapechar='"', quotechar='"', quoting=csv.QUOTE_ALL)
    writer.writeheader()
    return writer

def search_networks(args, client):
    q = append_rql(args.query, 'network', ['acronym', 'name', 'studyIds'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['networks', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','acronym','studyIds'])
    if 'networkResultDto' in res and 'obiba.mica.NetworkResultDto.result' in res['networkResultDto']:
        for item in res['networkResultDto']['obiba.mica.NetworkResultDto.result']['networks']:
            writer.writerow({
                'id': item['id'],
                'name': extract_label(item['name'], args.locale),
                'acronym': extract_label(item['acronym'], args.locale),
                'studyIds': '|'.join(item['studyIds'])
            })

def search_studies(args, client):
    q = append_rql(args.query, 'study', ['acronym', 'name'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['studies', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','acronym'])
    if 'studyResultDto' in res and 'obiba.mica.StudyResultDto.result' in res['studyResultDto']:
        for item in res['studyResultDto']['obiba.mica.StudyResultDto.result']['summaries']:
            writer.writerow({
                'id': item['id'],
                'name': extract_label(item['name'], args.locale),
                'acronym': extract_label(item['acronym'], args.locale)
            })

def search_study_populations(args, client):
    q = append_rql(args.query, 'study', ['populations.name'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['studies', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','studyId'])
    if 'studyResultDto' in res and 'obiba.mica.StudyResultDto.result' in res['studyResultDto']:
        for item in res['studyResultDto']['obiba.mica.StudyResultDto.result']['summaries']:
            if 'populationSummaries' in item:
                for pop in item['populationSummaries']:
                    writer.writerow({
                        'id': item['id'] + ':' + pop['id'],
                        'name': extract_label(pop['name'], args.locale),
                        'studyId': item['id']
                    })

def search_study_dces(args, client):
    q = append_rql(args.query, 'study', ['populations.dataCollectionEvents'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['studies', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','studyId', 'populationId', 'start', 'end'])
    if 'studyResultDto' in res and 'obiba.mica.StudyResultDto.result' in res['studyResultDto']:
        for item in res['studyResultDto']['obiba.mica.StudyResultDto.result']['summaries']:
            if 'populationSummaries' in item:
                for pop in item['populationSummaries']:
                    if 'dataCollectionEventSummaries' in pop:
                        for dce in pop['dataCollectionEventSummaries']:
                            writer.writerow({
                                'id': item['id'] + ':' + pop['id'] + dce['id'],
                                'name': extract_label(dce['name'], args.locale),
                                'studyId': item['id'],
                                'populationId': item['id'] + ':' + pop['id'],
                                'start': dce['start'] if 'start' in dce else '',
                                'end': dce['end'] if 'end' in dce else ''
                            })


def search_datasets(args, client):
    q = append_rql(args.query, 'dataset', ['acronym', 'name', 'studyTable', 'harmonizationTable'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['datasets', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','acronym', 'variableType', 'entityType', 'studyId', 'populationId', 'dceId'])
    if 'datasetResultDto' in res and 'obiba.mica.DatasetResultDto.result' in res['datasetResultDto']:
        for item in res['datasetResultDto']['obiba.mica.DatasetResultDto.result']['datasets']:
            study_id = ''
            population_id = ''
            dce_id = ''
            if 'obiba.mica.CollectedDatasetDto.type' in item:
                study_id = item['obiba.mica.CollectedDatasetDto.type']['studyTable']['studyId']
                population_id = study_id + ':' + item['obiba.mica.CollectedDatasetDto.type']['studyTable']['populationId']
                dce_id = item['obiba.mica.CollectedDatasetDto.type']['studyTable']['dceId']
            if 'obiba.mica.HarmonizedDatasetDto.type' in item:
                study_id = item['obiba.mica.HarmonizedDatasetDto.type']['harmonizationTable']['studyId']
                population_id = study_id + ':' + item['obiba.mica.HarmonizedDatasetDto.type']['harmonizationTable']['populationId']
            writer.writerow({
                'id': item['id'],
                'name': extract_label(item['name'], args.locale),
                'acronym': extract_label(item['acronym'], args.locale),
                'variableType': item['variableType'],
                'entityType': item['entityType'],
                'studyId': study_id,
                'populationId': population_id,
                'dceId': dce_id
            })

def search_variables(args, client):
    q = append_rql(args.query, 'variable', ['attributes','nature','valueType'], ['id'], args.start, args.limit, args.locale)
    ws = mica.core.UriBuilder(['variables', '_rql']).build()
    res = send_search_request(client, ws, q, args.verbose)
    writer = new_writer(args.out, ['id','name','label','valueType','nature','datasetId','studyId','variableType'])
    if 'variableResultDto' in res and 'obiba.mica.DatasetVariableResultDto.result' in res['variableResultDto']:
        for item in res['variableResultDto']['obiba.mica.DatasetVariableResultDto.result']['summaries']:
            writer.writerow({
                'id': item['id'],
                'name': item['name'],
                'label': extract_label(item['variableLabel'], args.locale),
                'datasetId': item['datasetId'],
                'studyId': item['studyId'],
                'variableType': item['variableType'],
                'valueType': item['valueType'] if 'valueType' in item else '',
                'nature': item['nature'] if 'nature' in item else ''
            })

def do_command(args):
    '''
    Execute search command
    '''
    client = mica.core.MicaClient.build(mica.core.MicaClient.LoginInfo.parse(args))
    if args.target == 'network':
        search_networks(args, client)
    elif args.target == 'study':
        search_studies(args, client)
    elif args.target == 'population':
        search_study_populations(args, client)
    elif args.target == 'dce':
        search_study_dces(args, client)
    elif args.target == 'dataset':
        search_datasets(args, client)
    elif args.target == 'variable':
        search_variables(args, client)
