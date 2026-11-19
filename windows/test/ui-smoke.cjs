// Run with the Electron executable, in a separate disposable userData profile.
const {app,BrowserWindow}=require('electron');const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');const prefs=require('../preferences.cjs'),{css}=require('../appearance.cjs');
const profile=fs.mkdtempSync(path.join(os.tmpdir(),'sticky-settings-qa-'));app.setPath('userData',profile);app.disableHardwareAcceleration();
const out=process.env.STICKY_QA_OUT;fs.mkdirSync(out,{recursive:true});let finished=false;
const timer=setTimeout(()=>{if(!finished){fs.writeFileSync(path.join(out,'desktop-ui-error.txt'),'timeout');app.exit(2)}},35000)
async function wait(check){for(let n=0;n<90;n++){if(await check())return;await new Promise(r=>setTimeout(r,90))}throw Error('UI not ready')}
app.whenReady().then(async()=>{prefs.init();prefs.open();let editor=BrowserWindow.getAllWindows()[0];editor.hide();await wait(()=>editor.webContents.getURL().startsWith('file:')&&!editor.webContents.isLoading());
 const run=s=>editor.webContents.executeJavaScript(s);
 await wait(()=>run('document.querySelector("#auto")?.checked===true'));
 await run("document.querySelector('#bg').value='#345678';document.querySelector('#fg').value='#ffffff';let s=document.querySelector('#alpha');s.value=60;s.dispatchEvent(new Event('input'));document.querySelector('#save').click()");
 await wait(()=>run("document.querySelector('#feedback').textContent.includes('已保存')"));
 const saved=JSON.parse(fs.readFileSync(path.join(profile,'float-appearance-v1.json')));assert.deepEqual(saved,{custom:true,bg:'#345678',fg:'#ffffff',opacity:40});
 // Hidden Electron windows need no capture surface for property assertions.
 const demo=new BrowserWindow({show:false,width:400,height:180});
 prefs.attach(demo);await demo.loadURL('data:text/html;charset=utf-8,'+encodeURIComponent('<html class="desktop-float"><div id="root"><div class="h-screen select-none"><div class="group"><button title="取消置顶"></button><span class="cursor-text">准备会议</span></div><div class="group"><button title="置顶"></button><span class="cursor-text">普通事项</span></div></div></div></html>'));
 await demo.webContents.insertCSS(css(saved),{cssOrigin:'user'});const v=await demo.webContents.executeJavaScript("(()=>{const p=document.querySelector('.h-screen'),s=document.querySelectorAll('.cursor-text');return [getComputedStyle(p).backgroundColor,getComputedStyle(s[0]).color,getComputedStyle(s[0],'::before').content,getComputedStyle(s[1],'::before').content,getComputedStyle(s[0]).opacity]})()");assert.deepEqual(v,['rgba(52, 86, 120, 0.4)','rgb(255, 255, 255)','none','none','1']);
 await run("document.querySelector('#reset').click();document.querySelector('#save').click()");await wait(()=>!JSON.parse(fs.readFileSync(path.join(profile,'float-appearance-v1.json'))).custom);
 fs.writeFileSync(path.join(out,'desktop-ui.json'),JSON.stringify({ok:true,nativeSettingsIPC:true,persisted:saved,computed:v,reset:true}));finished=true;clearTimeout(timer);app.exit(0)
}).catch(e=>{fs.writeFileSync(path.join(out,'desktop-ui-error.txt'),String(e.stack));app.exit(1)})