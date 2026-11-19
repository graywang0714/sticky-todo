import {useEffect,useRef} from 'react'
import {Pin} from 'lucide-react'
import {store} from '../lib/store'

type Slot={el:HTMLElement;top:number;height:number}
export function ReorderMark({id,pinned,kind="todos"}:{id:string,pinned:boolean,kind?:"todos"|"notes"}){
 const stop=useRef<null|(()=>void)>(null)
 useEffect(()=>()=>stop.current?.(),[])
 return <button type="button" className="sticky-mark shrink-0" title="按住上下拖动排序" aria-label={'拖动'+(pinned?'置顶':'')+(kind==='notes'?'随笔':'待办')+'排序'}
 onClick={e=>{e.preventDefault();e.stopPropagation()}}
 onPointerDown={e=>{
  if(e.button!==0||!e.isPrimary)return
  const handle=e.currentTarget,list=handle.closest<HTMLElement>('[data-todo-list]');if(!list)return
  stop.current?.();e.preventDefault();e.stopPropagation()
  const slots:Slot[]=Array.from(list.querySelectorAll<HTMLElement>('[data-todo-id]')).filter(r=>r.dataset.pinned===String(pinned)).map(el=>{const r=el.getBoundingClientRect();return {el,top:r.top,height:r.height}})
  const i=slots.findIndex(x=>x.el.dataset.todoId===id);if(i<0)return
  const own=slots[i],start=e.clientY,pointer=e.pointerId
  let y=start,to=i,held=false,frame=0,container:HTMLElement|null=list.parentElement
  while(container&&!/(auto|scroll)/.test(getComputedStyle(container).overflowY))container=container.parentElement
  const scroller=container||document.scrollingElement as HTMLElement|null,initialScroll=scroller?.scrollTop||0
  const gap=slots.length<2?0:Math.max(0,slots[1].top-slots[0].top-slots[0].height)
  const paint=()=>{if(!held)return
   const dy=Math.max(slots[0].top-own.top,Math.min(slots.at(-1)!.top+slots.at(-1)!.height-own.height-own.top,y-start+(scroller?.scrollTop||0)-initialScroll))
   let next=i
   if(dy<0){for(let n=i-1;n>=0&&own.top+dy<=slots[n].top+slots[n].height/2;n--)next=n}
   if(dy>0){for(let n=i+1;n<slots.length&&own.top+own.height+dy>=slots[n].top+slots[n].height/2;n++)next=n}
   to=next;for(let n=0;n<slots.length;n++){const shift=n===i?dy:n>=to&&n<i?own.height+gap:n<=to&&n>i?-own.height-gap:0;slots[n].el.style.transform='translate3d(0,'+shift+'px,0)'}
  }
  const tick=()=>{if(scroller&&held&&Math.abs(y-start)>3){const b=scroller===document.scrollingElement?{top:0,bottom:innerHeight}:scroller.getBoundingClientRect();if(y<b.top+24)scroller.scrollTop-=7;else if(y>b.bottom-24)scroller.scrollTop+=7}paint();frame=requestAnimationFrame(tick)}
  const begin=()=>{if(held)return;held=true;list.classList.add('reorder-live');own.el.classList.add('reorder-held');tick()}
  const timer=window.setTimeout(begin,220)
  function move(ev:PointerEvent){if(ev.pointerId!==pointer)return;y=ev.clientY;if(Math.abs(y-start)>=3)begin();paint()}
  function end(ev:PointerEvent){if(ev.pointerId!==pointer)return;y=ev.clientY;paint();const dest=slots[to]?.el.dataset.todoId;const commit=held&&dest&&to!==i;cleanup();if(commit)kind==="notes"?store.moveNoteOnto(id,dest!):store.moveOnto(id,dest!)}
  function cancel(){cleanup()}
  function key(ev:KeyboardEvent){if(ev.key==='Escape')cancel()}
  function cleanup(){clearTimeout(timer);cancelAnimationFrame(frame);window.removeEventListener('pointermove',move,true);window.removeEventListener('pointerup',end,true);window.removeEventListener('pointercancel',cancel,true);window.removeEventListener('blur',cancel);window.removeEventListener('keydown',key);handle.removeEventListener('lostpointercapture',cancel);if(handle.hasPointerCapture(pointer))handle.releasePointerCapture(pointer);list!.classList.remove('reorder-live');own.el.classList.remove('reorder-held');for(const slot of slots)slot.el.style.removeProperty('transform');stop.current=null}
  stop.current=cancel;handle.setPointerCapture(pointer);handle.addEventListener('lostpointercapture',cancel);window.addEventListener('pointermove',move,true);window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',cancel,true);window.addEventListener('blur',cancel);window.addEventListener('keydown',key)
 }}
 onKeyDown={e=>{if(e.altKey&&['ArrowUp','ArrowDown'].includes(e.key)){e.preventDefault();if(kind==='notes'){const group=store.sortedNotes.filter(n=>n.pinned===pinned);const target=group[group.findIndex(n=>n.id===id)+(e.key==='ArrowUp'?-1:1)];if(target)store.moveNoteOnto(id,target.id)}else store.move(id,e.key==='ArrowUp'?-1:1)}}}>
 {pinned?<Pin size={15} className="text-amber-400"/>:<i/>}</button>
}