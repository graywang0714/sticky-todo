const defaults=Object.freeze({custom:false,bg:'#000000',fg:'#ffffff',opacity:30});
const color=(v,f)=>typeof v==='string'&&/^#[0-9a-f]{6}$/i.test(v)?v.toLowerCase():f;
function normalize(v={}){v=v&&typeof v==='object'?v:{};return {custom:v.custom===true,bg:color(v.bg,defaults.bg),fg:color(v.fg,defaults.fg),opacity:Number.isFinite(v.opacity)?Math.min(100,Math.max(0,Math.round(v.opacity))):defaults.opacity}}
function css(raw){const p=normalize(raw),root='html.desktop-float #root .h-screen.select-none';
 const badge="";
 if(!p.custom)return badge;
 const rgb=[1,3,5].map(i=>parseInt(p.bg.substring(i,i+2),16)).join(',');
 return badge+root+'{--panel-fill:rgba('+rgb+','+(p.opacity/100)+');background-color:var(--panel-fill)!important;color:'+p.fg+'!important;text-shadow:none!important;color-scheme:normal;}'+
 root+' [class*="text-white"],'+root+' button:not([class*="bg-"]),'+root+' input,'+root+' textarea{color:'+p.fg+'!important;}'+
 root+' .rounded-full.bg-white\\/60{background-color:'+p.fg+'!important;}'+
 root+' input::placeholder,'+root+' textarea::placeholder{color:'+p.fg+'!important;opacity:.7;}'+
 root+' .absolute.inset-0{background:'+p.bg+'!important;color:'+p.fg+'!important;}'+
 root+' input,'+root+' textarea{background:color-mix(in srgb,'+p.fg+' 9%,'+p.bg+')!important;border-color:color-mix(in srgb,'+p.fg+' 40%,'+p.bg+')!important;}'+
 root+' [class*="hover:bg-white"]:hover{background-color:color-mix(in srgb,'+p.fg+' 16%,transparent)!important;}';
}
module.exports={defaults,normalize,css};