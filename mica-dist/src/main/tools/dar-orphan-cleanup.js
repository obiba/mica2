// Clean up what deleted data access requests left behind before the fix of their deletion:
//   - ACLs: the children prefix of SubjectAclService.removeResourcePermissions never matched (since 2015), and a
//     request with attachments failed to delete half-way, leaving all its ACLs
//   - agreements (dataAccessAgreement) and their form files (GridFS): never deleted with their request
//   - comments (comment): deleted with a resource id that never matched
//   - collaborators (dataAccessCollaborator): deleted by id instead of by request id, so never deleted. A pending
//     invitation to a deleted request could then be accepted on a new request reusing its id.
//
// Dry run (default, changes nothing):
//   mongosh --quiet mica dar-orphan-cleanup.js
//   docker exec -i <container> mongosh --quiet mica --eval "$(cat dar-orphan-cleanup.js)"
// Apply:
//   mongosh --quiet mica --eval 'var APPLY = true' --file dar-orphan-cleanup.js
//   docker exec -i <container> mongosh --quiet mica --eval "var APPLY = true; $(cat dar-orphan-cleanup.js)"
//
// Before applying: back up the database (mongodump --db mica) and stop Mica, or at least make sure no data access
// request is being created meanwhile (its documents could be written before the request itself).
// After applying: start/restart Mica (ACLs are cached).
//
// A document is an orphan when the data access request id it belongs to does not exist any more. Role-level ACLs
// (no instance or "*"), global resources such as "/data-access-request/private-comment" and the documents of existing
// requests are never touched. Documents left by a deleted request whose id was later reused by a new request cannot
// be told apart by id: the ones older than that new request are reported for manual review, never deleted.

const apply = typeof APPLY !== 'undefined' && APPLY === true;

// resources under /data-access-request/ that are not request ids
const NOT_REQUEST_IDS = new Set(['private-comment', 'action-logs', 'new']);

// existing requests; ids are stored raw or URL-encoded depending on the code path: keep both forms, an extra form can
// only make the script delete less
const liveIds = new Set();
const liveCreated = new Map(); // id -> created date
db.dataAccessRequest.find({}, { _id: 1, createdDate: 1 }).forEach(r => {
  const id = String(r._id);
  liveIds.add(id);
  liveIds.add(encodeURIComponent(id));
  liveCreated.set(id, r.createdDate);
});
const liveIdList = [...liveIds];
const isOrphanId = id => typeof id === 'string' && id.length > 0 && !liveIds.has(id);

//
// ACLs
//

function aclRequestId(acl) {
  const resource = acl.resource || '';
  const instance = acl.instance;
  if (resource === '/data-access-request') {
    if (!instance || instance === '*') return null;              // role-level
    return instance;                                             // the request itself
  }
  if (!resource.startsWith('/data-access-request/')) return null;
  const rest = resource.substring('/data-access-request/'.length);
  const slash = rest.indexOf('/');
  const id = slash < 0 ? rest : rest.substring(0, slash);
  if (!id || NOT_REQUEST_IDS.has(id)) return null;
  if (slash < 0) return instance === '_status' ? id : null;      // request status (anything else on that shape: keep)
  return id;                                                     // children: feasibility, amendment, agreement...
}

const orphanAcls = [];
db.subjectAcl.find({ resource: { $regex: '^/data-access-request(/|$)' } }).forEach(acl => {
  const id = aclRequestId(acl);
  if (id !== null && isOrphanId(id)) orphanAcls.push({ requestId: id, doc: acl });
});

//
// Agreements and their form files
//

// ids of the files of a schema form content, as SchemaFormContentFileService.deleteFiles finds them
function formFileIds(content) {
  if (!content) return [];
  let json;
  try {
    json = JSON.parse(content);
  } catch (e) {
    return [];
  }
  const ids = [];
  (function collect(node) {
    if (Array.isArray(node)) return node.forEach(collect);
    if (node === null || typeof node !== 'object') return;
    Object.keys(node).forEach(key => {
      if (key === 'obibaFiles' && Array.isArray(node[key])) {
        node[key].forEach(f => { if (f && f.id) ids.push(String(f.id)); });
      } else {
        collect(node[key]);
      }
    });
  })(json);
  return ids;
}

const orphanAgreements = db.dataAccessAgreement
  .find({ parentId: { $type: 'string', $nin: liveIdList } }).toArray()
  .map(a => ({ requestId: a.parentId, doc: a, fileIds: formFileIds(a.content) }));
const orphanFileIds = orphanAgreements.flatMap(a => a.fileIds);
// GridFS (default bucket "fs"): FileStoreService stores each file under filename = file id
const orphanFiles = orphanFileIds.length === 0 ? [] :
  db.getCollection('fs.files').find({ filename: { $in: orphanFileIds } }, { _id: 1, filename: 1 }).toArray();

//
// Comments
//

const orphanComments = db.comment
  .find({ resourceId: '/data-access-request', instanceId: { $type: 'string', $nin: liveIdList } }).toArray()
  .map(c => ({ requestId: c.instanceId, doc: c }));

//
// Collaborators (their VIEW ACLs are among the ACLs above, their agreements among the agreements)
//

const orphanCollaborators = db.dataAccessCollaborator
  .find({ requestId: { $type: 'string', $nin: liveIdList } }).toArray()
  .map(c => ({ requestId: c.requestId, doc: c }));

//
// Report
//

