/**
 * Data access related utilities.
 */
'use strict';

/**
 * Data access management.
 */
class DataAccessService {

  static create(id, type) {
    let url = '/ws/data-access-requests/_empty';
    if (type && id) {
      if (type === 'amendment') {
        url = '/ws/data-access-request/' + id + '/amendments/_empty';
      } else {
        url = '/ws/data-access-request/' + id + '/feasibilities/_empty';
      }
    }
    axios.post(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (response.status === 201) {
          const tokens = response.headers.location.split('/');
          const createdId = tokens[tokens.length - 1];
          let redirect = '/data-access/' + createdId;
          if (type) {
            redirect = '/data-access-' + type + '-form/' + createdId;
          }
          MicaService.redirect(MicaService.normalizeUrl(redirect));
        }
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Creation failed.');
      });
  }

  static createFromCart() {
    let url = '/ws/data-access-requests/_empty';
    axios.post(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (response.status === 201) {
          const tokens = response.headers.location.split('/');
          const createdId = tokens[tokens.length - 1];
          this.linkVariables(createdId);
        }
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Creation failed.');
      });
  }

  static delete(id, type, aId) {
    let url = '/ws/data-access-request/' + id;
    let redirect = '/data-accesses';
    if (type && aId) {
      url = url + '/' + type + '/' + aId;
      redirect = '/data-access/' + id;
    }
    axios.delete(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Deletion failed.');
      });
  }

  static archive(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_archive';
    let redirect = '/data-access/' + id;
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Archive failed.');
      });
  }

  static unarchive(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_archive';
    let redirect = '/data-access/' + id;
    axios.delete(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Unarchive failed.');
      });
  }

  static linkVariables(id, type, aId) {
    let url = '/ws/data-access-request/' + id;
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = url + '/' + type + '/' + aId;
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    url = url + '/variables';
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Linking variables failed.');
      });
  }

  static unlinkVariables(id, type, aId) {
    let url = '/ws/data-access-request/' + id;
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = url + '/' + type + '/' + aId;
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    url = url + '/variables';
    axios.delete(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Unlink variables failed.');
      });
  }

  static submit(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=SUBMITTED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=SUBMITTED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Submission failed.');
      });
  }

  static reopen(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=OPENED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=OPENED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Reopen failed.');
      });
  }

  static review(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=REVIEWED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=REVIEWED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Review failed.');
      });
  }

  static approve(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=APPROVED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=APPROVED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Approval failed.');
      });
  }

  static conditionally(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=CONDITIONALLY_APPROVED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=CONDITIONALLY_APPROVED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Conditional approval failed.');
      });
  };

  static reject(id, type, aId) {
    let url = '/ws/data-access-request/' + id + '/_status?to=REJECTED';
    let redirect = '/data-access-form/' + id;
    if (type && aId) {
      url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/_status?to=REJECTED';
      redirect = '/data-access-' + type + '-form/' + aId;
    }
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Rejection failed.');
      });
  }

  static sendComment(id, message, isPrivate) {
    let url = '/ws/data-access-request/' + id + '/comments';
    let redirect = '/data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '/data-access-private-comments/' + id;
    }
    axios({
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      url: MicaService.normalizeUrl(url),
      data: message
    })
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Sending comment failed.');
      });
  }

  static deleteComment(id, cid, isPrivate) {
    let url = '/ws/data-access-request/' + id + '/comment/' + cid;
    let redirect = '/data-access-comments/' + id;
    if (isPrivate) {
      url = url + '?admin=true';
      redirect = '/data-access-private-comments/' + id;
    }
    axios.delete(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Deleting comment failed.');
      });
  }

  static addAction(id, action) {
    //console.dir(action);
    let url = '/ws/data-access-request/' + id + '/_log-actions';
    let redirect = '/data-access-history/' + id;
    axios.post(MicaService.normalizeUrl(url), action)
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Adding action failed.');
      });
  }

  static setStartDate(id, startDate) {
    //console.log(startDate);
    let url = '/ws/data-access-request/' + id + '/_start-date?date=' + startDate;
    let redirect = '/data-access/' + id;
    axios.put(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('Setting start date failed.');
      });
  }

  static deleteAttachment(id, fileId) {
    let url = '/ws/data-access-request/' + id + '/attachments/' + fileId;
    let redirect = '/data-access-documents/' + id;
    axios.delete(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('File deletion failed.');
      });
  }

  static attachFile(id, fileId) {
    let url = '/ws/data-access-request/' + id + '/attachments/' + fileId;
    let redirect = '/data-access-documents/' + id;
    axios.post(MicaService.normalizeUrl(url))
      .then(() => {
        //console.dir(response);
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      })
      .catch(response => {
        console.dir(response);
        MicaService.toastError('File attachment failed.');
      });
  }

}
