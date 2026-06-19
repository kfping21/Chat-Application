const https = require('https');

const options = {
  hostname: 'api.siliconflow.cn',
  path: '/v1/models',
  method: 'GET',
  headers: {
    'Authorization': 'Bearer sk-ssjyovbqzrxwftpbmwcamovccvomwccbektsvbrxqhzomzml'
  }
};

const req = https.request(options, (res) => {
  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });
  res.on('end', () => {
    try {
      const json = JSON.parse(data);
      if (json.data) {
        console.log("Total models:", json.data.length);
        const qwenModels = json.data.filter(m => m.id.toLowerCase().includes('vl'));
        console.log("Vision/VL Models available:");
        qwenModels.forEach(m => console.log(m.id));
      } else {
        console.log("Error:", json);
      }
    } catch (e) {
      console.log("Parse error:", e.message);
    }
  });
});

req.on('error', (e) => {
  console.error("Request error:", e);
});

req.end();
