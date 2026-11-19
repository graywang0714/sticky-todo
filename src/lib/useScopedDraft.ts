import {useState,useRef} from 'react'
import type {SetStateAction} from 'react'
import {store} from './store'
// The key is captured by this editor instance, not looked up after a switch.
export function useScopedDraft<T>(name:string,initial:T):[T,(next:SetStateAction<T>)=>void] {
 const key=store.draftKey(name)
 const [value,set]=useState<T>(()=>{if(!key)return initial;try{const raw=localStorage.getItem(key);return raw===null?initial:JSON.parse(raw)}catch{return initial}})
 const latest=useRef(value)
 return [value,(next)=>{if(!key||key!==store.draftKey(name))return;const v=typeof next==='function'?(next as (x:T)=>T)(latest.current):next
 if(v===initial)localStorage.removeItem(key);else localStorage.setItem(key,JSON.stringify(v));latest.current=v;set(v)}]
}
