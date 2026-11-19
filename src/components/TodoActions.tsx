import {Check,Pin,PinOff,X} from "lucide-react"
import {store,type Todo} from "../lib/store"
export function TodoActions({t}:{t:Todo}){return <span className="inline-flex items-center gap-0.5 shrink-0 text-white">                    <button className="p-1 rounded-full bg-emerald-500/90 hover:bg-emerald-400" title="完成"
                      onClick={() => store.update(t.id, { done: true })}>
                      <Check className="w-3 h-3" />
                    </button>
                    <button className="p-1 rounded-full bg-amber-500/90 hover:bg-amber-400" title={t.pinned ? '取消置顶' : '置顶'}
                      onClick={() => store.update(t.id, { pinned: !t.pinned })}>
                      {t.pinned ? <PinOff className="w-3 h-3" /> : <Pin className="w-3 h-3" />}
                    </button>
                    <button className="p-1 rounded-full bg-red-500/90 hover:bg-red-400" title="删除"
                      onClick={() => store.remove(t.id)}>
                      <X className="w-3 h-3" />
                    </button></span>}
