// Clean up what deleted studies, networks, datasets and projects left behind before Mica 7.0:
//   - permissions (subjectAcl): projects had no delete listener, the child ACLs of a draft entity (comments, status...)
//     were never removed, and the file ACLs of the entity's folder could be left behind by concurrent deletes
//   - comments (comment): never deleted with their entity
//   - files (attachmentState, attachment and their stored file in GridFS): the folder of individual studies and of
//     both dataset types was never deleted
// A new entity reusing the id of a deleted one inherits all of them.
//
// Not handled: logo files of deleted studies. A logo cannot be told apart from other stored files once its study is
// gone, and older study versions may still refer to it. Variables of deleted datasets kept in variable sets (carts)
// are not handled either: editing the sets directly would leave the search index out of date.
//
// Dry run (default, changes nothing):
//   mongosh --quiet mica entity-orphan-cleanup.js
// Apply:
//   mongosh --quiet mica --eval 'var APPLY = true' --file entity-orphan-cleanup.js
// With Docker Compose (Mongo service "mongo"), after docker compose cp entity-orphan-cleanup.js mongo:/tmp/:
//   docker compose exec -T mongo mongosh --quiet mica --file /tmp/entity-orphan-cleanup.js                              (dry run)
//   docker compose exec -T mongo mongosh --quiet mica --eval 'var APPLY = true' --file /tmp/entity-orphan-cleanup.js    (apply)
// APPLY is a shell variable set with --eval, not an environment variable: APPLY=true or docker compose exec -e APPLY=true
// is ignored and the script runs a dry run. The last line of the output says which mode ran.
//
// Before applying: back up the database (mongodump --db mica) and stop Mica.
// After applying: start Mica (permissions are cached).
//
// A document is an orphan when the entity id it belongs to does not exist any more in the entity's collection.
// Role-level permissions (no instance or "*") and the documents of existing entities are never touched.

const apply = typeof APPLY !== 'undefined' && APPLY === true;

// resource (also the root folder of the entity's files) -> collection holding the entities
const ENTITY_TYPES = {
  'individual-study': 'study',
  'harmonization-study': 'harmonizationStudy',
  'network': 'network',
  'project': 'project',
  'collected-dataset': 'studyDataset',
  'harmonized-dataset': 'harmonizationDataset',
};

const FILE_RESOURCES = ['/file', '/draft/file'];

// same as org.obiba.mica.file.FileUtils#encode: java.net.URLEncoder (UTF-8) with '/' kept as is
function javaUrlEncode(s) {
  return encodeURIComponent(s)
    .replace(/[!'()~]/g, c => '%' + c.charCodeAt(0).toString(16).toUpperCase())
    .replace(/%20/g, '+')
    .replace(/%2F/g, '/');
}

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\\/]/g, '\\$&');
}

function idString(v) {
  return v && typeof v.toHexString === 'function' ? v.toHexString() : String(v);
}

function firstSegment(path) {
  const slash = path.indexOf('/');
  return slash < 0 ? path : path.substring(0, slash);
}

// orphans per entity: "<type> <id>" -> { acls, comments, states, attachments }
const byEntity = new Map();
function entry(type, id) {
  const key = `${type} ${id}`;
  if (!byEntity.has(key)) byEntity.set(key, { acls: [], comments: [], states: [], attachments: [] });
  return byEntity.get(key);
}

const handledAcls = new Set();
function addAcl(type, id, acl) {
  const key = idString(acl._id);
  if (handledAcls.has(key)) return;
  handledAcls.add(key);
  entry(type, id).acls.push(acl);
}

for (const [type, collection] of Object.entries(ENTITY_TYPES)) {
  // existing ids, raw and encoded: entity ACL instances and file paths are encoded, child ACL resources are not
  const liveIds = new Set();
  db.getCollection(collection).find({}, { _id: 1 }).forEach(doc => {
    const id = idString(doc._id);
    liveIds.add(id);
    liveIds.add(javaUrlEncode(id));
  });
  const isOrphanId = id => typeof id === 'string' && id.length > 0 && id !== '*' && !liveIds.has(id);
  print(`${type}: ${db.getCollection(collection).countDocuments({})} existing`);

  const resource = '/' + type;
  const draftResource = '/draft/' + type;

  // permissions on the published and draft entity
  db.subjectAcl.find({ resource: { $in: [resource, draftResource] } }).forEach(acl => {
    if (isOrphanId(acl.instance)) addAcl(type, acl.instance, acl);
  });

  // child permissions of the draft entity: /draft/<type>/<id> and /draft/<type>/<id>/...
  const childPrefix = draftResource + '/';
  db.subjectAcl.find({ resource: { $regex: '^' + escapeRegex(childPrefix) } }).forEach(acl => {
    const id = firstSegment(acl.resource.substring(childPrefix.length));
    if (isOrphanId(id)) addAcl(type, id, acl);
  });

  // file permissions: instance /<type>/<id> and below
  const filePrefix = resource + '/';
  db.subjectAcl.find({ resource: { $in: FILE_RESOURCES }, instance: { $regex: '^' + escapeRegex(filePrefix) } })
    .forEach(acl => {
      const id = firstSegment(acl.instance.substring(filePrefix.length));
      if (isOrphanId(id)) addAcl(type, id, acl);
    });

  // comments
  db.comment.find({ resourceId: draftResource, instanceId: { $type: 'string' } }).forEach(c => {
    if (isOrphanId(c.instanceId)) entry(type, c.instanceId).comments.push(c);
  });

  // files: states and all the revisions of each file, under /<type>/<id>
  db.attachmentState.find({ path: { $regex: '^' + escapeRegex(filePrefix) } }).forEach(s => {
    const id = firstSegment(s.path.substring(filePrefix.length));
    if (isOrphanId(id)) entry(type, id).states.push(s);
  });
  db.attachment.find({ path: { $regex: '^' + escapeRegex(filePrefix) } }).forEach(a => {
    const id = firstSegment(a.path.substring(filePrefix.length));
    if (isOrphanId(id)) entry(type, id).attachments.push(a);
  });
}

