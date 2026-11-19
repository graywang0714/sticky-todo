import {useEffect,useSyncExternalStore} from 'react'
import { Routes, Route } from 'react-router'
import Home from './pages/Home'
import DesktopFloat from './pages/DesktopFloat'
import LoginScreen from './pages/LoginScreen'
import {store} from './lib/store'

export default function App(){
 useSyncExternalStore(store.subscribe,()=>store.revision)
 useEffect(()=>{void store.init()},[])
 if(!store.visible)return store.status==='connecting'?<div role="status" className="min-h-screen bg-amber-50 p-6 text-stone-700">正在确认账号…</div>:<LoginScreen/>
 // Remount the entire editor tree (including draft fields) at identity boundaries.
 return <Routes key={store.identity}><Route path="/" element={<Home/>}/><Route path="/desktop" element={<DesktopFloat/>}/></Routes>
}