interface ProductImageProps {
  imageUrl: string | null
  category: string
  name: string
  className?: string
}

export function ProductImage({ imageUrl, category, name, className = '' }: ProductImageProps) {
  if (!imageUrl) {
    return <div role="img" aria-label={name} className={`grid place-items-center bg-gradient-to-br from-sky-200 via-blue-400 to-indigo-700 text-5xl font-bold text-white ${className}`}>{category.charAt(0).toUpperCase()}</div>
  }
  return <img src={imageUrl} alt={name} loading="lazy" className={className} onError={(event) => { event.currentTarget.style.display = 'none'; }} />
}
