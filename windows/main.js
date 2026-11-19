const appearance = require('./preferences.cjs')
const { app, BrowserWindow, globalShortcut, Tray, Menu, nativeImage, screen } = require('electron')
const { spawnSync } = require('child_process')
const path = require('path')
const fs = require('fs')

const BASE = 'https://sticky-notes.invalid/sticky-todo/#/desktop'
const boundsFile = () => path.join(app.getPath('userData'), 'window-bounds.json')
function loadBounds() {
  try { return JSON.parse(fs.readFileSync(boundsFile(), 'utf8')) } catch { return {} }
}
function saveBounds(b) {
  try { fs.writeFileSync(boundsFile(), JSON.stringify(b)) } catch { /* ignore */ }
}

const windows = {}
let tray = null
let topMost = false

// 单实例锁定：重复启动时只显示已有窗口，不再开新进程
const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const wins = Object.values(windows)
    if (wins.length === 0) createAll()
    else wins.forEach(w => { w.show(); })
  })
}

function makeWindow(key, url, def) {
  const saved = loadBounds()[key] || {}
  const win = new BrowserWindow({
    width: saved.width || def.width,
    height: saved.height || def.height,
    x: saved.x !== undefined ? saved.x : def.x,
    y: saved.y !== undefined ? saved.y : def.y,
    frame: false,
    transparent: true,
    alwaysOnTop: false,     // 普通层级，可被其他窗口覆盖
    skipTaskbar: true,
    resizable: true,
    hasShadow: false,
    autoHideMenuBar: true,
    title: 'Sticky Notes',
    webPreferences: { contextIsolation: true, preload: path.join(__dirname, 'preload.js') },
  })
  require('./bundled-ui.cjs').protect(win)
  appearance.attach(win)
  win.loadURL(url)
  const persist = () => {
    if (win.isDestroyed()) return
    const b = win.getBounds()
    const all = loadBounds()
    all[key] = b
    saveBounds(all)
  }
  win.on('moved', () => { persist(); pushBrightness(win) })
  win.on('resized', persist)
  win.on('show', () => pushBrightness(win))
  win.on('closed', () => { delete windows[key] })
  windows[key] = win
  return win
}

/** ============ 壁纸亮度检测（读壁纸文件，不受前台窗口影响） ============ */
let wpCache = { path: null, style: null, mtime: 0, img: null }
let wpInfoCache = { t: 0, info: null }
const flipState = new WeakMap() // win -> { sent, pending, count }
const PS_EXE = 'C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe'

function getWallpaperInfo() {
  // 30 秒缓存，避免频繁拉起 PowerShell
  if (Date.now() - wpInfoCache.t < 30000 && wpInfoCache.info) return wpInfoCache.info
  try {
    const r = spawnSync(PS_EXE, ['-NoProfile', '-Command',
      "(Get-ItemProperty 'HKCU:\\Control Panel\\Desktop').Wallpaper; (Get-ItemProperty 'HKCU:\\Control Panel\\Desktop').WallpaperStyle"],
      { encoding: 'utf8', timeout: 5000 })
    const lines = (r.stdout || '').split(/\r?\n/).map(s => s.trim()).filter(Boolean)
    const p = lines[0]
    const style = lines[1] || '10'
    const info = (p && fs.existsSync(p)) ? { path: p, style } : null
    wpInfoCache = { t: Date.now(), info }
    return info
  } catch { return null }
}

/** 计算浮窗区域在壁纸图片上对应的平均亮度（0-255），失败返回 null */
function wallpaperLuminance(win) {
  const info = getWallpaperInfo()
  if (!info) return null
  try {
    const mtime = fs.statSync(info.path).mtimeMs
    if (!wpCache.img || wpCache.path !== info.path || wpCache.style !== info.style || wpCache.mtime !== mtime) {
      wpCache = { path: info.path, style: info.style, mtime, img: nativeImage.createFromPath(info.path) }
    }
    const img = wpCache.img
    if (img.isEmpty()) return null
    const d = screen.getDisplayMatching(win.getBounds())
    const b = win.getBounds()
    const iw = img.getSize().width
    const ih = img.getSize().height
    const dw = d.size.width
    const dh = d.size.height
    // 壁纸在屏幕上的显示映射（支持 填充10 / 适应6 / 拉伸2 / 居中0）
    let scaleX, scaleY, offX = 0, offY = 0
    if (info.style === '2') {          // 拉伸
      scaleX = dw / iw; scaleY = dh / ih
    } else if (info.style === '6') {   // 适应（保持比例，可能有黑边）
      scaleX = scaleY = Math.min(dw / iw, dh / ih)
      offX = (dw - iw * scaleX) / 2; offY = (dh - ih * scaleY) / 2
    } else if (info.style === '0') {   // 居中
      scaleX = scaleY = 1
      offX = (dw - iw) / 2; offY = (dh - ih) / 2
    } else {                           // 填充（默认 10）
      scaleX = scaleY = Math.max(dw / iw, dh / ih)
      offX = (dw - iw * scaleX) / 2; offY = (dh - ih * scaleY) / 2
    }
    // 窗口屏幕坐标 → 壁纸图片坐标
    const rx = (b.x - d.bounds.x - offX) / scaleX
    const ry = (b.y - d.bounds.y - offY) / scaleY
    const rw = b.width / scaleX
    const rh = b.height / scaleY
    // 与图片区域求交集
    const x0 = Math.max(0, Math.floor(rx))
    const y0 = Math.max(0, Math.floor(ry))
    const x1 = Math.min(iw, Math.ceil(rx + rw))
    const y1 = Math.min(ih, Math.ceil(ry + rh))
    if (x1 - x0 <= 2 || y1 - y0 <= 2) return null
    // 缩样后取亮度（最多 40x40，避免大图卡顿）
    const ds = Math.max(1, Math.floor(Math.max(x1 - x0, y1 - y0) / 40))
    const cx = Math.max(0, Math.floor((x0 + x1) / 2 - (x1 - x0) / (2 * ds)))
    const cy = Math.max(0, Math.floor((y0 + y1) / 2 - (y1 - y0) / (2 * ds)))
    const cw = Math.min(iw - cx, Math.ceil((x1 - x0) / ds))
    const ch = Math.min(ih - cy, Math.ceil((y1 - y0) / ds))
    if (cw <= 0 || ch <= 0) return null
    const buf = img.crop({ x: cx, y: cy, width: cw, height: ch }).toBitmap()
    let sum = 0
    const n = buf.length / 4
    for (let i = 0; i < buf.length; i += 4) {
      sum += 0.299 * buf[i] + 0.587 * buf[i + 1] + 0.114 * buf[i + 2]
    }
    return sum / n
  } catch { return null }
}

