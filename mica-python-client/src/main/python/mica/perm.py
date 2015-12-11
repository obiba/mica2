"""
Mica permissions
"""

import mica.core

SUBJECT_TYPES = ('USER', 'GROUP')

PERMISSIONS = ('READER', 'EDITOR', 'REVIEWER')

def add_permission_arguments(parser):
  """
  Add permission arguments
  """
  parser.add_argument('--add', '-a', action='store_true', help='Add a permission')
  parser.add_argument('--delete', '-d', action='store_true', required=False, help='Delete a permission')
  parser.add_argument('--permission', '-pe', help="Permission to apply: %s" % ', '.join(PERMISSIONS).lower())
  parser.add_argument('--subject', '-s', required=True, help='Subject name to which the permission will be granted')
  parser.add_argument('--type', '-ty', required=False, help='Subject type: user or group')

def map_permission(permission):
  """
  Map permission argument to permission query parameter
  """
  if permission.upper() not in PERMISSIONS:
    return None

  return permission.upper()

def validate_args(args):
  """
  Validate action, permission and subject type
  """
  if not args.add and not args.delete:
    raise Exception("You must specify a permission operation: [--add|-a] or [--delete|-de]")

  if args.add:
    if not args.permission:
      raise Exception("A permission name is required: %s" % ', '.join(PERMISSIONS).lower())
    if map_permission(args.permission) is None:
      raise Exception("Valid permissions are: %s" % ', '.join(PERMISSIONS).lower())

  if not args.type or args.type.upper() not in SUBJECT_TYPES:
    raise Exception("Valid subject types are: %s" % ', '.join(SUBJECT_TYPES).lower())

def do_ws(args, path):
  """
  Build the web service resource path
  """
  if args.add:
    return mica.core.UriBuilder(path) \
      .query('type', args.type.upper()) \
      .query('role', map_permission(args.permission)) \
      .query('principal', args.subject) \
      .build()

  if args.delete:
    return mica.core.UriBuilder(path) \
      .query('type', args.type.upper()) \
      .query('principal', args.subject) \
      .build()
