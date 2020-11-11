// 计算登录密码
const crypto = require('crypto');

let user = process.argv[2];
let pass = process.argv[3];

if (!user) {
  console.log("Usage: node pass USERNAME PASSWORD");
  return;
}

const rand = crypto.randomBytes(16);
const md5 = crypto.createHash('md5');

md5.update(rand);
md5.update(user);
md5.update(pass);

let lpass = Buffer.concat([ rand, md5.digest() ]).toString('base64');

console.log('user name:', user);
console.log('password :', lpass);