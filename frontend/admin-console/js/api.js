/**
 * 同源 Cookie 会话 + Spring CSRF（XSRF-TOKEN Cookie + X-XSRF-TOKEN 头）
 */
window.CcecApi = {
  _csrfHeader: null,
  _csrfToken: null,

  async ensureCsrf() {
    const r = await fetch('/api/v1/auth/csrf', { credentials: 'include' });
    if (!r.ok) throw new Error('csrf');
    const j = await r.json();
    this._csrfHeader = j.headerName || 'X-XSRF-TOKEN';
    this._csrfToken = j.token || '';
    return j;
  },

  csrfHeaders() {
    const h = {};
    if (this._csrfHeader && this._csrfToken) {
      h[this._csrfHeader] = this._csrfToken;
    }
    return h;
  },

  readXsrfFromCookie() {
    const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    if (!m) return '';
    try {
      return decodeURIComponent(m[1]);
    } catch {
      return m[1];
    }
  },

  async authFetch(url, options = {}) {
    await this.ensureCsrf();
    const tokenFromCookie = this.readXsrfFromCookie();
    const hdr = { ...(options.headers || {}) };
    if (tokenFromCookie && !hdr[this._csrfHeader]) {
      hdr[this._csrfHeader] = tokenFromCookie;
    }
    Object.assign(hdr, this.csrfHeaders());
    return fetch(url, {
      credentials: 'include',
      ...options,
      headers: hdr
    });
  }
};
