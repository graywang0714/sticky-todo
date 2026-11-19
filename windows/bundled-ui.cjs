// Serve our packaged client at the app's own historical origin inside Electron.
// This preserves its Chromium profile, including the original offline cache.
// No system proxy, certificate installation, remote deployment or other profile is involved.
const {session,net}=require('electron'),fs=require('node:fs'),path=require('node:path')
const origin='https://sticky-notes.invalid',prefix='/sticky-todo/'
function init(passThrough=req=>net.fetch(req,{bypassCustomProtocolHandlers:true})){const root=path.join(__dirname,'renderer'),map=new Map()
 function walk(dir,base){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const name=base+e.name,p=path.join(dir,e.name);if(e.isDirectory())walk(p,name+'/');else map.set(prefix+name,p)}}
 walk(root,'');map.set(prefix,path.join(root,'index.html'))
 const mime={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png','.webmanifest':'application/manifest+json','.svg':'image/svg+xml'}
 session.defaultSession.protocol.handle('https',req=>{const u=new URL(req.url)
 if(u.origin!==origin)return passThrough(req)
 const f=map.get(u.pathname);if(!f||!['GET','HEAD'].includes(req.method))return new Response(null,{status:404})
 return new Response(req.method==='HEAD'?null:fs.readFileSync(f),{headers:{'content-type':mime[path.extname(f)]||'application/octet-stream','cache-control':'no-store','x-content-type-options':'nosniff','content-security-policy':"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src https://*.supabase.co wss://*.supabase.co; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"}})
 })}
function protect(win){win.webContents.setWindowOpenHandler(()=>({action:'deny'}));win.webContents.on('will-navigate',(e,url)=>{const u=new URL(url);if(u.origin!==origin||!u.pathname.startsWith(prefix))e.preventDefault()})}
module.exports={init,protect}
