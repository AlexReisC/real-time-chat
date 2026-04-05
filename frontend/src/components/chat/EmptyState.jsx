export function EmptyState() {
  return (
    <div className="empty-state">
      <span className="empty-state__glyph">◈</span>
      <h2 className="empty-state__title">Nenhuma conversa selecionada</h2>
      <p className="empty-state__sub">
        Escolha uma sala ou conversa privada para começar
      </p>
    </div>
  );
}
