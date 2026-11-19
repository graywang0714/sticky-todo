const {spawnSync}=require('node:child_process'),fs=require('node:fs'),path=require('node:path');
const endpoint=process.env.VITE_SUPABASE_URL, key=process.env.VITE_SUPABASE_PUBLISHABLE_KEY||'';
const clientKey=key.startsWith('sb_publishable_')||(()=>{try{return JSON.parse(Buffer.from(key.split('.')[1],'base64url')).role==='anon'}catch{return false}})();
if(!/^https:\/\/[a-z0-9]+\.supabase\.co$/.test(endpoint||'')||!clientKey)throw Error('Supply the project URL and a public client key before building the Windows renderer.');
const root=path.resolve(__dirname,'..');
function run(js,args){const r=spawnSync(process.execPath,[path.join(root,'node_modules',js),...args],{cwd:root,env:process.env,stdio:'inherit'});if(r.status!==0)process.exit(r.status||1)}
run('typescript/bin/tsc',['-b']);run('vite/bin/vite.js',['build','--outDir','windows/renderer','--emptyOutDir']);
