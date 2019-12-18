import React from 'react'

interface Props {
  contentType?: string
}

export const ContentTypeLogo: React.FC<Props> = ({ contentType }) => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 1024 1024">
      <path
        fill="#e0e0e0"
        d="M704 0v256h256v684.693c0 45.978-37.329 83.307-83.307 83.307H257.307C211.329 1024 174 986.671 174 940.693V83.307C174 37.329 211.328 0 257.307 0H704z"
      />
      <path fill="#cdcdcd" d="M704 0v256h256L704 0" />
      <path
        fill={contentTypeColor(contentType)}
        d="M831.997 550.184c0-21.075-17.111-38.185-38.186-38.185H102.186C81.11 512 64 529.11 64 550.184v307.629c0 21.075 17.11 38.185 38.186 38.185H793.81c21.075 0 38.186-17.11 38.186-38.185V550.184z"
      />
      <text
        x="448"
        y="704"
        fill="#fff"
        fontFamily="'ArialMT','Arial',sans-serif"
        fontSize="300"
        textAnchor="middle"
        dy="0.35em">
        {contentTypeExtension(contentType)}
      </text>
    </svg>
  )
}

function contentTypeExtension(contentType?: string): string {
  switch (contentType) {
    case 'application/pdf':
      return 'PDF'
    case 'image/png':
      return 'PNG'
    case 'image/jpeg':
      return 'JPG'
    case 'image/svg+xml':
      return 'SVG'
    default:
      return '???'
  }
}

function contentTypeColor(contentType?: string): string {
  switch (contentType) {
    case 'application/pdf':
      return '#d64d4d'
    case 'image/png':
    case 'image/jpeg':
    case 'image/svg+xml':
      return '#644cd6'
    default:
      return '#767676'
  }
}
