import { useState } from 'react'
import { store } from '@/lib/store'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function LoginScreen() {
  const [mode, setMode] = useState<'login' | 'register' | 'recover'>('login')
  const [email, setEmail] = useState('')
  const [verify,setVerify]=useState(false);const [proof,setProof]=useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [tip, setTip] = useState('')

  const submit = async () => {
    if(busy)return;setBusy(true);setError("");setTip("");try{const mail=email.trim();if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(mail))throw Error("请输入有效邮箱");if(mode!=="login"&&!verify){if(mode==="recover")await store.beginRecovery(mail);else await store.beginSignup(mail);setVerify(true);setTip("请查看邮箱，输入验证码或粘贴未打开的验证链接。");return}if(!password||(mode!=="login"&&password.length<8))throw Error("请输入密码，新密码至少 8 位");const err=mode==="login"?await store.signIn(mail,password):await store.confirmSignup(mail,password,proof.trim(),mode==="recover");if(err)setError(err);else if(mode==="recover"){setMode("login");setPassword("");setProof("");setVerify(false);setTip("密码已更新，请使用新密码登录。")}}catch(e){if((e as {code?:string})?.code==="same_password"){setMode("login");setPassword("");setProof("");setVerify(false);setTip("邮箱已验证。刚才填写的是这个账号的原密码，请直接登录。")}else setError(e instanceof Error?e.message:"请求失败")}finally{setBusy(false)}
  }

  return (
    <div className="min-h-screen w-full bg-[#f7e98e] bg-[radial-gradient(circle_at_30%_20%,#faf0ae,#f3e27f)] flex items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="text-5xl mb-3">📒</div>
          <h1 className="text-2xl font-extrabold text-stone-700">Sticky Notes</h1>
          <p className="text-sm text-stone-500 mt-1">随手记，随处见</p>
        </div>
        <div className="bg-white rounded-2xl shadow-lg border border-amber-100 p-6 space-y-4">
          <div className="flex bg-stone-100 rounded-lg p-1">
            {(['login', 'register'] as const).map(m => (
              <button disabled={busy} key={m} onClick={() => { setVerify(false);setProof("");setPassword("");setMode(m); setError(''); setTip('') }}
                className={`flex-1 py-1.5 rounded-md text-sm transition ${mode === m ? 'bg-amber-500 text-white font-medium shadow-sm' : 'text-stone-500'}`}>
                {m === 'login' ? '登录' : '注册'}
              </button>
            ))}
          </div>
          {mode==="recover"&&<h2 className="font-semibold">找回密码</h2>}
          <Input type="email" placeholder="邮箱" disabled={busy||verify} value={email}
            onChange={e => setEmail(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && submit()}
            className="bg-stone-50" />
          {verify&&<Input placeholder="邮箱验证码或验证链接" type="password" value={proof} onChange={e=>setProof(e.target.value)}/>}
          {(mode==="login"||verify)&&<Input type="password" placeholder={mode==="login"?"密码":"新密码（至少 8 位）"} value={password}
            onChange={e => setPassword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && submit()}
            className="bg-stone-50" />}
          {error && <div className="text-sm text-red-500">{error}</div>}
          {tip && <div className="text-sm text-emerald-600">{tip}</div>}
          <Button onClick={submit} disabled={busy}
            className="w-full bg-amber-500 hover:bg-amber-600 text-white">
            {busy ? '请稍候…' : mode === 'login' ? '登录' : verify?(mode==='recover'?'保存新密码':'验证并设置密码'):(mode==='recover'?'发送恢复邮件':'发送验证邮件')}
          </Button>
          {mode==="login"&&<button disabled={busy} onClick={()=>{setMode("recover");setVerify(false);setProof("");setPassword("");setError("");setTip("")}} className="text-sm text-amber-800">忘记密码？</button>}
          <p className="text-xs text-stone-400 text-center">你的数据仅自己可见，云端加密存储</p>
        </div>
      </div>
    </div>
  )
}
