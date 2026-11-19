const by=id=>document.getElementById(id), ids=['bg','fg','alpha'];let saved;
function current(){return {custom:!by('auto').checked,bg:by('bg').value,fg:by('fg').value,opacity:100-Number(by('alpha').value)}}
function luma(hex){const c=[1,3,5].map(x=>parseInt(hex.slice(x,x+2),16)/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4);return .2126*c[0]+.7152*c[1]+.0722*c[2]}
function preview(){const v=current();const fg=v.custom?v.fg:'#ffffff',bg=v.custom?v.bg:'#000000',alpha=v.custom?v.opacity:30;by('example').style.backgroundColor=bg+Math.round(alpha*2.55).toString(16).padStart(2,'0');by('example').style.color=fg;by('value').value=100-v.opacity+'%';const L=[luma(bg),luma(fg)].sort((a,b)=>b-a);by('feedback').textContent=v.custom&&(L[0]+.05)/(L[1]+.05)<2.5?'文字与底色接近，可以拉大色差或调整透明度。':''}
function load(v){by('auto').checked=!v.custom;by('bg').value=v.bg;by('fg').value=v.fg;by('alpha').value=100-v.opacity;preview()}
by('auto').onchange=preview;for(const id of ids)by(id).oninput=()=>{by('auto').checked=false;preview()};
by('reset').onclick=()=>load({custom:false,bg:'#000000',fg:'#ffffff',opacity:30});
by('save').onclick=async()=>{by('save').disabled=true;try{saved=await window.appearanceSettings.save(current());load(saved);by('feedback').textContent='已保存，两个浮窗已更新。'}catch{by('feedback').textContent='保存失败，请重试。'}finally{by('save').disabled=false}};
window.appearanceSettings.read().then(v=>{saved=v;load(v)}).catch(()=>{by('save').disabled=true;by('feedback').textContent='读取设置失败，请重新打开。'});