//
// Stored files (GridFS, bucket "fs"): a file is stored under filename = the attachment's file reference (its id when
// not set), with metadata.attachment = the attachment id. A copied file shares the file reference of the
// original but has its own stored file, told apart by metadata.attachment. Files stored by older versions have no
// metadata: they are only deleted when no remaining attachment refers to their file reference.
//

const orphanAttachments = [...byEntity.values()].flatMap(e => e.attachments);
const orphanAttachmentIds = new Set(orphanAttachments.map(a => idString(a._id)));
const reference = a => a.fileReference || idString(a._id);
const references = [...new Set(orphanAttachments.map(reference))];
const stillUsed = new Set();
if (references.length > 0) {
  const asIds = references.flatMap(r => /^[0-9a-f]{24}$/.test(r) ? [r, ObjectId(r)] : [r]);
  db.attachment.find({ $or: [{ fileReference: { $in: references } }, { _id: { $in: asIds } }] }, { _id: 1, fileReference: 1 })
    .forEach(a => {
      if (!orphanAttachmentIds.has(idString(a._id))) stillUsed.add(reference(a));
    });
}
const storedFiles = new Map(); // fs.files _id (as string) -> _id
let keptFiles = 0;
orphanAttachments.forEach(a => {
  const ref = reference(a);
  const files = db.getCollection('fs.files');
  // the file stored for this very attachment
  files.find({ filename: ref, 'metadata.attachment': idString(a._id) }, { _id: 1 })
    .forEach(f => storedFiles.set(idString(f._id), f._id));
  // a file stored without metadata, possibly shared with another attachment
  files.find({ filename: ref, 'metadata.attachment': { $exists: false } }, { _id: 1 }).forEach(f => {
    if (stillUsed.has(ref)) keptFiles++;
    else storedFiles.set(idString(f._id), f._id);
  });
});

//
// Report
//

const total = { acls: 0, comments: 0, states: 0, attachments: 0 };
[...byEntity.keys()].sort().forEach(key => {
  const e = byEntity.get(key);
  print(`\n${key}:`);
  e.acls.forEach(a => print(`   permission  ${a.resource} : ${a.instance || '*'} : ${a.principal} (${a.type})`));
  e.comments.forEach(c => print(`   comment     ${idString(c._id)} by ${c.createdBy}`));
  e.states.forEach(s => print(`   file        ${s.path}/${s.name}`));
  if (e.attachments.length > 0) print(`   ${e.attachments.length} file revision(s)`);
  Object.keys(total).forEach(k => total[k] += e[k].length);
});

print(`\n${byEntity.size} deleted entities with orphans:`);
print(`   ${total.acls} permissions`);
print(`   ${total.comments} comments`);
print(`   ${total.states} files (${total.attachments} revisions, ${storedFiles.size} stored files)`);
if (keptFiles > 0) print(`   ${keptFiles} stored file(s) kept: still used by another file`);

//
// Apply
//

const count = total.acls + total.comments + total.states + total.attachments;
if (!apply) {
  print(`\nDry run: nothing deleted.${count > 0 ? ' Rerun with APPLY = true to delete the orphans listed above.' : ''}`);
} else if (count === 0) {
  print('\nNothing to delete.');
} else {
  print('\nDeleting:');
  const all = field => [...byEntity.values()].flatMap(e => e[field]).map(d => d._id);
  if (storedFiles.size > 0) {
    const ids = [...storedFiles.values()];
    const chunks = db.getCollection('fs.chunks').deleteMany({ files_id: { $in: ids } });
    const files = db.getCollection('fs.files').deleteMany({ _id: { $in: ids } });
    print(`   ${files.deletedCount} stored files (${chunks.deletedCount} chunks)`);
  }
  print(`   ${db.attachment.deleteMany({ _id: { $in: all('attachments') } }).deletedCount} file revisions`);
  print(`   ${db.attachmentState.deleteMany({ _id: { $in: all('states') } }).deletedCount} files`);
  print(`   ${db.comment.deleteMany({ _id: { $in: all('comments') } }).deletedCount} comments`);
  print(`   ${db.subjectAcl.deleteMany({ _id: { $in: all('acls') } }).deletedCount} permissions`);
  print('Start or restart Mica (permissions are cached).');
}
