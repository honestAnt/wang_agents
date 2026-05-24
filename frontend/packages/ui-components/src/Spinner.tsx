export function Spinner({ className = "" }: { className?: string }) {
  return (
    <div className={`animate-spin rounded-full h-6 w-6 border-2 border-gray-300 border-t-blue-600 ${className}`} />
  );
}
