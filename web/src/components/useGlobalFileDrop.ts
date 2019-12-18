import React from 'react'

export function useGlobalFileDrop(handler: (file: File) => Promise<void>) {
  React.useEffect(() => {
    const dragOverHandler = (event: DragEvent) => {
      event.preventDefault()
    }
    const dropHandler = (event: DragEvent) => {
      event.preventDefault()

      if (event.dataTransfer && event.dataTransfer.items) {
        // Use DataTransferItemList interface to access the file(s)
        for (let i = 0; i < event.dataTransfer.items.length; i++) {
          // If dropped items aren't files, reject them
          if (event.dataTransfer.items[i].kind === 'file') {
            var file = event.dataTransfer.items[i].getAsFile()
            if (file) {
              handler(file)
            }
          }
        }
      } else if (event.dataTransfer && event.dataTransfer.files) {
        // Use DataTransfer interface to access the file(s)
        for (let i = 0; i < event.dataTransfer.files.length; i++) {
          handler(event.dataTransfer.files[i])
        }
      }
    }
    document.addEventListener('dragover', dragOverHandler)
    document.addEventListener('drop', dropHandler)
    return () => {
      document.removeEventListener('dragover', dragOverHandler)
      document.removeEventListener('drop', dropHandler)
    }
  }, [handler])
}
