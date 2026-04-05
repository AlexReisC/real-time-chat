export function Avatar({ name, size = 'md' }) {
  const initials = name
    ? name.split(' ').map((w) => w[0]).join('').slice(0, 2).toUpperCase()
    : '?';

  // Deterministic color from name
  const colors = [
    '#4a7c6f', '#6b7c4a', '#7c4a6b', '#4a6b7c',
    '#7c6b4a', '#4a4a7c', '#7c4a4a', '#4a7c7c',
  ];
  const idx = name
    ? [...name].reduce((acc, c) => acc + c.charCodeAt(0), 0) % colors.length
    : 0;

  return (
    <span
      className={`avatar avatar--${size}`}
      style={{ backgroundColor: colors[idx] }}
      aria-hidden="true"
    >
      {initials}
    </span>
  );
}
