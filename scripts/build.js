const path = require('path');
const fse = require('fs-extra')

const main = require('pkg-lumo').main;

const lumoVersion = '1.8.0-beta';
const nodeVersion = '8';
const abiVersion = '57';

const modules = [
  `node_modules/deasync/bin/${process.platform}-${process.arch}-node-${nodeVersion}/deasync.node`,
  `node_modules/sqlite3/lib/binding/node-v${abiVersion}-${process.platform}-${process.arch}/node_sqlite3.node`
];

modules.forEach(m => fse.copySync(m, `resources/${path.basename(m)}`));

main({
  classpath: 'src',
  resourceDirs: 'resources',
  mainNsName: 'closh.main',
});

fse.moveSync('my-lumo', 'closh');
