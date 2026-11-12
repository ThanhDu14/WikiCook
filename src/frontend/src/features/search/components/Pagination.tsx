export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  className = '',
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  const handlePageClick = (page: number) => {
    if (page === currentPage || page < 1 || page > totalPages) {
      return;
    }
    onPageChange(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <nav
      className={`wk-pagination ${className}`}
      aria-label="Phân trang"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '4px',
        marginTop: '24px',
      }}
    >
      <button
        type="button"
        className="wk-btn wk-btn--ghost"
        aria-label="Trang trước"
        disabled={currentPage <= 1}
        onClick={() => handlePageClick(currentPage - 1)}
        style={{ minHeight: '44px', minWidth: '44px', padding: '0 8px' }}
      >
        ‹
      </button>

      {pages.map((p) => {
        const isCurrent = p === currentPage;
        return (
          <button
            key={p}
            type="button"
            className={`wk-btn ${isCurrent ? 'wk-btn--primary' : 'wk-btn--ghost'}`}
            aria-current={isCurrent ? 'page' : undefined}
            onClick={() => handlePageClick(p)}
            style={{ minHeight: '44px', minWidth: '44px', padding: '0 12px' }}
          >
            {p}
          </button>
        );
      })}

      <button
        type="button"
        className="wk-btn wk-btn--ghost"
        aria-label="Trang sau"
        disabled={currentPage >= totalPages}
        onClick={() => handlePageClick(currentPage + 1)}
        style={{ minHeight: '44px', minWidth: '44px', padding: '0 8px' }}
      >
        ›
      </button>
    </nav>
  );
}
