const BACKEND_URL = 'http://34.59.79.51:8080';

export default async function handler(req, res) {
  const { path } = req.query;
  const targetPath = Array.isArray(path) ? path.join('/') : path;

  // Build target URL with query params
  const queryString = new URLSearchParams(
    Object.entries(req.query).filter(([key]) => key !== 'path')
  ).toString();
  const targetUrl = `${BACKEND_URL}/api/${targetPath}${queryString ? '?' + queryString : ''}`;

  // Forward headers (exclude host)
  const forwardHeaders = {
    'Content-Type': req.headers['content-type'] || 'application/json',
  };
  if (req.headers['authorization']) {
    forwardHeaders['Authorization'] = req.headers['authorization'];
  }

  const fetchOptions = {
    method: req.method,
    headers: forwardHeaders,
  };

  // Forward body for non-GET requests
  if (!['GET', 'HEAD'].includes(req.method) && req.body) {
    fetchOptions.body = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
  }

  try {
    const response = await fetch(targetUrl, fetchOptions);
    const contentType = response.headers.get('content-type') || '';

    // Forward response headers
    response.headers.forEach((value, key) => {
      if (!['transfer-encoding', 'connection', 'keep-alive'].includes(key.toLowerCase())) {
        res.setHeader(key, value);
      }
    });

    res.status(response.status);

    if (contentType.includes('application/json')) {
      const data = await response.json();
      res.json(data);
    } else {
      const buffer = await response.arrayBuffer();
      res.send(Buffer.from(buffer));
    }
  } catch (error) {
    console.error('Proxy error:', error);
    res.status(502).json({ error: 'Backend unreachable', detail: error.message });
  }
}
