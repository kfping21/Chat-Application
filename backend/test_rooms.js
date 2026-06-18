const http = require('http');
const jwt = require('jsonwebtoken');

const token = jwt.sign({userId: '6a200941cf382ffa0a90cd89'}, 'treehole_dev_secret_key_2024_do_not_use_in_production');

const options = {
  hostname: 'localhost',
  port: 3001,
  path: '/api/party/rooms',
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + token
  }
};

const req = http.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });
  res.on('end', () => {
    console.log(JSON.stringify(JSON.parse(data), null, 2));
  });
});

req.on('error', (e) => {
  console.error(e);
});
req.end();
