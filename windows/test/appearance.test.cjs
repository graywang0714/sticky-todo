const {test}=require('node:test'),assert=require('node:assert/strict');
const {normalize,css}=require('../appearance.cjs');
test('zero and fully opaque are not replaced by defaults',()=>{for(const n of [0,100])assert.equal(normalize({custom:true,bg:'#abcdef',fg:'#fedcba',opacity:n}).opacity,n)});
test('untrusted CSS values cannot enter the stylesheet',()=>{const x=normalize({custom:true,fg:'red;display:none',bg:'#123456',opacity:700});assert.equal(x.fg,'#ffffff');assert.equal(x.opacity,100);assert.doesNotMatch(css(x),/display:none/)});
test('default preserves wallpaper alpha and has an always-visible pinned badge',()=>{const s=css(normalize({}));assert.doesNotMatch(s,/--panel-fill/);assert.doesNotMatch(s,/content:\s*"置顶"/);});
test('custom opacity applies to panel paint and never the whole window',()=>{const s=css({custom:true,bg:'#123456',fg:'#123abc',opacity:0});assert.match(s,/rgba\(18,52,86,0\)/);assert.match(s,/#123abc/);assert.doesNotMatch(s,/^\s*opacity:/m)});