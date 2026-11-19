const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('floatBg', {
  onBrightness: (cb) => ipcRenderer.on('bg-brightness', (_e, v) => cb(v)),
})