async function pushBrightness(win) {
  try {
    if (win.isDestroyed()) return
    const avg = wallpaperLuminance(win)
    if (avg === null) return // 读不到壁纸时保持现状，不再用屏幕截图兜底（避免误识别窗口亮度）
    const state = avg > 140 ? 'light' : 'dark'
    // 防抖：连续两次一致才真正切换
    let f = flipState.get(win)
    if (!f) { f = { sent: null, pending: null, count: 0 }; flipState.set(win, f) }
    if (f.sent === null) { // 首次直接生效
      f.sent = state
      win.webContents.send('bg-brightness', state)
      return
    }
    if (state === f.sent) { f.count = 0; return }
    if (state !== f.pending) { f.pending = state; f.count = 1; return }
    f.count++
    if (f.count >= 2) {
      f.sent = state
      f.count = 0
      win.webContents.send('bg-brightness', state)
    }
  } catch { /* 忽略失败 */ }
}

function createAll() {
  const { width } = screen.getPrimaryDisplay().workAreaSize
  const bust = '&_=' + Date.now() // 每次启动拉取最新页面
  makeWindow('notes', BASE + '?view=notes' + bust, { width: 360, height: 460, x: width - 400, y: 40 })
  makeWindow('todos', BASE + '?view=todo' + bust, { width: 360, height: 420, x: width - 400, y: 520 })
}

function toggleAll() {
  const wins = Object.values(windows)
  if (wins.length === 0) { createAll(); return }
  const anyVisible = wins.some(w => w.isVisible())
  wins.forEach(w => anyVisible ? w.hide() : w.show())
}

function setTopMost(v) {
  topMost = v
  Object.values(windows).forEach(w => w.setAlwaysOnTop(v))
  tray?.setContextMenu(buildMenu())
}

function buildMenu() {
  return Menu.buildFromTemplate([
    { label: '浮窗外观…', click: () => appearance.open() },
    { label: '显示 / 隐藏 (Ctrl+Alt+G)', click: toggleAll },
    { label: (topMost ? '✓ ' : '') + '窗口置顶', click: () => setTopMost(!topMost) },
    { label: '打开完整版', click: () => {
      const full = new BrowserWindow({ width: 420, height: 760, autoHideMenuBar: true, backgroundColor: '#f7e98e' })
      require('./bundled-ui.cjs').protect(full)
      full.loadURL('https://sticky-notes.invalid/sticky-todo/')
    } },
    { type: 'separator' },
    { label: '退出', click: () => { app.exit(0) } },
  ])
}

app.whenReady().then(() => {
  require('./bundled-ui.cjs').init()
  appearance.init()
  createAll()
  globalShortcut.register('Ctrl+Alt+G', toggleAll)
  const icon = nativeImage.createFromPath(path.join(__dirname, 'icon-192.png'))
  tray = new Tray(icon.isEmpty() ? nativeImage.createEmpty() : icon)
  tray.setToolTip('Sticky Notes · Ctrl+Alt+G 显示/隐藏')
  tray.setContextMenu(buildMenu())
  tray.on('click', toggleAll)
  // 壁纸亮度定时检测（每 5 秒）
  setInterval(() => {
    Object.values(windows).forEach(w => { if (!w.isDestroyed()) pushBrightness(w) })
  }, 5000)
})

app.on('window-all-closed', () => { /* 保持后台运行 */ })
app.on('will-quit', () => globalShortcut.unregisterAll())
