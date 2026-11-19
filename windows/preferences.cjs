const {app,BrowserWindow,ipcMain,Menu}=require('electron');
const path=require('node:path'),fs=require('node:fs');const {pathToFileURL}=require('node:url');
const {defaults,normalize,css}=require('./appearance.cjs');
const local=path.join(__dirname,'settings.html'),localUrl=pathToFileURL(local).href;
const windows=new Set(), slots=new WeakMap();let prefs=defaults, editor=null;
const file=()=>path.join(app.getPath('userData'),'float-appearance-v1.json');
function render(win){const s=slots.get(win);if(!s)return;const rev=++s.rev;
 s.tail=s.tail.catch(()=>{}).then(async()=>{if(win.isDestroyed()||s.rev!==rev)return;
 const wc=win.webContents;const u=wc.getURL();if(!u.startsWith('https://sticky-notes.invalid/sticky-todo/')||!u.includes('#/desktop'))return;
 const next=await wc.insertCSS(css(prefs),{cssOrigin:'user'});const prev=s.key;s.key=next;if(prev)await wc.removeInsertedCSS(prev).catch(()=>{})}).catch(()=>{});}
function attach(win){windows.add(win);slots.set(win,{rev:0,key:'',tail:Promise.resolve()});win.webContents.on('did-finish-load',()=>render(win));
 win.webContents.on('context-menu',()=>Menu.buildFromTemplate([{label:'浮窗外观…',click:open}]).popup({window:win}));win.once('closed',()=>windows.delete(win));}
function allowed(e){return editor&&!editor.isDestroyed()&&e.sender===editor.webContents&&e.senderFrame===e.sender.mainFrame&&e.senderFrame.url===localUrl;}
function init(){try{prefs=normalize(JSON.parse(fs.readFileSync(file(),'utf8')))}catch{prefs=defaults}
 ipcMain.handle('float-pref:get',e=>{if(!allowed(e))throw Error('invalid source');return prefs});
 ipcMain.handle('float-pref:save',(e,v)=>{if(!allowed(e))throw Error('invalid source');const val=normalize(v),tmp=file()+'.tmp';fs.writeFileSync(tmp,JSON.stringify(val),'utf8');fs.renameSync(tmp,file());prefs=val;for(const w of windows)render(w);return val})}
function open(){if(editor&&!editor.isDestroyed()){editor.show();editor.focus();return}editor=new BrowserWindow({width:520,height:650,minWidth:400,minHeight:480,autoHideMenuBar:true,title:'浮窗外观 · Sticky Notes',backgroundColor:'#faf8f1',webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false,preload:path.join(__dirname,'settings-preload.cjs')}});editor.webContents.setWindowOpenHandler(()=>({action:'deny'}));editor.webContents.on('will-navigate',(e,u)=>{if(u!==localUrl)e.preventDefault()});editor.loadFile(local);editor.on('closed',()=>{editor=null})}
module.exports={init,open,attach};