const byRequest = new Map(); // request id -> lines
function report(requestId, line) {
  if (!byRequest.has(requestId)) byRequest.set(requestId, []);
  byRequest.get(requestId).push(line);
}
orphanAcls.forEach(o => report(o.requestId,
  `acl        ${o.doc.resource} : ${o.doc.instance || '*'} : ${o.doc.principal} (${o.doc.type})`));
orphanAgreements.forEach(o => report(o.requestId,
  `agreement  ${o.doc._id} (applicant ${o.doc.applicant}, ${o.doc.status}, ${o.fileIds.length} form file(s))`));
orphanComments.forEach(o => report(o.requestId,
  `comment    ${o.doc._id} by ${o.doc.createdBy}${o.doc.admin ? ' (private)' : ''}`));
orphanCollaborators.forEach(o => report(o.requestId,
  `collaborator ${o.doc.email}${o.doc.principal ? ` (${o.doc.principal})` : ''}${o.doc.invitationPending ? ', invitation pending' : ''}`));

[...byRequest.keys()].sort().forEach(id => {
  print(`${id}:`);
  byRequest.get(id).forEach(line => print(`   ${line}`));
});

print(`\n${liveCreated.size} existing requests, ${byRequest.size} deleted requests with orphans:`);
print(`   ${orphanAcls.length} ACLs`);
print(`   ${orphanAgreements.length} agreements, with ${orphanFiles.length} of their ${orphanFileIds.length} form files found in GridFS`);
print(`   ${orphanComments.length} comments`);
print(`   ${orphanCollaborators.length} collaborators`);

// report only: other children are deleted before a request delete can fail, none are expected
const unexpected = [
  ['dataAccessAmendment', { parentId: { $type: 'string', $nin: liveIdList } }],
  ['dataAccessFeasibility', { parentId: { $type: 'string', $nin: liveIdList } }],
  ['dataAccessPreliminary', { parentId: { $type: 'string', $nin: liveIdList } }],
].map(([collection, query]) => [collection, db.getCollection(collection).countDocuments(query)]);
const orphanAttachments = db.attachment.find({ path: { $regex: '^/data-access-request/' } }, { path: 1 }).toArray()
  .filter(a => isOrphanId(a.path.split('/')[2])).length;
unexpected.push(['attachment', orphanAttachments]);
unexpected.filter(([, count]) => count > 0).forEach(([collection, count]) =>
  print(`   REVIEW: ${count} ${collection} document(s) of deleted requests (not deleted by this script)`));

// report only: agreements, comments and collaborators older than the existing request with the same id may come from a previous,
// deleted request whose id was reused
let suspicious = 0;
function checkReused(collection, query, idField, describe = () => '') {
  db.getCollection(collection).find(query).forEach(d => {
    const created = liveCreated.get(d[idField]);
    if (created && d.createdDate && d.createdDate < created) {
      suspicious++;
      print(`   REVIEW: ${collection} ${d._id}${describe(d)} is older than request ${d[idField]} (possibly left by a deleted request with the same id)`);
    }
  });
}
checkReused('dataAccessAgreement', { parentId: { $in: liveIdList } }, 'parentId');
checkReused('comment', { resourceId: '/data-access-request', instanceId: { $in: liveIdList } }, 'instanceId');
// an older collaborator on a reused id can still accept its invitation, or already has VIEW on the new request
checkReused('dataAccessCollaborator', { requestId: { $in: liveIdList } }, 'requestId',
  c => ` (${c.email}${c.principal ? `, ${c.principal}` : ''}${c.invitationPending ? ', invitation pending' : ', accepted'})`);

//
// Apply
//

const total = orphanAcls.length + orphanAgreements.length + orphanComments.length + orphanCollaborators.length;
if (!apply) {
  print(`\nDry run: nothing deleted.${total > 0 ? ' Rerun with APPLY = true to delete the orphans listed above.' : ''}`);
} else if (total === 0) {
  print('\nNothing to delete.');
} else {
  print('\nDeleting:');
  if (orphanFiles.length > 0) {
    const fileIds = orphanFiles.map(f => f._id);
    const chunks = db.getCollection('fs.chunks').deleteMany({ files_id: { $in: fileIds } });
    const files = db.getCollection('fs.files').deleteMany({ _id: { $in: fileIds } });
    print(`   ${files.deletedCount} agreement form files (${chunks.deletedCount} chunks)`);
  }
  if (orphanAgreements.length > 0) {
    const res = db.dataAccessAgreement.deleteMany({ _id: { $in: orphanAgreements.map(o => o.doc._id) } });
    print(`   ${res.deletedCount} of ${orphanAgreements.length} agreements`);
  }
  if (orphanComments.length > 0) {
    const res = db.comment.deleteMany({ _id: { $in: orphanComments.map(o => o.doc._id) } });
    print(`   ${res.deletedCount} of ${orphanComments.length} comments`);
  }
  if (orphanCollaborators.length > 0) {
    const res = db.dataAccessCollaborator.deleteMany({ _id: { $in: orphanCollaborators.map(o => o.doc._id) } });
    print(`   ${res.deletedCount} of ${orphanCollaborators.length} collaborators`);
  }
  if (orphanAcls.length > 0) {
    const res = db.subjectAcl.deleteMany({ _id: { $in: orphanAcls.map(o => o.doc._id) } });
    print(`   ${res.deletedCount} of ${orphanAcls.length} ACLs`);
  }
  print('Restart Mica (ACLs are cached).');
}
